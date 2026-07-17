package com.borrowly.controller;

import com.borrowly.dto.request.CreateReviewRequest;
import com.borrowly.dto.response.ReviewResponse;
import com.borrowly.service.review.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reviewService.createReview(request));
    }

    @GetMapping("/items/{itemId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getItemReviews(
            @PathVariable UUID itemId,
            Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByItem(itemId, pageable));
    }
}