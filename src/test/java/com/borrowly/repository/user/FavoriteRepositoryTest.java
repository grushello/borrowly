package com.borrowly.repository.user;

import com.borrowly.model.item.Category;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.user.Favorite;
import com.borrowly.model.user.User;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


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
                "passwordHash"
        );

        entityManager.persist(user);


        category = Category.builder()
                .name("Electronics")
                .description("Electronic devices")
                .build();

        entityManager.persist(category);


        item1 = Item.builder()
                .title("Laptop")
                .description("Gaming laptop")
                .pricePerDay(BigDecimal.valueOf(20))
                .depositAmount(BigDecimal.valueOf(100))
                .finePerDay(BigDecimal.valueOf(5))
                .condition(ItemCondition.GOOD)
                .category(category)
                .owner(user)
                .build();

        entityManager.persist(item1);


        item2 = Item.builder()
                .title("Camera")
                .description("DSLR camera")
                .pricePerDay(BigDecimal.valueOf(15))
                .depositAmount(BigDecimal.valueOf(80))
                .finePerDay(BigDecimal.valueOf(4))
                .condition(ItemCondition.GOOD)
                .category(category)
                .owner(user)
                .build();

        entityManager.persist(item2);


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
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getItem()).isEqualTo(item1);
    }


    @Test
    void shouldFindFavoritesOrderedByCreatedAtDesc() throws InterruptedException {

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


        Page<Favorite> result =
                favoriteRepository.findByUser_IdOrderByCreatedAtDesc(
                        user.getId(),
                        PageRequest.of(0, 10)
                );


        assertThat(result.getContent())
                .containsExactly(second, first);
    }


    @Test
    void shouldReturnTrueWhenFavoriteExists() {

        favoriteRepository.saveAndFlush(
                Favorite.builder()
                        .user(user)
                        .item(item1)
                        .build()
        );


        boolean exists =
                favoriteRepository.existsByUser_IdAndItem_Id(
                        user.getId(),
                        item1.getId()
                );


        assertThat(exists).isTrue();
    }


    @Test
    void shouldReturnFalseWhenFavoriteDoesNotExist() {


        boolean exists =
                favoriteRepository.existsByUser_IdAndItem_Id(
                        user.getId(),
                        item1.getId()
                );


        assertThat(exists).isFalse();
    }


    @Test
    void shouldDeleteFavoriteAndReturnOne() {

        favoriteRepository.saveAndFlush(
                Favorite.builder()
                        .user(user)
                        .item(item1)
                        .build()
        );


        int deleted =
                favoriteRepository.deleteByUser_IdAndItem_Id(
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
    void shouldReturnZeroWhenDeletingMissingFavorite() {


        int deleted =
                favoriteRepository.deleteByUser_IdAndItem_Id(
                        user.getId(),
                        item1.getId()
                );


        assertThat(deleted).isZero();
    }


    @Test
    void shouldRejectDuplicateFavorite() {

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
        )
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}