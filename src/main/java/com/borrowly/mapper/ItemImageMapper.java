package com.borrowly.mapper;

import com.borrowly.dto.response.ItemImageResponse;
import com.borrowly.model.item.ItemImage;
import com.borrowly.repository.item.ItemImageMetadata;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ItemImageMapper {
    ItemImageResponse toResponse(ItemImage image);

    List<ItemImageResponse> toResponseList(List<ItemImage> images);

    default ItemImageResponse fromProjection(ItemImageMetadata metadata) {
        return new ItemImageResponse(
                metadata.getId(),
                metadata.getFileName(),
                metadata.getContentType(),
                metadata.getPrimary(),
                metadata.getCreatedAt()
        );
    }

    default List<ItemImageResponse> fromProjectionList(List<ItemImageMetadata> projections) {
        return projections.stream().map(this::fromProjection).toList();
    }
}