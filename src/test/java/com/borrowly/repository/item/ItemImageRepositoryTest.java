package com.borrowly.repository.item;


import com.borrowly.model.item.*;
import com.borrowly.model.user.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ItemImageRepositoryTest {


    @Autowired
    private ItemImageRepository itemImageRepository;


    @Autowired
    private EntityManager entityManager;


    private Item item;


    @BeforeEach
    void setUp() {

        User owner = User.register(
                "John",
                "Doe",
                "john@example.com",
                "password"
        );

        entityManager.persist(owner);


        Category category = Category.builder()
                .name("Electronics")
                .description("Devices")
                .build();

        entityManager.persist(category);


        item = Item.builder()
                .title("Laptop")
                .description("Gaming laptop")
                .pricePerDay(BigDecimal.valueOf(20))
                .depositAmount(BigDecimal.valueOf(100))
                .finePerDay(BigDecimal.valueOf(5))
                .condition(ItemCondition.GOOD)
                .category(category)
                .owner(owner)
                .build();

        entityManager.persist(item);

        entityManager.flush();
    }


    @Test
    void shouldSaveAndReloadImage() {

        ItemImage image =
                ItemImage.builder()
                        .imageData(new byte[]{1, 2, 3})
                        .fileName("photo.png")
                        .contentType("image/png")
                        .primary(true)
                        .item(item)
                        .build();


        ItemImage saved =
                itemImageRepository.saveAndFlush(image);


        entityManager.clear();


        ItemImage reloaded =
                itemImageRepository.findById(saved.getId())
                        .orElseThrow();


        assertThat(reloaded.getImageData())
                .containsExactly(1, 2, 3);

        assertThat(reloaded.getFileName())
                .isEqualTo("photo.png");
    }


    @Test
    void shouldReturnMetadataOrderedByCreatedAtAscending() {

        itemImageRepository.saveAndFlush(
                        ItemImage.builder()
                                .imageData(new byte[]{1})
                                .fileName("first.png")
                                .contentType("image/png")
                                .primary(false)
                                .item(item)
                                .build()
        );

        itemImageRepository.saveAndFlush(
                        ItemImage.builder()
                                .imageData(new byte[]{2})
                                .fileName("second.png")
                                .contentType("image/png")
                                .primary(true)
                                .item(item)
                                .build()
        );


        List<ItemImageMetadata> result =
                itemImageRepository.findByItem_IdOrderByCreatedAtAsc(
                        item.getId()
                );


        assertThat(result)
                .hasSize(2);


        assertThat(result.get(0).getFileName())
                .isEqualTo("first.png");


        assertThat(result.get(1).getFileName())
                .isEqualTo("second.png");
    }


    @Test
    void shouldFindPrimaryImageMetadata() {


        itemImageRepository.saveAndFlush(
                ItemImage.builder()
                        .imageData(new byte[]{1})
                        .fileName("normal.png")
                        .contentType("image/png")
                        .primary(false)
                        .item(item)
                        .build()
        );


        itemImageRepository.saveAndFlush(
                ItemImage.builder()
                        .imageData(new byte[]{2})
                        .fileName("primary.png")
                        .contentType("image/png")
                        .primary(true)
                        .item(item)
                        .build()
        );


        Optional<ItemImageMetadata> result =
                itemImageRepository.findByItem_IdAndPrimaryTrue(
                        item.getId()
                );


        assertThat(result)
                .isPresent();


        assertThat(result.get().getFileName())
                .isEqualTo("primary.png");
    }


    @Test
    void shouldCountImagesForItem() {


        itemImageRepository.saveAndFlush(
                ItemImage.builder()
                        .imageData(new byte[]{1})
                        .fileName("one.png")
                        .contentType("image/png")
                        .primary(false)
                        .item(item)
                        .build()
        );


        itemImageRepository.saveAndFlush(
                ItemImage.builder()
                        .imageData(new byte[]{2})
                        .fileName("two.png")
                        .contentType("image/png")
                        .primary(false)
                        .item(item)
                        .build()
        );


        long count =
                itemImageRepository.countByItem_Id(
                        item.getId()
                );


        assertThat(count)
                .isEqualTo(2);
    }


    @Test
    void shouldLoadFullImageForStreamingEndpoint() {


        ItemImage saved =
                itemImageRepository.saveAndFlush(
                        ItemImage.builder()
                                .imageData(new byte[]{10,20,30})
                                .fileName("stream.png")
                                .contentType("image/png")
                                .primary(true)
                                .item(item)
                                .build()
                );


        entityManager.clear();


        ItemImage image =
                itemImageRepository.findByIdAndItem_Id(
                                saved.getId(),
                                item.getId()
                        )
                        .orElseThrow();


        assertThat(image.getImageData())
                .containsExactly(10,20,30);
    }
}