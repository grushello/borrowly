package com.borrowly.repository.item;

import com.borrowly.model.item.Category;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.item.ItemStatus;
import com.borrowly.model.user.User;
import com.borrowly.support.AbstractPostgresTest;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ItemRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private EntityManager entityManager;


    private User owner;
    private User secondOwner;

    private Category category;
    private Category secondCategory;


    @BeforeEach
    void setUp() {

        owner = User.register(
                "John",
                "Owner",
                "owner@test.com",
                "hash"
        );

        secondOwner = User.register(
                "Jane",
                "Owner",
                "owner2@test.com",
                "hash"
        );


        entityManager.persist(owner);
        entityManager.persist(secondOwner);


        category = Category.builder()
                .name("Tools")
                .description("Tools")
                .build();

        secondCategory = Category.builder()
                .name("Electronics")
                .description("Electronics")
                .build();


        entityManager.persist(category);
        entityManager.persist(secondCategory);

        entityManager.flush();
    }


    private Item createItem(
            String title,
            User owner,
            Category category,
            BigDecimal price,
            ItemStatus status
    ) {

        return Item.builder()
                .title(title)
                .description("Description")
                .pricePerDay(price)
                .depositAmount(BigDecimal.valueOf(50))
                .finePerDay(BigDecimal.valueOf(5))
                .condition(ItemCondition.GOOD)
                .status(status)
                .owner(owner)
                .category(category)
                .build();
    }


    @Test
    void shouldSaveItemWithOwnerAndCategory() {

        Item item = createItem(
                "Drill",
                owner,
                category,
                BigDecimal.valueOf(20),
                ItemStatus.ACTIVE
        );


        Item saved = itemRepository.saveAndFlush(item);


        entityManager.clear();


        Item reloaded = itemRepository.findById(saved.getId())
                .orElseThrow();


        assertThat(reloaded.getOwner().getId())
                .isEqualTo(owner.getId());

        assertThat(reloaded.getCategory().getId())
                .isEqualTo(category.getId());
    }


    @Test
    void shouldFindItemsByOwnerAndExcludeArchived() {

        Item active = createItem(
                "Active drill",
                owner,
                category,
                BigDecimal.valueOf(20),
                ItemStatus.ACTIVE
        );


        Item archived = createItem(
                "Archived drill",
                owner,
                category,
                BigDecimal.valueOf(20),
                ItemStatus.ARCHIVED
        );


        itemRepository.save(active);
        itemRepository.save(archived);
        itemRepository.flush();


        Page<Item> result =
                itemRepository.findByOwner_IdAndStatusNot(
                        owner.getId(),
                        ItemStatus.ARCHIVED,
                        PageRequest.of(0, 10)
                );


        assertThat(result.getContent())
                .containsExactly(active);
    }


    @Test
    void shouldFindItemsByStatus() {

        Item active = createItem(
                "Active item",
                owner,
                category,
                BigDecimal.valueOf(20),
                ItemStatus.ACTIVE
        );


        Item rented = createItem(
                "Rented item",
                owner,
                category,
                BigDecimal.valueOf(30),
                ItemStatus.RENTED
        );


        itemRepository.save(active);
        itemRepository.save(rented);
        itemRepository.flush();


        Page<Item> result =
                itemRepository.findByStatus(
                        ItemStatus.ACTIVE,
                        PageRequest.of(0, 10)
                );


        assertThat(result.getContent())
                .containsExactly(active);
    }


    @Test
    void shouldNotFindArchivedItemById() {

        Item archived = createItem(
                "Archived item",
                owner,
                category,
                BigDecimal.valueOf(20),
                ItemStatus.ARCHIVED
        );


        itemRepository.saveAndFlush(archived);


        assertThat(
                itemRepository.findByIdAndStatusNot(
                        archived.getId(),
                        ItemStatus.ARCHIVED
                )
        ).isEmpty();
    }


    @Test
    void shouldFindItemBySpecificationCategoryAndPriceRange() {

        Item cheap = createItem(
                "Cheap item",
                owner,
                category,
                BigDecimal.valueOf(10),
                ItemStatus.ACTIVE
        );


        Item expensive = createItem(
                "Expensive item",
                owner,
                category,
                BigDecimal.valueOf(100),
                ItemStatus.ACTIVE
        );


        itemRepository.save(cheap);
        itemRepository.save(expensive);
        itemRepository.flush();


        Specification<Item> specification =
                Specification.allOf(
                        (root, query, cb) ->
                                cb.equal(
                                        root.get("category"),
                                        category
                                ),

                        (root, query, cb) ->
                                cb.between(
                                        root.get("pricePerDay"),
                                        BigDecimal.valueOf(5),
                                        BigDecimal.valueOf(20)
                                )
                );

        Page<Item> result =
                itemRepository.findAll(
                        specification,
                        PageRequest.of(0, 10)
                );


        assertThat(result.getContent())
                .containsExactly(cheap);
    }


    @Test
    void shouldLoadOwnerAndCategoryWithoutExtraQueries() {

        for (int i = 0; i < 5; i++) {

            itemRepository.save(
                    createItem(
                            "Item " + i,
                            i % 2 == 0 ? owner : secondOwner,
                            i % 2 == 0 ? category : secondCategory,
                            BigDecimal.valueOf(20),
                            ItemStatus.ACTIVE
                    )
            );
        }

        itemRepository.flush();
        entityManager.clear();


        Page<Item> result =
                itemRepository.findByStatus(
                        ItemStatus.ACTIVE,
                        PageRequest.of(0, 10)
                );


        result.getContent()
                .forEach(item -> {
                    item.getOwner().getEmail();
                    item.getCategory().getName();
                });


        assertThat(result.getContent())
                .hasSize(5);
    }
}