package com.ecommerce.service;

import com.ecommerce.dto.CartDto.AddCartItemRequest;
import com.ecommerce.dto.CartDto.CartResponse;
import com.ecommerce.dto.CartDto.UpdateCartItemRequest;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.Mappers;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CartResponse getCart(Long userId) {
        return Mappers.toCart(getOrCreateCart(userId));
    }

    /** Adding a product that is already in the cart increases its quantity instead of duplicating the line. */
    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(request.productId())
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + request.productId()));

        CartItem existing = findItem(cart, product.getId());
        int newQuantity = (existing == null ? 0 : existing.getQuantity()) + request.quantity();
        ensureStock(product, newQuantity);

        if (existing == null) {
            CartItem item = new CartItem();
            item.setProduct(product);
            item.setQuantity(newQuantity);
            cart.addItem(item);
        } else {
            existing.setQuantity(newQuantity);
        }
        return Mappers.toCart(cart);
    }

    @Transactional
    public CartResponse updateItem(Long userId, Long productId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = requireItem(cart, productId);
        ensureStock(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());
        return Mappers.toCart(cart);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);
        cart.removeItem(requireItem(cart, productId)); // orphanRemoval deletes the row
        return Mappers.toCart(cart);
    }

    @Transactional
    public CartResponse clear(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        return Mappers.toCart(cart);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(userRepository.getReferenceById(userId));
            return cartRepository.save(cart);
        });
    }

    private CartItem findItem(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private CartItem requireItem(Cart cart, Long productId) {
        CartItem item = findItem(cart, productId);
        if (item == null) {
            throw new ResourceNotFoundException("Product " + productId + " is not in your cart");
        }
        return item;
    }

    private void ensureStock(Product product, int requested) {
        if (requested > product.getStockQuantity()) {
            throw new BadRequestException("Only " + product.getStockQuantity() + " unit(s) of '"
                    + product.getName() + "' available");
        }
    }
}
