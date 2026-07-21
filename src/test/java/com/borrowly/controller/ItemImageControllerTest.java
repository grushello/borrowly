package com.borrowly.controller;

import com.borrowly.config.SecurityConfig;
import com.borrowly.dto.response.ItemImageResponse;
import com.borrowly.exception.GlobalExceptionHandler;
import com.borrowly.exception.InvalidImageException;
import com.borrowly.model.item.ItemImage;
import com.borrowly.security.AuthEntryPointJwt;
import com.borrowly.security.AuthTokenFilter;
import com.borrowly.security.JwtUtil;
import com.borrowly.service.auth.UserDetailsServiceImpl;
import com.borrowly.service.auth.AuthService;
import com.borrowly.service.item.ItemImageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemImageController.class)
@Import({SecurityConfig.class, AuthTokenFilter.class, AuthEntryPointJwt.class,
        GlobalExceptionHandler.class})
class ItemImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemImageService itemImageService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private final UUID itemId = UUID.randomUUID();
    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2025, Month.JANUARY, 1, 12, 0);

    @Test
    @WithMockUser
    @DisplayName("POST upload — 201 with ItemImageResponse")
    void uploadReturns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[100]);

        ItemImageResponse response = new ItemImageResponse(
                UUID.randomUUID(), "photo.jpg", "image/jpeg", true, FIXED_TIME
        );
        when(itemImageService.upload(eq(itemId), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/items/{itemId}/images", itemId).file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("photo.jpg"))
                .andExpect(jsonPath("$.primary").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("POST upload — invalid content type returns 400")
    void uploadInvalidContentTypeReturns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[100]);

        when(itemImageService.upload(eq(itemId), any()))
                .thenThrow(new InvalidImageException("Unsupported content type. Allowed: JPEG, PNG, WebP"));

        mockMvc.perform(multipart("/api/items/{itemId}/images", itemId).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported content type. Allowed: JPEG, PNG, WebP"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST upload — file exceeding 5 MB returns 400")
    void uploadFileTooLargeReturns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", new byte[100]);

        when(itemImageService.upload(eq(itemId), any()))
                .thenThrow(new InvalidImageException("File size exceeds the 5 MB limit"));

        mockMvc.perform(multipart("/api/items/{itemId}/images", itemId).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File size exceeds the 5 MB limit"));
    }

    @Test
    @DisplayName("GET download — streams binary with correct headers")
    void downloadStreamsBinaryWithHeaders() throws Exception {
        UUID imageId = UUID.randomUUID();
        byte[] data = {1, 2, 3, 4, 5};
        ItemImage image = ItemImage.builder()
                .imageData(data)
                .fileName("photo.png")
                .contentType("image/png")
                .build();

        when(itemImageService.download(itemId, imageId)).thenReturn(image);

        mockMvc.perform(get("/api/items/{itemId}/images/{imageId}", itemId, imageId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline"))
                .andExpect(content().bytes(data));
    }

    @Test
    @DisplayName("GET list — public, returns 200")
    void listReturnsMetadata() throws Exception {
        mockMvc.perform(get("/api/items/{itemId}/images", itemId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE — returns 204")
    void deleteReturns204() throws Exception {
        UUID imageId = UUID.randomUUID();

        mockMvc.perform(delete("/api/items/{itemId}/images/{imageId}", itemId, imageId))
                .andExpect(status().isNoContent());
    }
}