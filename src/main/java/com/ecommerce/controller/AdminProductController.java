package com.ecommerce.controller;

import com.ecommerce.dto.MessageResponse;
import com.ecommerce.dto.ProductDto.ProductRequest;
import com.ecommerce.dto.ProductDto.ProductResponse;
import com.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "8. Admin - Products", description = "ADMIN role required")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Add a product")
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @Operation(summary = "Update a product")
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @Operation(summary = "Delete a product (soft delete - hidden from the catalogue, kept for order history)")
    @DeleteMapping("/{id}")
    public MessageResponse delete(@PathVariable Long id) {
        productService.delete(id);
        return new MessageResponse("Product deleted");
    }
}
