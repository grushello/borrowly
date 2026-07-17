package com.borrowly.repository.item;

import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.model.item.ItemStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public final class ItemSpecification {

    private ItemSpecification() {
    }

    public static Specification<Item> browse(
            UUID categoryId,
            ItemCondition condition,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String search
    ) {
        Specification<Item> specification = hasStatus(ItemStatus.ACTIVE);

        if (categoryId != null) {
            specification = specification.and(hasCategory(categoryId));
        }

        if (condition != null) {
            specification = specification.and(hasCondition(condition));
        }

        if (minPrice != null) {
            specification = specification.and(hasMinPrice(minPrice));
        }

        if (maxPrice != null) {
            specification = specification.and(hasMaxPrice(maxPrice));
        }

        if (search != null && !search.isBlank()) {
            specification = specification.and(titleContains(search));
        }

        return specification;
    }

    private static Specification<Item> hasStatus(ItemStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    private static Specification<Item> hasCategory(UUID categoryId) {

        return (root, query, cb) ->
                cb.equal(root.get("category").get("id"), categoryId);
    }

    private static Specification<Item> hasCondition(ItemCondition condition) {

        if (condition == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(root.get("condition"), condition);
    }

    private static Specification<Item> hasMinPrice(BigDecimal minPrice) {

        if (minPrice == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("pricePerDay"), minPrice);
    }

    private static Specification<Item> hasMaxPrice(BigDecimal maxPrice) {

        if (maxPrice == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("pricePerDay"), maxPrice);
    }

    private static Specification<Item> titleContains(String search) {

        if (search == null || search.isBlank()) {
            return null;
        }

        return (root, query, cb) ->
                cb.like(
                        cb.lower(root.get("title")),
                        "%" + search.toLowerCase() + "%"
                );
    }
}