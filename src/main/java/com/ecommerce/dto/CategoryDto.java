package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoryDto {

    private CategoryDto() {
    }

    public record CategoryRequest(
            @NotBlank(message = "Category name is required")
            @Size(max = 100, message = "Name must be at most 100 characters")
            String name,

            @Size(max = 500, message = "Description must be at most 500 characters")
            String description) {
    }

    public record CategoryResponse(Long id, String name, String description) {
    }
}
