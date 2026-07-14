package com.borrowly.model.user;

import com.borrowly.model.item.Item;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FavoriteTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    private User user;
    private Item item;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        if (factory != null) {
            factory.close();
        }
    }

    @BeforeEach
    void setUp() {
        user = User.register("Jane", "Doe", "jane@borrowly.test", "hash");

        item = Item.builder()
                .title("Bosch Drill")
                .pricePerDay(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.00"))
                .build();
    }

    @Test
    void builder_HappyPath_CreatesValidFavorite() {
        Favorite favorite = Favorite.builder()
                .user(user)
                .item(item)
                .build();

        assertThat(favorite.getId()).isNotNull();
        assertThat(favorite.getUser()).isSameAs(user);
        assertThat(favorite.getItem()).isSameAs(item);

        Set<ConstraintViolation<Favorite>> violations = validator.validate(favorite);
        assertThat(violations).isEmpty();
    }

    @Test
    void validation_RejectsNullUser() {
        Favorite favorite = Favorite.builder()
                .user(null)
                .item(item)
                .build();

        Set<ConstraintViolation<Favorite>> violations = validator.validate(favorite);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath()).hasToString("user");
    }

    @Test
    void validation_RejectsNullItem() {
        Favorite favorite = Favorite.builder()
                .user(user)
                .item(null)
                .build();

        Set<ConstraintViolation<Favorite>> violations = validator.validate(favorite);

        assertThat(violations).singleElement()
                .extracting(v -> v.getPropertyPath().toString())
                .isEqualTo("item");
    }
}