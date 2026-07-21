package com.borrowly.repository.item;

import com.borrowly.model.item.ItemImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemImageRepository extends JpaRepository<ItemImage, UUID> {

    List<ItemImageMetadata> findByItem_IdOrderByCreatedAtAsc(UUID itemId);

    Optional<ItemImage> findByIdAndItem_Id(UUID id, UUID itemId);

    Optional<ItemImageMetadata> findByItem_IdAndPrimaryTrue(UUID itemId);

    long countByItem_Id(UUID itemId);

    Optional<ItemImage> findFirstByItem_IdOrderByCreatedAtAsc(UUID itemId);

    @Modifying
    @Query("UPDATE ItemImage i SET i.primary = true WHERE i.id = :imageId")
    void markAsPrimary(UUID imageId);
}