package com.borrowly.controller;

import com.borrowly.dto.request.CategoryRequest;
import com.borrowly.dto.response.CategoryResponse;
import com.borrowly.service.CategoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for category endpoints.
 * Returns JSON — does not render HTML pages.
 */
@RequiredArgsConstructor
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/api/categories")
    public List<CategoryResponse> findAll() {
        return categoryService.findAll();
    }

    @GetMapping("/api/admin/categories/{id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable UUID id) {
        return categoryService.findById(id).map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/admin/categories")
    @Transactional
    public CategoryResponse addCategory(@RequestBody CategoryRequest categoryRequest) {
        return categoryService.add(categoryRequest);
    }

    @Transactional
    @PatchMapping("/api/admin/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable UUID id, @RequestBody CategoryRequest categoryRequest) {
        return categoryService.update(id, categoryRequest);
    }

    @Transactional
    @DeleteMapping("/api/admin/categories/{id}")
    public void deleteCategory(@PathVariable UUID id) {
        categoryService.delete(id);
    }
}
