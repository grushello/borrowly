package com.borrowly.mapper;

import com.borrowly.dto.request.CategoryRequest;
import com.borrowly.dto.response.CategoryResponse;
import com.borrowly.model.item.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void toResponse_RequestToEntity() {
        Category category = Category.builder().name("fishing").description("All things fishing.").build();
        CategoryRequest request = new CategoryRequest(category.getName(),  category.getDescription());

        // TODO
    }
}
