package com.borrowly.mapper;

import com.borrowly.dto.response.ItemImageResponse;
import com.borrowly.model.item.ItemImage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ItemImageMapper {
    ItemImageResponse toResponse(ItemImage image);

    List<ItemImageResponse> toResponseList(List<ItemImage> images);
}