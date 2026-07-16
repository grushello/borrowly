package com.borrowly.controller;

import com.borrowly.dto.request.CategoryRequest;
import com.borrowly.dto.response.CategoryResponse;
import com.borrowly.model.item.Category;
import com.borrowly.service.CategoryService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Categories", description = "Endpoints for creating, viewing, and deleting categories")
public class CategoryController {

    private final CategoryService categoryService;

    @ApiResponse(responseCode = "200", description = "List of categories retrieved successfully")
    @GetMapping("/api/categories")
    public List<CategoryResponse> findAll() {
        return categoryService.findAll();
    }

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @GetMapping("/api/admin/categories/{id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable UUID id) {
        return categoryService.findById(id).map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
    }

    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Category created successfully"),
            @ApiResponse(responseCode = "409", description = "A category with this name already exists")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/admin/categories")
    @Transactional
    public CategoryResponse addCategory(@RequestBody CategoryRequest categoryRequest) {
        return categoryService.add(categoryRequest);
    }

    @ApiResponse(responseCode = "201", description = "Category updated successfully")
    @Transactional
    @PatchMapping("/api/admin/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable UUID id, @RequestBody CategoryRequest categoryRequest) {
        return categoryService.update(id, categoryRequest);
    }

    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
            @ApiResponse(responseCode = "409", description = "Could not delete category")
    })
    @Transactional
    @DeleteMapping("/api/admin/categories/{id}")
    public void deleteCategory(@PathVariable UUID id) {
        categoryService.delete(id);
    }
}
