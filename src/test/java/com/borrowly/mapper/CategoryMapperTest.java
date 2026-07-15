package com.borrowly.mapper;

import com.borrowly.dto.request.CategoryRequest;
import com.borrowly.dto.response.CategoryResponse;
import com.borrowly.model.item.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CategoryMapperTest {
    CategoryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CategoryMapperImpl();
    }

    @Test
    void toResponse_MapsCategoryFields() {
        Category category = Category.builder().name("fishing").description("All things fishing.").build();
        CategoryResponse response = mapper.toResponse(category);

        assertEquals(category.getName(), response.name());
        assertEquals(category.getDescription(), response.description());
    }

    @Test
    void toCategory_CreatesEntityFromRequest() {
        CategoryRequest request = new CategoryRequest("fishing",  "All things fishing");

        Category category = mapper.toCategory(request);

        assertNotNull(category.getId());
        assertEquals("fishing", category.getName());
        assertEquals("All things fishing", category.getDescription());
    }

    @Test
    void updateEntity_PartialUpdate_LeavesNonProvidedFieldsUnchanged(){
        Category existingCategory =  Category.builder().name("fishing").description("All things fishing.").build();
        UUID originalId = existingCategory.getId();

        CategoryRequest request = new CategoryRequest(null,  "Everything fishing.");
        mapper.updateEntity(existingCategory, request);

        assertEquals(originalId, existingCategory.getId());
        assertEquals("fishing", existingCategory.getName());
        assertEquals("Everything fishing.", existingCategory.getDescription());
    }
}
