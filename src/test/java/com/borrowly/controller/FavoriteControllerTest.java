package com.borrowly.controller;

import com.borrowly.config.SecurityConfig;
import com.borrowly.dto.response.FavoriteResponse;
import com.borrowly.exception.CannotFavoriteOwnItemException;
import com.borrowly.exception.GlobalExceptionHandler;
import com.borrowly.exception.ItemNotFoundException;
import com.borrowly.security.AuthEntryPointJwt;
import com.borrowly.security.AuthTokenFilter;
import com.borrowly.security.JwtUtil;
import com.borrowly.service.auth.UserDetailsServiceImpl;
import com.borrowly.service.favorite.FavoriteResult;
import com.borrowly.service.favorite.FavoriteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FavoriteController.class)
@Import({SecurityConfig.class, AuthTokenFilter.class, AuthEntryPointJwt.class,
        GlobalExceptionHandler.class})
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoriteService favoriteService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private FavoriteResponse favoriteResponse() {
        return new FavoriteResponse(UUID.randomUUID(), null, LocalDateTime.now());
    }

    @Test
    @DisplayName("POST without authentication returns 401")
    void addUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(post("/api/favorites/{itemId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        verify(favoriteService, never()).addFavorite(any());
    }

    @Test
    @WithMockUser
    @DisplayName("POST returns 201 on first add")
    void addReturns201OnFirstAdd() throws Exception {
        when(favoriteService.addFavorite(any()))
                .thenReturn(new FavoriteResult(favoriteResponse(), true));

        mockMvc.perform(post("/api/favorites/{itemId}", UUID.randomUUID()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    @DisplayName("POST returns 200 when the item is already favorited")
    void addReturns200OnDuplicate() throws Exception {
        when(favoriteService.addFavorite(any()))
                .thenReturn(new FavoriteResult(favoriteResponse(), false));

        mockMvc.perform(post("/api/favorites/{itemId}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("POST returns 404 when the item does not exist")
    void addReturns404WhenItemMissing() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(favoriteService.addFavorite(itemId)).thenThrow(new ItemNotFoundException(itemId));

        mockMvc.perform(post("/api/favorites/{itemId}", itemId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("POST returns 403 when favoriting your own item")
    void addReturns403OnOwnItem() throws Exception {
        when(favoriteService.addFavorite(any()))
                .thenThrow(new CannotFavoriteOwnItemException());

        mockMvc.perform(post("/api/favorites/{itemId}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE returns 204")
    void removeReturns204() throws Exception {
        mockMvc.perform(delete("/api/favorites/{itemId}", UUID.randomUUID()))
                .andExpect(status().isNoContent());

        verify(favoriteService).removeFavorite(any());
    }

    @Test
    @WithMockUser
    @DisplayName("GET returns 200 with a page of favorites")
    void listReturnsPage() throws Exception {
        Page<FavoriteResponse> page = new PageImpl<>(List.of(favoriteResponse()));
        when(favoriteService.listForCurrentUser(any())).thenReturn(page);

        mockMvc.perform(get("/api/favorites?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").exists());
    }
}