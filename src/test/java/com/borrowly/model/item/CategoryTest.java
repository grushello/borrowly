package com.borrowly.model.item;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpBeforeClass() {
        // Initialize once for the entire test class lifecycle
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDownAfterClass() {
        // Clean up resources after all tests have run
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void builder_GeneratesIdAutomatically() {
        Category category = Category.builder()
                .name("Tools")
                .build();

        // Verify the ID is internally generated at the moment of instantiation
        assertNotNull(category.getId(), "Category ID should be automatically generated upon creation");
    }

    @Test
    void builder_HappyPath_CreatesValidCategory() {
        Category category = Category.builder()
                .name("Camping Gear")
                .description("Tents, sleeping bags, and outdoor equipment")
                .build();

        // Verify Builder populated fields correctly
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

    @Test
    void validation_RejectsNullName() {
        Category category = Category.builder()
                .name(null)
                .description("Valid description")
                .build();

        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        assertEquals(1, violations.size(), "Should trigger exactly one violation");

        ConstraintViolation<Category> violation = violations.iterator().next();
        assertEquals("name", violation.getPropertyPath().toString(), "Violation must be on the name property");
    }

    @Test
    void validation_RejectsOverlyLongName() {
        String overlyLongName = "a".repeat(101); // Exceeds @Size(max = 100)

        Category category = Category.builder()
                .name(overlyLongName)
                .build();

        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        assertEquals(1, violations.size(), "Should trigger exactly one violation");
        assertEquals("name", violations.iterator().next().getPropertyPath().toString(), "Violation must be on the name property");
    }

    @Test
    void validation_RejectsOverlyLongDescription() {
        String overlyLongDescription = "a".repeat(501); // Exceeds @Size(max = 500)

        Category category = Category.builder()
                .name("Valid Name")
                .description(overlyLongDescription)
                .build();

        Set<ConstraintViolation<Category>> violations = validator.validate(category);

        assertEquals(1, violations.size(), "Should trigger exactly one violation");
        assertEquals("description", violations.iterator().next().getPropertyPath().toString(), "Violation must be on the description property");
    }
}
