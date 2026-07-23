package com.borrowly.mapper;

import com.borrowly.dto.response.RentalRequestResponse;
import com.borrowly.model.item.Category;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.item.ItemImage;
import com.borrowly.model.item.ItemStatus;
import com.borrowly.model.rental.RentalRequest;
import com.borrowly.model.rental.RentalRequestStatus;
import com.borrowly.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class RentalRequestMapperTest {

    RentalRequestMapper mapper;

    private User owner;
    private User borrower;
    private Category category;
    private Item item;
    private ItemImage primaryImage;
    private RentalRequest request;

    @BeforeEach
    void setUp() {
        ItemMapperImpl itemMapper = new ItemMapperImpl();
        ReflectionTestUtils.setField(itemMapper, "userMapper", new UserMapperImpl());
        ReflectionTestUtils.setField(itemMapper, "categoryMapper", new CategoryMapperImpl());
        ReflectionTestUtils.setField(itemMapper, "itemImageMapper", new ItemImageMapperImpl());

        RentalRequestMapperImpl impl = new RentalRequestMapperImpl();
        ReflectionTestUtils.setField(impl, "itemMapper", itemMapper);
        ReflectionTestUtils.setField(impl, "userMapper", new UserMapperImpl());
        mapper = impl;

        owner = User.register("Alice", "Smith", "alice@example.com", "hashed");
        borrower = User.register("Bob", "Jones", "bob@example.com", "hashed");

        category = Category.builder()
                .name("Power Tools")
                .description("Drills, saws, etc.")
                .build();

        primaryImage = ItemImage.builder()
                .imageData(new byte[]{1, 2, 3})
                .fileName("front.jpg")
                .contentType("image/jpeg")
                .primary(true)
                .build();

        item = Item.builder()
                .title("Cordless Drill")
                .description("18V cordless")
                .pricePerDay(new BigDecimal("12.50"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("5.00"))
                .condition(ItemCondition.GOOD)
                .status(ItemStatus.ACTIVE)
                .owner(owner)
                .category(category)
                .build();
        item.addImage(primaryImage);

        request = RentalRequest.builder()
                .item(item)
                .borrower(borrower)
                .startDate(LocalDate.of(2026, Month.JANUARY, 10))
                .endDate(LocalDate.of(2026, Month.JANUARY, 15))
                .status(RentalRequestStatus.PENDING)
                .build();
    }

    @Test
    void toResponse_mapsScalarFields() {
        RentalRequestResponse response = mapper.toResponse(request);

        assertThat(response.id()).isEqualTo(request.getId());
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, Month.JANUARY, 10));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, Month.JANUARY, 15));
        assertThat(response.status()).isEqualTo(RentalRequestStatus.PENDING);
    }

    @Test
    void toResponse_embedsItemSummary() {
        RentalRequestResponse response = mapper.toResponse(request);

        assertThat(response.item()).isNotNull();
        assertThat(response.item().id()).isEqualTo(item.getId());
        assertThat(response.item().title()).isEqualTo("Cordless Drill");
        assertThat(response.item().pricePerDay()).isEqualByComparingTo(new BigDecimal("12.50"));
        assertThat(response.item().condition()).isEqualTo(ItemCondition.GOOD);
        assertThat(response.item().itemStatus()).isEqualTo(ItemStatus.ACTIVE);
        assertThat(response.item().ownerName()).isEqualTo("Alice Smith");
        assertThat(response.item().primaryImageId()).isEqualTo(primaryImage.getId());
    }

    @Test
    void toResponse_embedsBorrowerSummary() {
        RentalRequestResponse response = mapper.toResponse(request);

        assertThat(response.borrower()).isNotNull();
        assertThat(response.borrower().id()).isEqualTo(borrower.getId());
        assertThat(response.borrower().firstName()).isEqualTo("Bob");
        assertThat(response.borrower().lastName()).isEqualTo("Jones");
    }

    @Test
    void toResponse_mapsStatusAfterApproval() {
        request.approve();

        RentalRequestResponse response = mapper.toResponse(request);

        assertThat(response.status()).isEqualTo(RentalRequestStatus.APPROVED);
    }
}