package com.borrowly.repository.item;

import com.borrowly.model.item.ItemImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemImageRepository extends JpaRepository<ItemImage, UUID> {


    List<ItemImageMetadata> findMetadataByItem_IdOrderByCreatedAtAsc(UUID itemId);


    Optional<ItemImage> findByIdAndItem_Id(UUID id, UUID itemId);


    Optional<ItemImageMetadata> findByItem_IdAndPrimaryTrue(UUID itemId);


    long countByItem_Id(UUID itemId);
}