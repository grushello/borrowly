package com.borrowly.mapper;

import com.borrowly.dto.request.UpdateUserRequest;
import com.borrowly.dto.response.*;
import com.borrowly.model.user.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);

    UserSummaryResponse toSummary(User user);

    default UserProfileResponse toProfile(
            User user,
            List<ItemSummaryResponse> items,
            List<ReviewResponse> reviews,
            Double averageRating,
            long reviewCount
    ) {
        return new UserProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getCreatedAt(),
                items,
                reviews,
                averageRating,
                reviewCount
        );
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget User target, UpdateUserRequest source);
}
