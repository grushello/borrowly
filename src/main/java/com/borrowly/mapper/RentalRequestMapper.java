package com.borrowly.mapper;

import com.borrowly.dto.response.RentalRequestResponse;
import com.borrowly.model.rental.RentalRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {ItemMapper.class, UserMapper.class})
public interface RentalRequestMapper {

    RentalRequestResponse toResponse(RentalRequest rentalRequest);
}