package com.borrowly.repository.item;

import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID>, JpaSpecificationExecutor<Item> {

    @EntityGraph(attributePaths = {"owner", "category"})
    Page<Item> findByOwner_IdAndStatusNot(
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
}