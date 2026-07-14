package com.borrowly.mapper;

import com.borrowly.dto.response.TransactionResponse;
import com.borrowly.model.transaction.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(source = "rental.id", target = "rentalId")
    TransactionResponse toResponse(Transaction transaction);
}