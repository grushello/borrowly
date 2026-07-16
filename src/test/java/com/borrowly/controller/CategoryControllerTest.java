package com.borrowly.controller;

import com.borrowly.dto.request.CategoryRequest;
import com.borrowly.dto.response.CategoryResponse;
import com.borrowly.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CategoryController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void findAll_ReturnsListOfCategoriesAndStatus200() throws Exception {
        CategoryResponse cat1 = new CategoryResponse(UUID.randomUUID(), "Fishing", "Gear");
        CategoryResponse cat2 = new CategoryResponse(UUID.randomUUID(), "Camping", "Tents");
        when(categoryService.findAll()).thenReturn(List.of(cat1, cat2));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].name").value("Fishing"));
    }

    @Test
    void findById_ReturnsCategoryAndStatus200_WhenFound() throws Exception {
        UUID id = UUID.randomUUID();
        CategoryResponse response = new CategoryResponse(id, "Fishing", "Gear");

        when(categoryService.findById(id)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/admin/categories/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fishing"));
    }

    @Test
    void findById_ReturnsStatus404_WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();

        when(categoryService.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/categories/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void addCategory_ReturnsCreatedCategoryAndStatus201() throws Exception {
        CategoryRequest request = new CategoryRequest("Fishing", "Gear");
        CategoryResponse response = new CategoryResponse(UUID.randomUUID(), "Fishing", "Gear");

        when(categoryService.add(any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Fishing"));
    }

    @Test
    void deleteCategory_ReturnsStatus200() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/categories/{id}", id))
                .andExpect(status().isOk());
    }
}
