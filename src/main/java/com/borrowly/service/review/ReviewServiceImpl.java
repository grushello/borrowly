package com.borrowly.service.review;

import com.borrowly.dto.request.CreateReviewRequest;
import com.borrowly.dto.response.ReviewResponse;
import com.borrowly.exception.RentalNotFoundException;
import com.borrowly.exception.ReviewAlreadyExistsException;
import com.borrowly.exception.ReviewNotAllowedException;
import com.borrowly.mapper.ReviewMapper;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.model.user.Review;
import com.borrowly.model.user.User;
import com.borrowly.repository.rental.RentalRepository;
import com.borrowly.repository.user.ReviewRepository;
import com.borrowly.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final RentalRepository rentalRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        Rental rental = rentalRepository.findById(request.rentalId())
                .orElseThrow(() -> new RentalNotFoundException(request.rentalId()));

        if (rental.getStatus() != RentalStatus.RETURNED) {
            throw new ReviewNotAllowedException("Rental must be RETURNED before reviewing");
        }

        User currentUser = currentUserProvider.getCurrentUser();
        if (!Objects.equals(rental.getBorrower().getId(), currentUser.getId())) {
            throw new AccessDeniedException("Only the borrower can review this rental");
        }

        if (reviewRepository.existsByRentalId(rental.getId())) {
            throw new ReviewAlreadyExistsException(rental.getId());
        }

        Review review = Review.builder()
                .rental(rental)
                .reviewer(rental.getBorrower())
                .rating(request.rating())
                .comment(request.comment())
                .build();

        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByItem(UUID itemId, Pageable pageable) {
        return reviewRepository
                .findByItemIdOrderByCreatedAtDesc(itemId, pageable)
                .map(reviewMapper::toResponse);
    }
}