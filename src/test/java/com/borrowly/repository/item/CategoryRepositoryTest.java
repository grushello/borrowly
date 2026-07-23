package com.borrowly.repository.item;

import com.borrowly.model.item.Category;
import org.junit.jupiter.api.Test;
import com.borrowly.support.AbstractPostgresTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CategoryRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private CategoryRepository categoryRepository;


    @Test
    void saveAndReloadCategory() {
        Category category = Category.builder()
                .name("Tools")
                .description("Power and hand tools")
                .build();

        Category saved = categoryRepository.save(category);

        Optional<Category> reloaded = categoryRepository.findById(saved.getId());

        assertTrue(reloaded.isPresent());
        assertEquals("Tools", reloaded.get().getName());
    }


    @Test
    void existsByNameIgnoreCase_returnsTrue_whenNameExists() {
        Category category = Category.builder()
                .name("Electronics")
                .build();

        categoryRepository.save(category);

        assertTrue(categoryRepository.existsByNameIgnoreCase("ELECTRONICS"));
    }


    @Test
    void existsByNameIgnoreCase_returnsFalse_whenNameDoesNotExist() {
        assertFalse(categoryRepository.existsByNameIgnoreCase("Missing Category"));
    }


    @Test
    void save_throwsDataIntegrityViolationException_whenNameIsDuplicated() {
        Category first = Category.builder()
                .name("Vehicles")
                .build();

        Category second = Category.builder()
                .name("Vehicles")
                .build();

        categoryRepository.saveAndFlush(first);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> categoryRepository.saveAndFlush(second)
        );
    }


}