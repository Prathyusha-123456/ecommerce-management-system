package com.ecommerce.controller;

import com.ecommerce.dto.CartDto.AddCartItemRequest;
import com.ecommerce.dto.CartDto.CartResponse;
import com.ecommerce.dto.CartDto.UpdateCartItemRequest;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "5. Cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(summary = "View my cart")
    @GetMapping
    public CartResponse get(@AuthenticationPrincipal UserPrincipal principal) {
        return cartService.getCart(principal.getId());
    }

    @Operation(summary = "Add a product to my cart")
    @PostMapping("/items")
    public CartResponse add(@AuthenticationPrincipal UserPrincipal principal,
                            @Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(principal.getId(), request);
    }

    @Operation(summary = "Change the quantity of a cart item")
    @PutMapping("/items/{productId}")
    public CartResponse update(@AuthenticationPrincipal UserPrincipal principal,
                               @PathVariable Long productId,
                               @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(principal.getId(), productId, request);
    }

    @Operation(summary = "Remove a product from my cart")
    @DeleteMapping("/items/{productId}")
    public CartResponse remove(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long productId) {
        return cartService.removeItem(principal.getId(), productId);
    }

    @Operation(summary = "Empty my cart")
    @DeleteMapping
    public CartResponse clear(@AuthenticationPrincipal UserPrincipal principal) {
        return cartService.clear(principal.getId());
    }
}
