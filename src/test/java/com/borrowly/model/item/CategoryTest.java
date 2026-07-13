package com.borrowly.model.item;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void builder_HappyPath_CreatesValidCategory() {
        UUID id = UUID.randomUUID();

        Category category = Category.builder()
                .id(id)
                .name("Camping Gear")
                .description("Tents, sleeping bags, and outdoor equipment")
                .build();

        // Verify Builder populated fields correctly
        assertEquals(id, category.getId());
        assertEquals("Camping Gear", category.getName());
        assertEquals("Tents, sleeping bags, and outdoor equipment", category.getDescription());

        // Verify it passes all validation constraints
        Set<ConstraintViolation<Category>> violations = validator.validate(category);
        assertTrue(violations.isEmpty(), "A fully populated category should not trigger any violations");
    }

    @Test
    void validation_RejectsBlankName() {
        Category category = Category.builder()
                .name("   ") // Invalid: @NotBlank fails on whitespace
                .description("Valid description")
                .build();

        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        assertEquals(1, violations.size(), "Should trigger exactly one violation");

        ConstraintViolation<Category> violation = violations.iterator().next();
        assertEquals("name", violation.getPropertyPath().toString(), "Violation must be on the name property");
    }
}
