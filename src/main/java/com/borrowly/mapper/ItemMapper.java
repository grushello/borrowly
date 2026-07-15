package com.borrowly.mapper;

import com.borrowly.dto.request.CreateItemRequest;
import com.borrowly.dto.request.UpdateItemRequest;
import com.borrowly.dto.response.ItemResponse;
import com.borrowly.dto.response.ItemSummaryResponse;
import com.borrowly.model.item.Item;
import com.borrowly.model.item.ItemImage;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel = "spring",
        uses = {UserMapper.class, CategoryMapper.class, ItemImageMapper.class})
public interface ItemMapper {

    @Mapping(source = "status", target = "itemStatus")
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount", ignore = true)
    ItemResponse toResponse(Item item);

    /**
     * Service-level overload: the mapper cannot compute averageRating and reviewCount
     * from the entity alone — these aggregates come from the review repository.
     * The service calls this method, passing the pre-computed values alongside the entity.
     */
    default ItemResponse toResponse(Item item, Double averageRating, long reviewCount) {
        ItemResponse base = toResponse(item);
        return new ItemResponse(
                base.id(),
                base.title(),
                base.description(),
                base.pricePerDay(),
                base.depositAmount(),
                base.finePerDay(),
                base.condition(),
                base.itemStatus(),
                base.owner(),
                base.category(),
                base.images(),
                averageRating,
                reviewCount,
                base.createdAt(),
                base.updatedAt()
        );
    }

    @Mapping(source = "status", target = "itemStatus")
    @Mapping(target = "ownerName", expression = "java(item.getOwner().getFirstName() + \" \" + item.getOwner().getLastName())")
    @Mapping(target = "primaryImageId", expression = "java(extractPrimaryImageId(item))")
    ItemSummaryResponse toSummary(Item item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "images", ignore = true)
    Item toEntity(CreateItemRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "images", ignore = true)
    void updateEntity(@MappingTarget Item target, UpdateItemRequest source);

    default UUID extractPrimaryImageId(Item item) {
        return item.getPrimaryImage().map(ItemImage::getId).orElse(null);
    }
}