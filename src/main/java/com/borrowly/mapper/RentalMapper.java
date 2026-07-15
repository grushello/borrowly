package com.borrowly.mapper;

import com.borrowly.dto.response.RentalResponse;
import com.borrowly.model.rental.Rental;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {ItemMapper.class, UserMapper.class})
public interface RentalMapper {

    RentalResponse toResponse(Rental rental);
}