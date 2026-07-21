package com.borrowly.repository.item;

import com.borrowly.model.item.ItemImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemImageRepository extends JpaRepository<ItemImage, UUID> {

    List<ItemImageMetadata> findByItemIdOrderByCreatedAtAsc(UUID itemId);

    Optional<ItemImage> findByIdAndItemId(UUID id, UUID itemId);

    Optional<ItemImageMetadata> findByItemIdAndPrimaryTrue(UUID itemId);

    long countByItemId(UUID itemId);

    Optional<ItemImage> findFirstByItemIdOrderByCreatedAtAsc(UUID itemId);

    @Modifying
    @Query("UPDATE ItemImage i SET i.primary = true WHERE i.id = :imageId")
    void markAsPrimary(UUID imageId);
}