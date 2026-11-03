package com.ecommerce.dto;

import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.PaymentMethod;
import com.ecommerce.entity.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDto {

    private OrderDto() {
    }

    public record PlaceOrderRequest(
            @NotBlank(message = "Shipping address is required")
            @Size(max = 500, message = "Address must be at most 500 characters")
            String shippingAddress,

            @NotNull(message = "Payment method is required")
            PaymentMethod paymentMethod) {
    }

    public record UpdateOrderStatusRequest(
            @NotNull(message = "Status is required")
            OrderStatus status) {
    }

    public record OrderItemResponse(Long productId, String productName, Integer quantity,
                                    BigDecimal price, BigDecimal subtotal) {
    }

    public record PaymentResponse(Long id, PaymentMethod method, PaymentStatus status,
                                  String transactionId, BigDecimal amount) {
    }

    public record OrderResponse(Long id, Long userId, String userEmail, BigDecimal totalAmount,
                                OrderStatus status, String shippingAddress, LocalDateTime createdAt,
                                List<OrderItemResponse> items, PaymentResponse payment) {
    }
}
