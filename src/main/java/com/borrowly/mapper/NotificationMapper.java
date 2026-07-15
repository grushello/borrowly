package com.borrowly.mapper;

import com.borrowly.dto.response.NotificationResponse;
import com.borrowly.model.notification.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "rental.id", target = "rentalId")
    @Mapping(source = "transaction.id", target = "transactionId")
    NotificationResponse toResponse(Notification notification);
}
