package com.ecommerce.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class CartDto {

    private CartDto() {
    }

    public record AddCartItemRequest(
            @NotNull(message = "Product id is required")
            Long productId,

            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            @Max(value = 100, message = "Quantity must be at most 100")
            Integer quantity) {
    }

    public record UpdateCartItemRequest(
            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            @Max(value = 100, message = "Quantity must be at most 100")
            Integer quantity) {
    }

    public record CartItemResponse(Long productId, String productName, BigDecimal unitPrice,
                                   Integer quantity, BigDecimal subtotal, Integer availableStock) {
    }

    public record CartResponse(Long cartId, List<CartItemResponse> items, int totalItems, BigDecimal totalAmount) {
    }
}
