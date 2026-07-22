package com.borrowly.controller;

import com.borrowly.config.SecurityConfig;
import com.borrowly.dto.request.CategoryRequest;
import com.borrowly.dto.response.CategoryResponse;
import com.borrowly.exception.CategoryAlreadyExistsException;
import com.borrowly.exception.CategoryConflictException;
import com.borrowly.exception.GlobalExceptionHandler;
import com.borrowly.security.AuthEntryPointJwt;
import com.borrowly.security.AuthTokenFilter;
import com.borrowly.security.JwtUtil;
import com.borrowly.service.auth.UserDetailsServiceImpl;
import com.borrowly.service.item.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminCategoryController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityConfig.class, AuthTokenFilter.class, AuthEntryPointJwt.class,
        GlobalExceptionHandler.class})
@WithMockUser(roles = "ADMIN")
class AdminCategoryControllerTest {

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

        doThrow(new CategoryAlreadyExistsException())
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

        doThrow(new CategoryConflictException(
                "Cannot eliminate: there are associated items with this category."))
                .when(categoryService).delete(id);

        mockMvc.perform(delete("/api/admin/categories/{id}", id))
                .andExpect(status().isConflict());

        verify(categoryService).delete(id);
    }
}
