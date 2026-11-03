package com.ecommerce.service;

import com.ecommerce.dto.OrderDto.OrderResponse;
import com.ecommerce.dto.OrderDto.PlaceOrderRequest;
import com.ecommerce.dto.PageResponse;
import com.ecommerce.entity.*;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.Mappers;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {

    private static final int MAX_PAGE_SIZE = 50;

    /** Allowed admin status changes (a simple state machine). */
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            OrderStatus.PLACED, Set.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.PAID, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.CANCELLED, Set.of());

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository,
                        ProductRepository productRepository, PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.paymentService = paymentService;
    }

    /**
     * Checkout. One transaction covers: stock check + deduction, order creation, payment and clearing the cart.
     * Any failure (e.g. payment declined) rolls ALL of it back.
     */
    @Transactional
    public OrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Your cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        Order order = new Order();
        order.setUser(cart.getUser());
        order.setShippingAddress(request.shippingAddress().trim());
        order.setStatus(OrderStatus.PLACED);

        // Lock products in a fixed (id) order so two concurrent checkouts cannot deadlock each other.
        List<CartItem> items = cart.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProduct().getId()))
                .toList();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : items) {
            Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product no longer exists"));
            if (!product.isActive()) {
                throw new BadRequestException("'" + product.getName() + "' is no longer available. Remove it from your cart.");
            }
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new BadRequestException("Insufficient stock for '" + product.getName()
                        + "'. Available: " + product.getStockQuantity());
            }
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setPrice(product.getPrice()); // price snapshot
            orderItem.setQuantity(cartItem.getQuantity());
            order.addItem(orderItem);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        order.setTotalAmount(total);
        order = orderRepository.save(order);

        Payment payment = paymentService.process(order, request.paymentMethod());
        order.setPayment(payment);
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            order.setStatus(OrderStatus.PAID);
        }

        cart.getItems().clear();
        return Mappers.toOrder(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(Long userId, int page, int size) {
        Page<Order> orders = orderRepository.findByUserId(userId, newestFirst(page, size));
        return PageResponse.of(orders.map(Mappers::toOrder));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(OrderStatus status, int page, int size) {
        Pageable pageable = newestFirst(page, size);
        Page<Order> orders = status == null
                ? orderRepository.findAll(pageable)
                : orderRepository.findByStatus(status, pageable);
        return PageResponse.of(orders.map(Mappers::toOrder));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long userId, boolean isAdmin, Long orderId) {
        return Mappers.toOrder(findVisibleOrder(userId, isAdmin, orderId));
    }

    @Transactional
    public OrderResponse cancelMyOrder(Long userId, Long orderId) {
        Order order = findVisibleOrder(userId, false, orderId);
        if (order.getStatus() != OrderStatus.PLACED && order.getStatus() != OrderStatus.PAID) {
            throw new BadRequestException("An order that is " + order.getStatus() + " can no longer be cancelled");
        }
        cancel(order);
        return Mappers.toOrder(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + orderId));
        if (!TRANSITIONS.get(order.getStatus()).contains(newStatus)) {
            throw new BadRequestException("Cannot change order status from " + order.getStatus() + " to " + newStatus);
        }

        if (newStatus == OrderStatus.CANCELLED) {
            cancel(order);
        } else {
            order.setStatus(newStatus);
            Payment payment = order.getPayment();
            boolean cashCollected = newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.PAID;
            if (cashCollected && payment != null && payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.SUCCESS);
            }
        }
        return Mappers.toOrder(order);
    }

    /** Puts the stock back, refunds the payment and marks the order cancelled. */
    private void cancel(Order order) {
        for (OrderItem item : order.getItems()) {
            productRepository.findByIdForUpdate(item.getProduct().getId()).ifPresent(product ->
                    product.setStockQuantity(product.getStockQuantity() + item.getQuantity()));
        }
        paymentService.refundIfNeeded(order);
        order.setStatus(OrderStatus.CANCELLED);
    }

    /** Another user's order is reported as "not found" so order ids cannot be probed. */
    private Order findVisibleOrder(Long userId, boolean isAdmin, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + orderId));
        if (!isAdmin && !order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Order not found with id " + orderId);
        }
        return order;
    }

    private Pageable newestFirst(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }
}
