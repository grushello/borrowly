package com.borrowly.service.favorite;

import com.borrowly.dto.response.FavoriteResponse;


public record FavoriteResult(FavoriteResponse favorite, boolean created) {
}

