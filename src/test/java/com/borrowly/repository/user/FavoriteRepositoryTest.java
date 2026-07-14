package com.borrowly.repository.user;

import com.borrowly.model.item.*;
import com.borrowly.model.user.Favorite;
import com.borrowly.model.user.User;
import com.borrowly.repository.item.ItemRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FavoriteRepositoryTest {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private Category category;
    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {

        user = User.register(
                "John",
                "Doe",
                "john@example.com",
                "password"
        );

        entityManager.persist(user);

        category = Category.builder()
                .name("Electronics")
                .description("Electronics")
                .build();

        item1 = itemRepository.save(
                Item.builder()
                        .title("Laptop")
                        .description("Gaming laptop")
                        .pricePerDay(BigDecimal.valueOf(20))
                        .depositAmount(BigDecimal.valueOf(100))
                        .finePerDay(BigDecimal.valueOf(5))
                        .condition(ItemCondition.GOOD)
                        .category(category)
                        .owner(user)
                        .build()
        );

        item2 = itemRepository.save(
                Item.builder()
                        .title("Camera")
                        .description("DSLR")
                        .pricePerDay(BigDecimal.valueOf(15))
                        .depositAmount(BigDecimal.valueOf(80))
                        .finePerDay(BigDecimal.valueOf(4))
                        .condition(ItemCondition.GOOD)
                        .category(category)
                        .owner(user)
                        .build()
        );

        entityManager.flush();
    }

    @Test
    void shouldSaveFavorite() {

        Favorite favorite = Favorite.builder()
                .user(user)
                .item(item1)
                .build();

        Favorite saved = favoriteRepository.saveAndFlush(favorite);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindFavoritesOrderedByCreatedAtDesc() throws Exception {

        Favorite first = favoriteRepository.saveAndFlush(
                Favorite.builder()
                        .user(user)
                        .item(item1)
                        .build()
        );

        Thread.sleep(10);

        Favorite second = favoriteRepository.saveAndFlush(
                Favorite.builder()
                        .user(user)
                        .item(item2)
                        .build()
        );

        Page<Favorite> page =
                favoriteRepository.findByUser_IdOrderByCreatedAtDesc(
                        user.getId(),
                        PageRequest.of(0, 10)
                );

        assertThat(page.getContent()).containsExactly(second, first);
    }

    @Test
    void shouldReturnTrueWhenFavoriteExists() {

        favoriteRepository.saveAndFlush(
                Favorite.builder()
                        .user(user)
                        .item(item1)
                        .build()
        );

        assertThat(
                favoriteRepository.existsByUser_IdAndItem_Id(
                        user.getId(),
                        item1.getId()
                )
        ).isTrue();
    }

    @Test
    void shouldReturnFalseWhenFavoriteDoesNotExist() {

        assertThat(
                favoriteRepository.existsByUser_IdAndItem_Id(
                        user.getId(),
                        item1.getId()
                )
        ).isFalse();
    }

    @Test
    void shouldDeleteFavorite() {

        favoriteRepository.saveAndFlush(
                Favorite.builder()
                        .user(user)
                        .item(item1)
                        .build()
        );

        int deleted = favoriteRepository.deleteByUser_IdAndItem_Id(
                user.getId(),
                item1.getId()
        );

        entityManager.flush();

        assertThat(deleted).isEqualTo(1);

        assertThat(
                favoriteRepository.existsByUser_IdAndItem_Id(
                        user.getId(),
                        item1.getId()
                )
        ).isFalse();
    }

    @Test
    void shouldReturnZeroWhenFavoriteDoesNotExist() {

        int deleted = favoriteRepository.deleteByUser_IdAndItem_Id(
                user.getId(),
                item1.getId()
        );

        assertThat(deleted).isZero();
    }

    @Test
    void shouldThrowWhenDuplicateFavoriteInserted() {

        favoriteRepository.saveAndFlush(
                Favorite.builder()
                        .user(user)
                        .item(item1)
                        .build()
        );

        assertThatThrownBy(() ->
                favoriteRepository.saveAndFlush(
                        Favorite.builder()
                                .user(user)
                                .item(item1)
                                .build()
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}