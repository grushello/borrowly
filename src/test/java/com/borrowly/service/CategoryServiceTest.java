package com.borrowly.service;

import com.borrowly.dto.request.CategoryRequest;
import com.borrowly.dto.response.CategoryResponse;
import com.borrowly.mapper.CategoryMapper;
import com.borrowly.model.item.Category;
import com.borrowly.repository.item.CategoryRepository;
import com.borrowly.repository.item.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ItemRepository itemRepository;

    @Spy
    private CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void findsAllCategories() {
        Category category1 = Category.builder()
                .name("Fishing")
                .description("All things fishing")
                .build();
        Category category2 = Category.builder()
                .name("Camping Gear")
                .description("Tents, sleeping bags, and outdoor equipment")
                .build();
        when(categoryRepository.findAll()).thenReturn(List.of(category1, category2));

        List<CategoryResponse> results = categoryService.findAll();
        assertEquals(2, results.size());
        assertEquals(category1.getName(), results.get(0).name());
        assertEquals(category1.getDescription(), results.get(0).description());

        assertEquals(category2.getName(), results.get(1).name());
        assertEquals(category2.getDescription(), results.get(1).description());
    }

    @Test
    void addCategory_DuplicateReturnsConflictAndStatus409() {
        // Arrange
        CategoryRequest request = new CategoryRequest("Furniture", "All things furniture");

        when(categoryRepository.existsByNameIgnoreCase("Furniture")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> categoryService.add(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void findById_findsCategoryById() {
        Category category = Category.builder()
                .name("Fishing")
                .description("All things fishing")
                .build();

        UUID id = category.getId();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));

        CategoryResponse response = categoryService.findById(id).get();

        assertNotNull(response);
        assertEquals("Fishing", response.name());
        assertEquals("All things fishing", response.description());

        verify(categoryRepository).findById(id);
    }

    @Test
    void findById_ThrowsExceptionWhenIdNotFound() {
        UUID fakeId = UUID.randomUUID();

        when(categoryRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            categoryService.findById(fakeId);
        });

        verify(categoryRepository).findById(fakeId);
    }

    @Test
    void add_addsCategoryCorrectly(){
        CategoryRequest request = new CategoryRequest("Fishing", "All things fishing");

        Category savedCategory = Category.builder()
                .name("Fishing")
                .description("All things fishing")
                .build();

        when(categoryRepository.save(Mockito.<Category>any())).thenReturn(savedCategory);

        CategoryResponse response = categoryService.add(request);

        assertNotNull(response);
        assertEquals("Fishing", response.name());
        assertEquals("All things fishing", response.description());

        verify(categoryRepository).save(Mockito.<Category>any());
    }

    @Test
    void delete_deletesCategoryCorrectly(){
        Category existingCategory = Category.builder()
                .name("Fishing")
                .build();
        UUID id = existingCategory.getId();

        assertNotNull(id);
        when(categoryRepository.existsById(id)).thenReturn(true);
        when(itemRepository.existsByCategory_Id(id)).thenReturn(false);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(existingCategory));

        categoryService.delete(id);

        verify(itemRepository).existsByCategory_Id(id);
        verify(categoryRepository).delete(existingCategory);
    }

    @Test
    void delete_deleteThrowsExceptionWhenIdNotFound() {
        UUID fakeId = UUID.randomUUID();

        when(itemRepository.existsByCategory_Id(fakeId)).thenReturn(false);
        when(categoryRepository.existsById(fakeId)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> {
            categoryService.delete(fakeId);
        });

        verify(categoryRepository).existsById(fakeId);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_ShouldThrowConflict_WhenItemsAreAssociated() {
        UUID id = UUID.randomUUID();
        when(itemRepository.existsByCategory_Id(id)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> categoryService.delete(id)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Cannot eliminate: there are associated items with this category.", exception.getReason());

        verify(categoryRepository, never()).deleteById(any());
        verify(itemRepository).existsByCategory_Id(id);
    }
}
