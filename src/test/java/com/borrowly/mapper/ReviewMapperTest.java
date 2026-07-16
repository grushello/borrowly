package com.borrowly.mapper;

import com.borrowly.dto.response.ReviewResponse;
import com.borrowly.model.item.Category;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.item.ItemStatus;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.user.Review;
import com.borrowly.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewMapperTest {

    private ReviewMapper mapper;

    private User reviewer;
    private Item item;
    private Rental rental;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = new UserMapperImpl();
        CategoryMapper categoryMapper = new CategoryMapperImpl();
        ItemImageMapper itemImageMapper = new ItemImageMapperImpl();

        ItemMapper itemMapper = new ItemMapperImpl();

        ReflectionTestUtils.setField(itemMapper, "userMapper", userMapper);
        ReflectionTestUtils.setField(itemMapper, "categoryMapper", categoryMapper);
        ReflectionTestUtils.setField(itemMapper, "itemImageMapper", itemImageMapper);

        mapper = new ReviewMapperImpl();

        ReflectionTestUtils.setField(mapper, "itemMapper", itemMapper);
        ReflectionTestUtils.setField(mapper, "userMapper", userMapper);

        reviewer = User.register(
                "Mario",
                "Rossi",
                "mario@example.com",
                "HASH"
        );

        Category category = Category.builder()
                .name("Electronics")
                .description("Devices")
                .build();

        item = Item.builder()
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

        rental = Rental.builder()
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
    }

    @Test
    void toResponse_MapsNestedObjects() {
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

    @Test
    void toResponse_NullEntityReturnsNull() {
        assertThat(mapper.toResponse(null))
                .isNull();
    }
}