package com.borrowly.service.review;

import com.borrowly.dto.request.CreateReviewRequest;
import com.borrowly.dto.response.ReviewResponse;
import com.borrowly.exception.RentalNotFoundException;
import com.borrowly.exception.ReviewAlreadyExistsException;
import com.borrowly.exception.ReviewNotAllowedException;
import com.borrowly.mapper.ReviewMapper;
import com.borrowly.model.item.Item;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.model.user.Review;
import com.borrowly.model.user.User;
import com.borrowly.repository.rental.RentalRepository;
import com.borrowly.repository.user.ReviewRepository;
import com.borrowly.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private RentalRepository rentalRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewMapper reviewMapper;
    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User borrower;
    private User stranger;
    private Item item;
    private Rental returnedRental;
    private UUID returnedRentalId;
    private UUID itemId;

    @BeforeEach
    void setUp() {
        borrower = User.register("Alice", "Smith", "alice@example.com", "hashed");
        stranger = User.register("Bob", "Jones", "bob@example.com", "hashed");

        item = newItem();
        itemId = Objects.requireNonNull(item.getId());

        returnedRental = newRental(item, borrower, RentalStatus.RETURNED);
        returnedRentalId = Objects.requireNonNull(returnedRental.getId());
    }

    @Test
    @DisplayName("happy path — creates review for a RETURNED rental")
    void createReview_happyPath() {
        CreateReviewRequest request = new CreateReviewRequest(returnedRentalId, 4, "Great drill");
        ReviewResponse expected = new ReviewResponse(
                UUID.randomUUID(), null, null, 4, "Great drill", returnedRentalId, null);

        when(rentalRepository.findById(returnedRentalId)).thenReturn(Optional.of(returnedRental));
        when(currentUserProvider.getCurrentUser()).thenReturn(borrower);
        when(reviewRepository.existsByRentalId(returnedRentalId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewMapper.toResponse(any(Review.class))).thenReturn(expected);

        ReviewResponse result = reviewService.createReview(request);

        assertThat(result.rating()).isEqualTo(4);
        assertThat(result.comment()).isEqualTo("Great drill");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();
        assertThat(saved.getReviewer()).isSameAs(borrower);
        assertThat(saved.getRental()).isSameAs(returnedRental);
    }

    @Test
    @DisplayName("rental not found — throws RentalNotFoundException")
    void createReview_rentalNotFound() {
        UUID missing = UUID.randomUUID();
        CreateReviewRequest request = new CreateReviewRequest(missing, 3, null);
        when(rentalRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(request))
                .isInstanceOf(RentalNotFoundException.class);

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("rental not RETURNED — throws ReviewNotAllowedException (409)")
    void createReview_rentalNotReturned() {
        Rental active = newRental(item, borrower, RentalStatus.ACTIVE);
        UUID activeId = Objects.requireNonNull(active.getId());

        CreateReviewRequest request = new CreateReviewRequest(activeId, 5, null);
        when(rentalRepository.findById(activeId)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> reviewService.createReview(request))
                .isInstanceOf(ReviewNotAllowedException.class)
                .hasMessageContaining("RETURNED");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("authenticated user is not the borrower — throws AccessDeniedException (403)")
    void createReview_wrongUser() {
        CreateReviewRequest request = new CreateReviewRequest(returnedRentalId, 3, null);
        when(rentalRepository.findById(returnedRentalId)).thenReturn(Optional.of(returnedRental));
        when(currentUserProvider.getCurrentUser()).thenReturn(stranger);

        assertThatThrownBy(() -> reviewService.createReview(request))
                .isInstanceOf(AccessDeniedException.class);

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("duplicate review for the same rental — throws ReviewAlreadyExistsException (409)")
    void createReview_duplicateReview() {
        CreateReviewRequest request = new CreateReviewRequest(returnedRentalId, 2, null);
        when(rentalRepository.findById(returnedRentalId)).thenReturn(Optional.of(returnedRental));
        when(currentUserProvider.getCurrentUser()).thenReturn(borrower);
        when(reviewRepository.existsByRentalId(returnedRentalId)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(request))
                .isInstanceOf(ReviewAlreadyExistsException.class);

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("list reviews by item returns reviews from multiple rentals of the same item")
    void getReviewsByItem_multipleRentals() {
        Pageable pageable = PageRequest.of(0, 10);

        User anotherBorrower = User.register("Charlie", "Brown", "charlie@example.com", "hashed");
        Rental rental2 = newRental(item, anotherBorrower, RentalStatus.RETURNED);
        UUID rental2Id = Objects.requireNonNull(rental2.getId());

        Review r1 = Review.builder().rental(returnedRental).reviewer(borrower).rating(5).build();
        Review r2 = Review.builder().rental(rental2).reviewer(anotherBorrower).rating(3).build();

        ReviewResponse resp1 = new ReviewResponse(r1.getId(), null, null, 5, null, returnedRentalId, null);
        ReviewResponse resp2 = new ReviewResponse(r2.getId(), null, null, 3, null, rental2Id, null);

        when(reviewRepository.findByItemIdOrderByCreatedAtDesc(itemId, pageable))
                .thenReturn(new PageImpl<>(List.of(r1, r2)));
        when(reviewMapper.toResponse(r1)).thenReturn(resp1);
        when(reviewMapper.toResponse(r2)).thenReturn(resp2);

        Page<ReviewResponse> result = reviewService.getReviewsByItem(itemId, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(ReviewResponse::rentalId)
                .containsExactly(returnedRentalId, rental2Id);
    }

    private static <T> T newInstance(Class<T> clazz) {
        try {
            Constructor<T> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Item newItem() {
        Item i = newInstance(Item.class);
        ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(i, "title", "Drill");
        return i;
    }

    private static Rental newRental(Item item, User borrower, RentalStatus status) {
        Rental r = newInstance(Rental.class);
        ReflectionTestUtils.setField(r, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(r, "item", item);
        ReflectionTestUtils.setField(r, "borrower", borrower);
        ReflectionTestUtils.setField(r, "status", status);
        return r;
    }
}