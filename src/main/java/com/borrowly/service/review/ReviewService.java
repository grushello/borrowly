package com.borrowly.service.review;

import com.borrowly.dto.request.CreateReviewRequest;
import com.borrowly.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse createReview(CreateReviewRequest request);

    Page<ReviewResponse> getReviewsByItem(UUID itemId, Pageable pageable);
}