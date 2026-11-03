package com.ecommerce.mapper;

import com.ecommerce.dto.CartDto.CartItemResponse;
import com.ecommerce.dto.CartDto.CartResponse;
import com.ecommerce.dto.CategoryDto.CategoryResponse;
import com.ecommerce.dto.OrderDto.OrderItemResponse;
import com.ecommerce.dto.OrderDto.OrderResponse;
import com.ecommerce.dto.OrderDto.PaymentResponse;
import com.ecommerce.dto.ProductDto.ProductResponse;
import com.ecommerce.dto.UserDto.UserResponse;
import com.ecommerce.entity.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Entity -> DTO conversion. Always call these inside a service @Transactional method,
 * because they read lazily loaded associations.
 */
public final class Mappers {

    private Mappers() {
    }

    public static UserResponse toUser(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                u.getAddress(), u.getRole(), u.getCreatedAt());
    }

    public static CategoryResponse toCategory(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription());
    }

    public static ProductResponse toProduct(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getStockQuantity(), p.getImageUrl(), p.getCategory().getId(),
                p.getCategory().getName(), p.getCreatedAt());
    }

    public static CartResponse toCart(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(i -> {
            Product p = i.getProduct();
            BigDecimal subtotal = p.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
            return new CartItemResponse(p.getId(), p.getName(), p.getPrice(), i.getQuantity(),
                    subtotal, p.getStockQuantity());
        }).toList();

        int totalItems = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal total = items.stream().map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, totalItems, total);
    }

    public static PaymentResponse toPayment(Payment p) {
        if (p == null) {
            return null;
        }
        return new PaymentResponse(p.getId(), p.getMethod(), p.getStatus(), p.getTransactionId(), p.getAmount());
    }

    public static OrderResponse toOrder(Order o) {
        List<OrderItemResponse> items = o.getItems().stream()
                .map(i -> new OrderItemResponse(i.getProduct().getId(), i.getProductName(), i.getQuantity(),
                        i.getPrice(), i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();
        return new OrderResponse(o.getId(), o.getUser().getId(), o.getUser().getEmail(), o.getTotalAmount(),
                o.getStatus(), o.getShippingAddress(), o.getCreatedAt(), items, toPayment(o.getPayment()));
    }
}
