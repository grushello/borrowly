package com.borrowly.repository.item;

import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID>, JpaSpecificationExecutor<Item> {

    @EntityGraph(attributePaths = {"owner", "category"})
    Page<Item> findByOwnerIdAndStatusNot(
            UUID ownerId,
            ItemStatus excluded,
            Pageable pageable
    );


    @EntityGraph(attributePaths = {"owner", "category"})
    Page<Item> findByStatus(
            ItemStatus status,
            Pageable pageable
    );


    @EntityGraph(attributePaths = {"owner", "category"})
    Optional<Item> findByIdAndStatusNot(
            UUID id,
            ItemStatus excluded
    );

    @EntityGraph(attributePaths = {"owner", "category"})
    Page<Item> findByOwnerId(UUID ownerId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"owner", "images"})
    Page<Item> findAll(Specification<Item> spec, Pageable pageable);

    boolean existsByCategoryId(UUID categoryId);

    long countByStatus(ItemStatus status);

    @EntityGraph(attributePaths = {"owner", "category"})
    @Query("select i from Item i")
    Page<Item> findAllForAdmin(Pageable pageable);
}