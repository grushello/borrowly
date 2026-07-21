package com.borrowly.service.favorite;

import com.borrowly.dto.response.FavoriteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FavoriteService {

    FavoriteResult addFavorite(UUID itemId);

    void removeFavorite(UUID itemId);

    Page<FavoriteResponse> listForCurrentUser(Pageable pageable);

    boolean isFavoritedByCurrentUser(UUID itemId);
}