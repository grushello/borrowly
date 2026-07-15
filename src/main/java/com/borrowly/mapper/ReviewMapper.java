package com.borrowly.mapper;

import com.borrowly.dto.response.ReviewResponse;
import com.borrowly.model.user.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ItemMapper.class, UserMapper.class})
public interface ReviewMapper {

    @Mapping(source = "rental.item", target = "item")
    @Mapping(source = "rental.id", target = "rentalId")
    ReviewResponse toResponse(Review review);
}