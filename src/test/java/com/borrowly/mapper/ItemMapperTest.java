package com.borrowly.mapper;

import com.borrowly.dto.request.CreateItemRequest;
import com.borrowly.dto.request.UpdateItemRequest;
import com.borrowly.dto.response.ItemResponse;
import com.borrowly.dto.response.ItemSummaryResponse;
import com.borrowly.model.item.*;
import com.borrowly.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ItemMapperTest {

    ItemMapper mapper;

    private User owner;
    private Category category;
    private Item item;
    private ItemImage primaryImage;
    private ItemImage secondaryImage;

    @BeforeEach
    void setUp() {
        ItemMapperImpl impl = new ItemMapperImpl();
        ReflectionTestUtils.setField(impl, "userMapper", new UserMapperImpl());
        ReflectionTestUtils.setField(impl, "categoryMapper", new CategoryMapperImpl());
        ReflectionTestUtils.setField(impl, "itemImageMapper", new ItemImageMapperImpl());
        mapper = impl;

        owner = User.register("Alice", "Smith", "alice@example.com", "hashed");

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

        secondaryImage = ItemImage.builder()
                .imageData(new byte[]{4, 5, 6})
                .fileName("side.jpg")
                .contentType("image/jpeg")
                .primary(false)
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
        item.addImage(secondaryImage);
    }

    @Test
    void toResponse_mapsAllFieldsIncludingNestedOwnerAndCategory() {
        ItemResponse response = mapper.toResponse(item);

        assertThat(response.id()).isEqualTo(item.getId());
        assertThat(response.title()).isEqualTo("Cordless Drill");
        assertThat(response.description()).isEqualTo("18V cordless");
        assertThat(response.pricePerDay()).isEqualByComparingTo(new BigDecimal("12.50"));
        assertThat(response.depositAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(response.finePerDay()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(response.condition()).isEqualTo(ItemCondition.GOOD);
        assertThat(response.itemStatus()).isEqualTo(ItemStatus.ACTIVE);

        assertThat(response.owner()).isNotNull();
        assertThat(response.owner().id()).isEqualTo(owner.getId());
        assertThat(response.owner().firstName()).isEqualTo("Alice");
        assertThat(response.owner().lastName()).isEqualTo("Smith");

        assertThat(response.category()).isNotNull();
        assertThat(response.category().id()).isEqualTo(category.getId());
        assertThat(response.category().name()).isEqualTo("Power Tools");

        assertThat(response.averageRating()).isNull();
        assertThat(response.reviewCount()).isZero();
    }

    @Test
    void toResponse_imagesListIsPreserved() {
        ItemResponse response = mapper.toResponse(item);

        assertThat(response.images()).hasSize(2);
        assertThat(response.images().get(0).id()).isEqualTo(primaryImage.getId());
        assertThat(response.images().get(0).fileName()).isEqualTo("front.jpg");
        assertThat(response.images().get(0).primary()).isTrue();
        assertThat(response.images().get(1).id()).isEqualTo(secondaryImage.getId());
        assertThat(response.images().get(1).primary()).isFalse();
    }

    @Test
    void toResponse_withAverageRatingAndReviewCount_populatesAggregates() {
        ItemResponse response = mapper.toResponse(item, 4.3, 17);

        assertThat(response.averageRating()).isEqualTo(4.3);
        assertThat(response.reviewCount()).isEqualTo(17);
        assertThat(response.title()).isEqualTo("Cordless Drill");
        assertThat(response.owner()).isNotNull();
    }

    @Test
    void toSummary_mapsOwnerNameAndPrimaryImageId() {
        ItemSummaryResponse summary = mapper.toSummary(item);

        assertThat(summary.id()).isEqualTo(item.getId());
        assertThat(summary.title()).isEqualTo("Cordless Drill");
        assertThat(summary.pricePerDay()).isEqualByComparingTo(new BigDecimal("12.50"));
        assertThat(summary.condition()).isEqualTo(ItemCondition.GOOD);
        assertThat(summary.itemStatus()).isEqualTo(ItemStatus.ACTIVE);
        assertThat(summary.ownerName()).isEqualTo("Alice Smith");
        assertThat(summary.primaryImageId()).isEqualTo(primaryImage.getId());
    }

    @Test
    void toSummary_noPrimaryImage_returnsNullPrimaryImageId() {
        Item noPrimary = Item.builder()
                .title("Ladder")
                .pricePerDay(new BigDecimal("8.00"))
                .depositAmount(new BigDecimal("30.00"))
                .finePerDay(new BigDecimal("3.00"))
                .condition(ItemCondition.FAIR)
                .status(ItemStatus.ACTIVE)
                .owner(owner)
                .category(category)
                .build();

        ItemSummaryResponse summary = mapper.toSummary(noPrimary);

        assertThat(summary.primaryImageId()).isNull();
    }

    @Test
    void toEntity_mapsScalarFieldsOnly() {
        CreateItemRequest request = new CreateItemRequest(
                "Jigsaw",
                "Variable speed",
                new BigDecimal("9.00"),
                new BigDecimal("25.00"),
                new BigDecimal("4.00"),
                ItemCondition.LIKE_NEW,
                UUID.randomUUID()
        );

        Item entity = mapper.toEntity(request);

        assertThat(entity.getTitle()).isEqualTo("Jigsaw");
        assertThat(entity.getDescription()).isEqualTo("Variable speed");
        assertThat(entity.getPricePerDay()).isEqualByComparingTo(new BigDecimal("9.00"));
        assertThat(entity.getDepositAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(entity.getFinePerDay()).isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(entity.getCondition()).isEqualTo(ItemCondition.LIKE_NEW);

        assertThat(entity.getOwner()).isNull();
        assertThat(entity.getCategory()).isNull();
        assertThat(entity.getVersion()).isNull();
    }

    @Test
    void updateEntity_partialUpdate_leavesNonProvidedFieldsUnchanged() {
        UpdateItemRequest request = new UpdateItemRequest(
                "Updated Drill",
                null,
                new BigDecimal("15.00"),
                null,
                null,
                null,
                null
        );

        mapper.updateEntity(item, request);

        assertThat(item.getTitle()).isEqualTo("Updated Drill");
        assertThat(item.getDescription()).isEqualTo("18V cordless");
        assertThat(item.getPricePerDay()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(item.getDepositAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(item.getFinePerDay()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(item.getCondition()).isEqualTo(ItemCondition.GOOD);
    }

    @Test
    void updateEntity_neverTouchesImmutableFields() {
        UUID originalId = item.getId();
        UpdateItemRequest request = new UpdateItemRequest(
                "New Title", null, null, null, null, null, null
        );

        mapper.updateEntity(item, request);

        assertThat(item.getId()).isEqualTo(originalId);
        assertThat(item.getOwner()).isEqualTo(owner);
        assertThat(item.getCategory()).isEqualTo(category);
        assertThat(item.getStatus()).isEqualTo(ItemStatus.ACTIVE);
    }
}