package com.borrowly.service.item;

import com.borrowly.dto.request.CategoryRequest;
import com.borrowly.dto.response.CategoryResponse;
import com.borrowly.exception.CategoryAlreadyExistsException;
import com.borrowly.exception.CategoryConflictException;
import com.borrowly.exception.CategoryNotFoundException;
import com.borrowly.mapper.CategoryMapper;
import com.borrowly.model.item.Category;
import com.borrowly.repository.item.CategoryRepository;
import com.borrowly.repository.item.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
    private final CategoryMapper categoryMapper;

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(categoryMapper::toResponse).toList();
    }

    public CategoryResponse findById(UUID id) {
        return categoryRepository.findById(id)
                .map(categoryMapper::toResponse)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    @Transactional
    public CategoryResponse add(CategoryRequest categoryRequest) {
        if (categoryRepository.existsByNameIgnoreCase(categoryRequest.name())) {
            throw new CategoryAlreadyExistsException();
        }
        Category category = categoryMapper.toEntity(categoryRequest);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest categoryRequest) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        categoryMapper.updateEntity(category, categoryRequest);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id) {
        if (itemRepository.existsByCategoryId(id)) {
            throw new CategoryConflictException(
                    "Cannot eliminate: there are associated items with this category."
            );
        }

        Category category = categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
        categoryRepository.delete(category);
    }
}
