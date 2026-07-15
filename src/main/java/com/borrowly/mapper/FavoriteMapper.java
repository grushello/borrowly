package com.borrowly.mapper;

import com.borrowly.dto.response.FavoriteResponse;
import com.borrowly.model.user.Favorite;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {ItemMapper.class})
public interface FavoriteMapper {

    FavoriteResponse toResponse(Favorite favorite);
}