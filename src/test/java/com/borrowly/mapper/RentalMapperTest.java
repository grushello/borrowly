package com.borrowly.mapper;

import com.borrowly.dto.response.RentalResponse;
import com.borrowly.model.item.Category;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.item.ItemImage;
import com.borrowly.model.item.ItemStatus;
import com.borrowly.model.rental.Rental;
import com.borrowly.model.rental.RentalStatus;
import com.borrowly.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class RentalMapperTest {

    RentalMapper mapper;

    private User owner;
    private User borrower;
    private Category category;
    private Item item;
    private ItemImage primaryImage;
    private Rental rental;

    @BeforeEach
    void setUp() {
        ItemMapperImpl itemMapper = new ItemMapperImpl();
        ReflectionTestUtils.setField(itemMapper, "userMapper", new UserMapperImpl());
        ReflectionTestUtils.setField(itemMapper, "categoryMapper", new CategoryMapperImpl());
        ReflectionTestUtils.setField(itemMapper, "itemImageMapper", new ItemImageMapperImpl());

        RentalMapperImpl impl = new RentalMapperImpl();
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

        rental = Rental.builder()
                .item(item)
                .borrower(borrower)
                .startDate(LocalDate.of(2026, Month.JANUARY, 10))
                .endDate(LocalDate.of(2026, Month.JANUARY, 15))
                .itemTitle("Cordless Drill (at rental time)")
                .dailyPrice(new BigDecimal("12.50"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("5.00"))
                .totalPrice(new BigDecimal("62.50"))
                .status(RentalStatus.ACTIVE)
                .build();
    }

    @Test
    void toResponse_preservesAllSnapshotFields() {
        RentalResponse response = mapper.toResponse(rental);

        assertThat(response.id()).isEqualTo(rental.getId());
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, Month.JANUARY, 10));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, Month.JANUARY, 15));
        assertThat(response.actualReturnDate()).isNull();
        assertThat(response.itemTitle()).isEqualTo("Cordless Drill (at rental time)");
        assertThat(response.dailyPrice()).isEqualByComparingTo(new BigDecimal("12.50"));
        assertThat(response.depositAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(response.finePerDay()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(response.totalPrice()).isEqualByComparingTo(new BigDecimal("62.50"));
        assertThat(response.status()).isEqualTo(RentalStatus.ACTIVE);
    }

    @Test
    void toResponse_embedsItemSummary() {
        RentalResponse response = mapper.toResponse(rental);

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
    void toResponse_snapshotTitleCanDifferFromCurrentItemTitle() {
        RentalResponse response = mapper.toResponse(rental);

        assertThat(response.itemTitle()).isEqualTo("Cordless Drill (at rental time)");
        assertThat(response.item().title()).isEqualTo("Cordless Drill");
        assertThat(response.itemTitle()).isNotEqualTo(response.item().title());
    }

    @Test
    void toResponse_embedsBorrowerSummary() {
        RentalResponse response = mapper.toResponse(rental);

        assertThat(response.borrower()).isNotNull();
        assertThat(response.borrower().id()).isEqualTo(borrower.getId());
        assertThat(response.borrower().firstName()).isEqualTo("Bob");
        assertThat(response.borrower().lastName()).isEqualTo("Jones");
    }

    @Test
    void toResponse_mapsActualReturnDateWhenReturned() {
        rental.returnItem(LocalDate.of(2026, Month.JANUARY, 14));

        RentalResponse response = mapper.toResponse(rental);

        assertThat(response.actualReturnDate()).isEqualTo(LocalDate.of(2026, Month.JANUARY, 14));
        assertThat(response.status()).isEqualTo(RentalStatus.RETURNED);
    }
}