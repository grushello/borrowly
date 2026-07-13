package com.borrowly.model.item;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ItemImageTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void builder_HappyPath_CreatesValidItemImage() {
        UUID id = UUID.randomUUID();
        Item mockItem = new Item(); // Assuming Item has a no-args constructor
        byte[] mockImageData = new byte[]{1, 2, 3, 4};

        ItemImage image = ItemImage.builder()
                .id(id)
                .imageData(mockImageData)
                .fileName("power-drill.jpg")
                .contentType("image/jpeg")
                .primaryImage(true)
                .item(mockItem)
                .build();

        assertEquals(id, image.getId());
        assertArrayEquals(mockImageData, image.getImageData());
        assertEquals("power-drill.jpg", image.getFileName());
        assertEquals("image/jpeg", image.getContentType());
        assertTrue(image.getPrimaryImage());
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
    void builder_PrimaryImageDefaultsToFalse() {
        // Build image without specifying primaryImage status
        ItemImage image = ItemImage.builder()
                .fileName("default.png")
                .build();

        // Verify the @Builder.Default annotation populated the field immediately
        assertFalse(image.getPrimaryImage(), "primaryImage should default to false");
    }

    /**
     * Helper method to create a valid ItemImage baseline so we can test single failing fields.
     */
    private ItemImage.ItemImageBuilder createValidItemImageBuilder() {
        return ItemImage.builder()
                .imageData(new byte[]{0x00})
                .fileName("valid-image.png")
                .contentType("image/png")
                .item(new Item());
    }
}