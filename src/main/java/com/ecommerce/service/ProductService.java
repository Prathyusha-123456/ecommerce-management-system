package com.ecommerce.service;

import com.ecommerce.dto.PageResponse;
import com.ecommerce.dto.ProductDto.ProductRequest;
import com.ecommerce.dto.ProductDto.ProductResponse;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.Mappers;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class ProductService {

    /** Whitelist: never pass a client-supplied sort field straight to the database layer. */
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "name", "price", "createdAt");
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String keyword, Long categoryId, BigDecimal minPrice,
                                                BigDecimal maxPrice, int page, int size,
                                                String sortBy, String direction) {
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new BadRequestException("sortBy must be one of " + SORTABLE_FIELDS);
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice cannot be greater than maxPrice");
        }
        Sort.Direction dir = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(dir, sortBy));

        Page<Product> result = productRepository.findAll(
                ProductSpecifications.filter(keyword, categoryId, minPrice, maxPrice), pageable);
        return PageResponse.of(result.map(Mappers::toProduct));
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return Mappers.toProduct(findActive(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return Mappers.toProduct(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findActive(id);
        apply(product, request);
        return Mappers.toProduct(product);
    }

    /** Soft delete: old orders still reference the product row, so it is hidden instead of removed. */
    @Transactional
    public void delete(Long id) {
        findActive(id).setActive(false);
    }

    private void apply(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + request.categoryId()));
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setImageUrl(request.imageUrl());
        product.setCategory(category);
    }

    private Product findActive(Long id) {
        return productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
    }
}
