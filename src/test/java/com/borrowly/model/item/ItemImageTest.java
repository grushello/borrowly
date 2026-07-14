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

class ItemImageTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpBeforeClass() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @AfterAll
    static void tearDownAfterClass() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void builder_HappyPath_CreatesValidItemImage() {
        Item mockItem = new Item(); // Assuming Item has a no-args constructor
        byte[] mockImageData = new byte[]{1, 2, 3, 4};

        ItemImage image = ItemImage.builder()
                .imageData(mockImageData)
                .fileName("power-drill.jpg")
                .contentType("image/jpeg")
                .item(mockItem)
                .build();

        assertArrayEquals(mockImageData, image.getImageData());
        assertEquals("power-drill.jpg", image.getFileName());
        assertEquals("image/jpeg", image.getContentType());
        assertEquals(mockItem, image.getItem());

        Set<ConstraintViolation<ItemImage>> violations = validator.validate(image);
        assertTrue(violations.isEmpty(), "A fully populated ItemImage should pass validation");
    }

    @Test
    void validation_RejectsBlankFileName() {
        ItemImage image = createValidItemImageBuilder()
                .fileName("   ") // Invalid: @NotBlank
                .build();

        Set<ConstraintViolation<ItemImage>> violations = validator.validate(image);

        assertEquals(1, violations.size(), "Should trigger exactly one violation");
        assertEquals("fileName", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validation_RejectsBlankContentType() {
        ItemImage image = createValidItemImageBuilder()
                .contentType("") // Invalid: @NotBlank
                .build();

        Set<ConstraintViolation<ItemImage>> violations = validator.validate(image);

        assertEquals(1, violations.size(), "Should trigger exactly one violation");
        assertEquals("contentType", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validation_RejectsNullItem() {
        ItemImage image = createValidItemImageBuilder()
                .item(null) // Invalid: @NotNull (Requires the annotation added to the entity)
                .build();

        Set<ConstraintViolation<ItemImage>> violations = validator.validate(image);

        assertEquals(1, violations.size(), "Should trigger exactly one violation");
        assertEquals("item", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validation_FileName_Exactly255Characters_Passes() {
        String exactBoundaryName = "a".repeat(255);
        ItemImage image = createValidItemImageBuilder()
                .fileName(exactBoundaryName)
                .build();

        Set<ConstraintViolation<ItemImage>> violations = validator.validate(image);
        assertTrue(violations.isEmpty(), "A file name of exactly 255 characters should pass validation");
    }

    @Test
    void validation_FileName_Exactly256Characters_Fails() {
        String overBoundaryName = "a".repeat(256);
        ItemImage image = createValidItemImageBuilder()
                .fileName(overBoundaryName)
                .build();

        Set<ConstraintViolation<ItemImage>> violations = validator.validate(image);

        assertEquals(1, violations.size());
        assertEquals("fileName", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void validation_ContentType_Exactly100Characters_Passes() {
        String exactBoundaryContentType = "a".repeat(100);
        ItemImage image = createValidItemImageBuilder()
                .contentType(exactBoundaryContentType)
                .build();

        Set<ConstraintViolation<ItemImage>> violations = validator.validate(image);
        assertTrue(violations.isEmpty(), "A content type of exactly 100 characters should pass validation");
    }

    @Test
    void validation_ContentType_Exactly101Characters_Fails() {
        String overBoundaryContentType = "a".repeat(101);
        ItemImage image = createValidItemImageBuilder()
                .contentType(overBoundaryContentType)
                .build();

        Set<ConstraintViolation<ItemImage>> violations = validator.validate(image);

        assertEquals(1, violations.size());
        assertEquals("contentType", violations.iterator().next().getPropertyPath().toString());
    }

    /**
     * Helper method to create a valid ItemImage baseline so we can test single failing fields.
     */
    private ItemImage.ItemImageBuilder createValidItemImageBuilder() {
        return ItemImage.builder()
                .imageData(new byte[]{1, 2, 3})
                .fileName("valid-image.png")
                .contentType("image/png")
                .item(new Item()); // Assuming a no-args Item constructor exists
    }
}