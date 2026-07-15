package com.borrowly.mapper;

import com.borrowly.dto.response.ReviewResponse;
import com.borrowly.model.item.Category;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.item.ItemStatus;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.user.Review;
import com.borrowly.model.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReviewMapperTest {

    @Autowired
    private ReviewMapper mapper;

    @Test
    void toResponse_MapsReviewWithNestedObjects() {
        Category category = Category.builder()
                .name("Electronics")
                .description("Electronic devices")
                .build();

        User reviewer = User.register(
                "Mario",
                "Rossi",
                "mario@example.com",
                "HASH"
        );

        Item item = Item.builder()
                .title("Camera")
                .description("Mirrorless camera")
                .pricePerDay(BigDecimal.valueOf(15))
                .depositAmount(BigDecimal.valueOf(200))
                .finePerDay(BigDecimal.valueOf(20))
                .condition(ItemCondition.GOOD)
                .status(ItemStatus.ACTIVE)
                .owner(reviewer)
                .category(category)
                .build();

        Rental rental = Rental.builder()
                .item(item)
                .borrower(reviewer)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .itemTitle(item.getTitle())
                .dailyPrice(item.getPricePerDay())
                .depositAmount(item.getDepositAmount())
                .finePerDay(item.getFinePerDay())
                .totalPrice(BigDecimal.valueOf(45))
                .build();

        Review review = Review.builder()
                .rating(5)
                .comment("Great item!")
                .rental(rental)
                .reviewer(reviewer)
                .build();

        ReviewResponse response = mapper.toResponse(review);

        assertThat(response).isNotNull();

        assertThat(response.id())
                .isEqualTo(review.getId());

        assertThat(response.rating())
                .isEqualTo(5);

        assertThat(response.comment())
                .isEqualTo("Great item!");

        assertThat(response.rentalId())
                .isEqualTo(rental.getId());

        assertThat(response.item())
                .isNotNull();

        assertThat(response.item().id())
                .isEqualTo(item.getId());

        assertThat(response.item().title())
                .isEqualTo("Camera");

        assertThat(response.reviewer())
                .isNotNull();

        assertThat(response.reviewer().id())
                .isEqualTo(reviewer.getId());

        assertThat(response.reviewer().firstName())
                .isEqualTo("Mario");

        assertThat(response.reviewer().lastName())
                .isEqualTo("Rossi");
    }
    @Test
    void toResponse_AllowsNullComment() {
        User reviewer = User.register(
                "Mario",
                "Rossi",
                "mario@example.com",
                "HASH"
        );

        Item item = Item.builder()
                .title("Camera")
                .description("Camera")
                .pricePerDay(BigDecimal.TEN)
                .depositAmount(BigDecimal.valueOf(100))
                .finePerDay(BigDecimal.valueOf(10))
                .condition(ItemCondition.GOOD)
                .status(ItemStatus.ACTIVE)
                .owner(reviewer)
                .category(Category.builder()
                        .name("Electronics")
                        .description("Devices")
                        .build())
                .build();

        Rental rental = Rental.builder()
                .item(item)
                .borrower(reviewer)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .itemTitle(item.getTitle())
                .dailyPrice(item.getPricePerDay())
                .depositAmount(item.getDepositAmount())
                .finePerDay(item.getFinePerDay())
                .totalPrice(BigDecimal.TEN)
                .build();

        Review review = Review.builder()
                .rating(4)
                .comment(null)
                .rental(rental)
                .reviewer(reviewer)
                .build();

        ReviewResponse response = mapper.toResponse(review);

        assertThat(response.comment()).isNull();
        assertThat(response.rating()).isEqualTo(4);
    }
}