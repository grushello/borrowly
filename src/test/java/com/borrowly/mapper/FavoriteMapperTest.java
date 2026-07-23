package com.borrowly.mapper;

import com.borrowly.dto.response.FavoriteResponse;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.item.ItemImage;
import com.borrowly.model.item.ItemStatus;
import com.borrowly.model.item.Category;
import com.borrowly.model.user.Favorite;
import com.borrowly.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FavoriteMapperTest {

    FavoriteMapper mapper;

    private User owner;
    private Item item;
    private ItemImage primaryImage;

    @BeforeEach
    void setUp() {
        ItemMapperImpl itemMapperImpl = new ItemMapperImpl();
        ReflectionTestUtils.setField(itemMapperImpl, "userMapper", new UserMapperImpl());
        ReflectionTestUtils.setField(itemMapperImpl, "categoryMapper", new CategoryMapperImpl());
        ReflectionTestUtils.setField(itemMapperImpl, "itemImageMapper", new ItemImageMapperImpl());

        FavoriteMapperImpl impl = new FavoriteMapperImpl();
        ReflectionTestUtils.setField(impl, "itemMapper", itemMapperImpl);
        mapper = impl;

        owner = User.register("Alice", "Smith", "alice@example.com", "hashed");

        Category category = Category.builder()
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
    }

    @Test
    void toResponse_mapsIdAndCreatedAtAndEmbeddedItemSummary() {
        Favorite favorite = Favorite.builder()
                .user(owner)
                .item(item)
                .build();

        FavoriteResponse response = mapper.toResponse(favorite);

        assertThat(response.id()).isEqualTo(favorite.getId());
        assertThat(response.createdAt()).isEqualTo(favorite.getCreatedAt());

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
    void toResponse_nullFavorite_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}