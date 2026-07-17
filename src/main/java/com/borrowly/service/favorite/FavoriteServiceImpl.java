package com.borrowly.service.favorite;

import com.borrowly.dto.response.FavoriteResponse;
import com.borrowly.exception.CannotFavoriteOwnItemException;
import com.borrowly.exception.ItemNotFoundException;
import com.borrowly.mapper.FavoriteMapper;
import com.borrowly.model.item.Item;
import com.borrowly.model.user.Favorite;
import com.borrowly.model.user.User;
import com.borrowly.repository.item.ItemRepository;
import com.borrowly.repository.user.FavoriteRepository;
import com.borrowly.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ItemRepository itemRepository;
    private final FavoriteMapper favoriteMapper;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public FavoriteResult addFavorite(UUID itemId) {
        User currentUser = currentUserProvider.getCurrentUser();

        Favorite existing = favoriteRepository
                .findByUser_IdAndItem_Id(currentUser.getId(), itemId)
                .orElse(null);
        if (existing != null) {
            return new FavoriteResult(favoriteMapper.toResponse(existing), false);
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException(itemId));

        if (item.getOwner().getId().equals(currentUser.getId())) {
            throw new CannotFavoriteOwnItemException();
        }

        Favorite favorite = Favorite.builder()
                .user(currentUser)
                .item(item)
                .build();
        favoriteRepository.save(favorite);
        log.info("User '{}' favorited item '{}'", currentUser.getId(), itemId);

        return new FavoriteResult(favoriteMapper.toResponse(favorite), true);
    }

    @Override
    @Transactional
    public void removeFavorite(UUID itemId) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        int removed = favoriteRepository.deleteByUser_IdAndItem_Id(userId, itemId);
        if (removed > 0) {
            log.info("User '{}' unfavorited item '{}'", userId, itemId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FavoriteResponse> listForCurrentUser(Pageable pageable) {
        UUID userId = currentUserProvider.getCurrentUser().getId();
        return favoriteRepository
                .findByUser_IdOrderByCreatedAtDesc(userId, pageable)
                .map(favoriteMapper::toResponse);
    }
}