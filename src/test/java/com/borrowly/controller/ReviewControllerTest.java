package com.borrowly.controller;

import com.borrowly.config.SecurityConfig;
import com.borrowly.dto.request.CreateReviewRequest;
import com.borrowly.dto.response.ReviewResponse;
import com.borrowly.exception.GlobalExceptionHandler;
import com.borrowly.security.AuthEntryPointJwt;
import com.borrowly.security.AuthTokenFilter;
import com.borrowly.security.JwtUtil;
import com.borrowly.service.review.ReviewService;
import com.borrowly.service.auth.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(ReviewController.class)
@Import({SecurityConfig.class, AuthTokenFilter.class, AuthEntryPointJwt.class,
        GlobalExceptionHandler.class})
class ReviewControllerTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, Month.JANUARY, 15, 12, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("GET /api/items/{itemId}/reviews is public — returns 200 without auth")
    void getItemReviews_publicAccess() throws Exception {
        UUID itemId = UUID.randomUUID();
        ReviewResponse review = new ReviewResponse(
                UUID.randomUUID(), null, null, 4, "Nice", UUID.randomUUID(),
                FIXED_TIME);
        Page<ReviewResponse> page = new PageImpl<>(List.of(review));

        when(reviewService.getReviewsByItem(any(UUID.class), any())).thenReturn(page);

        mockMvc.perform(get("/api/items/{itemId}/reviews", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(4));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/reviews with valid data returns 201")
    void createReview_validRequest() throws Exception {
        UUID rentalId = UUID.randomUUID();
        CreateReviewRequest request = new CreateReviewRequest(rentalId, 4, "Great item");
        ReviewResponse response = new ReviewResponse(
                UUID.randomUUID(), null, null, 4, "Great item", rentalId,
                FIXED_TIME);

        when(reviewService.createReview(any())).thenReturn(response);

        mockMvc.perform(post("/api/reviews")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Great item"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/reviews with rating 0 fails validation — 400")
    void createReview_ratingZero_rejected() throws Exception {
        String json = """
                {"rentalId":"%s","rating":0}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/reviews")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/reviews with rating 6 fails validation — 400")
    void createReview_ratingSix_rejected() throws Exception {
        String json = """
                {"rentalId":"%s","rating":6}
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/reviews")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/reviews with rating 1 passes validation")
    void createReview_ratingOne_accepted() throws Exception {
        UUID rentalId = UUID.randomUUID();
        CreateReviewRequest request = new CreateReviewRequest(rentalId, 1, null);
        ReviewResponse response = new ReviewResponse(
                UUID.randomUUID(), null, null, 1, null, rentalId, FIXED_TIME);

        when(reviewService.createReview(any())).thenReturn(response);

        mockMvc.perform(post("/api/reviews")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/reviews with rating 5 passes validation")
    void createReview_ratingFive_accepted() throws Exception {
        UUID rentalId = UUID.randomUUID();
        CreateReviewRequest request = new CreateReviewRequest(rentalId, 5, "Perfect");
        ReviewResponse response = new ReviewResponse(
                UUID.randomUUID(), null, null, 5, "Perfect", rentalId, FIXED_TIME);

        when(reviewService.createReview(any())).thenReturn(response);

        mockMvc.perform(post("/api/reviews")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    @DisplayName("POST /api/reviews without auth returns 401")
    void createReview_noAuth_returns401() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(UUID.randomUUID(), 3, null);

        mockMvc.perform(post("/api/reviews")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}