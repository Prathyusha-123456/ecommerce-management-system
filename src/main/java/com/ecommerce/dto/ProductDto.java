package com.ecommerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ProductDto {

    private ProductDto() {
    }

    public record ProductRequest(
            @NotBlank(message = "Product name is required")
            @Size(max = 200, message = "Name must be at most 200 characters")
            String name,

            @Size(max = 2000, message = "Description must be at most 2000 characters")
            String description,

            @NotNull(message = "Price is required")
            @DecimalMin(value = "0.01", message = "Price must be greater than 0")
            @Digits(integer = 10, fraction = 2, message = "Price can have at most 2 decimal places")
            BigDecimal price,

            @NotNull(message = "Stock quantity is required")
            @Min(value = 0, message = "Stock cannot be negative")
            Integer stockQuantity,

            @Size(max = 500, message = "Image URL must be at most 500 characters")
            String imageUrl,

            @NotNull(message = "Category is required")
            Long categoryId) {
    }

    public record ProductResponse(Long id, String name, String description, BigDecimal price,
                                  Integer stockQuantity, String imageUrl, Long categoryId,
                                  String categoryName, LocalDateTime createdAt) {
    }
}
