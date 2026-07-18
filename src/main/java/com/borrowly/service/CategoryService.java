package com.borrowly.service;

import com.borrowly.dto.request.CategoryRequest;
import com.borrowly.dto.response.CategoryResponse;
import com.borrowly.mapper.CategoryMapper;
import com.borrowly.model.item.Category;
import com.borrowly.repository.item.CategoryRepository;
import com.borrowly.repository.item.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
    private final CategoryMapper categoryMapper;

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(categoryMapper::toResponse).toList();
    }

    public Optional<CategoryResponse> findById(UUID id) {
        Optional<Category> category = categoryRepository.findById(id);
        if (category.isEmpty()) {
            throw new EntityNotFoundException("Category not found");
        }
        return category.map(categoryMapper::toResponse);
    }

    public CategoryResponse add(CategoryRequest categoryRequest) {
        if (categoryRepository.existsByNameIgnoreCase(categoryRequest.name())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A category with the name '" + categoryRequest.name() + "' already exists."
            );
        }
        Category category = categoryMapper.toEntity(categoryRequest);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    public CategoryResponse update(UUID id, CategoryRequest categoryRequest) {
        Category category = categoryRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        categoryMapper.updateEntity(category, categoryRequest);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    public void delete(UUID id) {
        if (itemRepository.existsByCategory_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot eliminate: there are associated items with this category."
            );
        }

        if (!categoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found.");
        }
        Category category = categoryRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        categoryRepository.delete(category);
    }
}
