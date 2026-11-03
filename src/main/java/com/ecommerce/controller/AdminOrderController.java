package com.ecommerce.controller;

import com.ecommerce.dto.OrderDto.OrderResponse;
import com.ecommerce.dto.OrderDto.UpdateOrderStatusRequest;
import com.ecommerce.dto.PageResponse;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "9. Admin - Orders", description = "ADMIN role required")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "List all orders (optionally filtered by status)")
    @GetMapping
    public PageResponse<OrderResponse> all(@RequestParam(required = false) OrderStatus status,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return orderService.getAllOrders(status, page, size);
    }

    @Operation(summary = "Update an order's status",
            description = "Allowed: PLACED->PAID/SHIPPED/CANCELLED, PAID->SHIPPED/CANCELLED, SHIPPED->DELIVERED")
    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(id, request.status());
    }
}
