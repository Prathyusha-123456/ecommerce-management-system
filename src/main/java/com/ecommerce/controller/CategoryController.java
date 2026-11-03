package com.ecommerce.controller;

import com.ecommerce.dto.CategoryDto.CategoryResponse;
import com.ecommerce.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "3. Categories (public)")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "List all categories")
    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.findAll();
    }

    @Operation(summary = "Get a category by id")
    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable Long id) {
        return categoryService.findById(id);
    }
}
