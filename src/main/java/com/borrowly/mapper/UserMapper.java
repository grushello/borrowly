package com.borrowly.mapper;

import com.borrowly.dto.request.UpdateUserRequest;
import com.borrowly.dto.response.UserResponse;
import com.borrowly.dto.response.UserSummaryResponse;
import com.borrowly.model.user.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);

    UserSummaryResponse toSummary(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget User target, UpdateUserRequest source);
}
