package com.ecommerce.controller;

import com.ecommerce.dto.OrderDto.OrderResponse;
import com.ecommerce.dto.OrderDto.PlaceOrderRequest;
import com.ecommerce.dto.PageResponse;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "6. Orders & Payment")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Checkout: turn my cart into an order and pay",
            description = "paymentMethod: CARD | UPI | NET_BANKING | COD. Online methods go through the payment gateway.")
    @PostMapping
    public ResponseEntity<OrderResponse> place(@AuthenticationPrincipal UserPrincipal principal,
                                               @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(principal.getId(), request));
    }

    @Operation(summary = "My order history (newest first, paginated)")
    @GetMapping
    public PageResponse<OrderResponse> myOrders(@AuthenticationPrincipal UserPrincipal principal,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return orderService.getMyOrders(principal.getId(), page, size);
    }

    @Operation(summary = "Get one of my orders")
    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return orderService.getOrder(principal.getId(), principal.isAdmin(), id);
    }

    @Operation(summary = "Cancel my order (only while PLACED or PAID); stock is restored and payment refunded")
    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return orderService.cancelMyOrder(principal.getId(), id);
    }
}
