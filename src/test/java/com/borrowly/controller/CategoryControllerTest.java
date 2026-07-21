package com.borrowly.controller;

import com.borrowly.config.SecurityConfig;
import com.borrowly.dto.request.CategoryRequest;
import com.borrowly.dto.response.CategoryResponse;
import com.borrowly.exception.CategoryNotFoundException;
import com.borrowly.exception.GlobalExceptionHandler;
import com.borrowly.security.AuthEntryPointJwt;
import com.borrowly.security.AuthTokenFilter;
import com.borrowly.security.JwtUtil;
import com.borrowly.service.item.CategoryService;
import com.borrowly.service.auth.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CategoryController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityConfig.class, AuthTokenFilter.class, AuthEntryPointJwt.class,
        GlobalExceptionHandler.class})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

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

        when(categoryService.findById(id)).thenReturn(response);

        mockMvc.perform(get("/api/categories/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fishing"));
    }

    @Test
    void findById_ReturnsStatus404_WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();

        when(categoryService.findById(id)).thenThrow(new CategoryNotFoundException(id));

        mockMvc.perform(get("/api/categories/{id}", id))
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
    void addCategory_DuplicateReturnsConflictAndStatus409() throws Exception {
        CategoryRequest request = new CategoryRequest("Furniture", "All things furniture");

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT))
                .when(categoryService).add(any(CategoryRequest.class));

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void addCategory_WhenNameIsBlank_ReturnsStatus400() throws Exception {
        CategoryRequest request = new CategoryRequest(" ", "Valid description");

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists());
    }

    @Test
    void addCategory_WhenNameIsTooLong_ReturnsStatus400() throws Exception {
        String tooLongName = "a".repeat(101);
        CategoryRequest request = new CategoryRequest(tooLongName, "Valid description");

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists());

        verifyNoInteractions(categoryService);
    }

    @Test
    void addCategory_WhenDescriptionIsTooLong_ReturnsStatus400() throws Exception {
        String tooLongDescription = "a".repeat(501);
        CategoryRequest request = new CategoryRequest("Valid name", tooLongDescription);

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.description").exists());

        verifyNoInteractions(categoryService);
    }

    @Test
    void deleteCategory_ReturnsStatus204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/categories/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCategory_Returns409_WhenItemsAreAssociated() throws Exception {
        UUID id = UUID.randomUUID();

        doThrow(new ResponseStatusException(HttpStatus.CONFLICT))
                .when(categoryService).delete(id);

        mockMvc.perform(delete("/api/admin/categories/{id}", id))
                .andExpect(status().isConflict());

        verify(categoryService).delete(id);
    }
}
