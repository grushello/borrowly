package com.borrowly.service;

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
import com.borrowly.service.favorite.FavoriteResult;
import com.borrowly.service.favorite.FavoriteServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceImplTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private FavoriteMapper favoriteMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private FavoriteServiceImpl favoriteService;

    private User userWithId(UUID id) {
        User user = User.register("Jane", "Doe", "jane@borrowly.test", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Item itemOwnedBy(UUID itemId, User owner) {
        return Item.builder()
                .id(itemId)
                .title("Bosch Drill")
                .pricePerDay(new BigDecimal("5.00"))
                .depositAmount(new BigDecimal("50.00"))
                .finePerDay(new BigDecimal("2.00"))
                .owner(owner)
                .build();
    }

    private FavoriteResponse someResponse() {
        return new FavoriteResponse(UUID.randomUUID(), null, LocalDateTime.now());
    }

    @Test
    @DisplayName("addFavorite creates a new favorite and reports created=true")
    void addFavoriteCreatesNew() {
        UUID itemId = UUID.randomUUID();
        User me = userWithId(UUID.randomUUID());
        Item item = itemOwnedBy(itemId, userWithId(UUID.randomUUID()));

        when(currentUserProvider.getCurrentUser()).thenReturn(me);
        when(favoriteRepository.findByUser_IdAndItem_Id(me.getId(), itemId))
                .thenReturn(Optional.empty());
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(favoriteMapper.toResponse(any(Favorite.class))).thenReturn(someResponse());

        FavoriteResult result = favoriteService.addFavorite(itemId);

        assertThat(result.created()).isTrue();
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    @DisplayName("addFavorite is idempotent: already-favorited item returns created=false and does not save")
    void addFavoriteIdempotentOnDuplicate() {
        UUID itemId = UUID.randomUUID();
        User me = userWithId(UUID.randomUUID());
        Favorite existing = Favorite.builder()
                .user(me)
                .item(itemOwnedBy(itemId, userWithId(UUID.randomUUID())))
                .build();

        when(currentUserProvider.getCurrentUser()).thenReturn(me);
        when(favoriteRepository.findByUser_IdAndItem_Id(me.getId(), itemId))
                .thenReturn(Optional.of(existing));
        when(favoriteMapper.toResponse(existing)).thenReturn(someResponse());

        FavoriteResult result = favoriteService.addFavorite(itemId);

        assertThat(result.created()).isFalse();
        verify(favoriteRepository, never()).save(any());
        verify(itemRepository, never()).findById(any());
    }

    @Test
    @DisplayName("addFavorite throws 404 when the item does not exist")
    void addFavoriteThrowsWhenItemMissing() {
        UUID itemId = UUID.randomUUID();
        User me = userWithId(UUID.randomUUID());

        when(currentUserProvider.getCurrentUser()).thenReturn(me);
        when(favoriteRepository.findByUser_IdAndItem_Id(me.getId(), itemId))
                .thenReturn(Optional.empty());
        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.addFavorite(itemId))
                .isInstanceOf(ItemNotFoundException.class);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("addFavorite blocks favoriting your own item")
    void addFavoriteBlocksOwnItem() {
        UUID itemId = UUID.randomUUID();
        User me = userWithId(UUID.randomUUID());
        Item myItem = itemOwnedBy(itemId, me);

        when(currentUserProvider.getCurrentUser()).thenReturn(me);
        when(favoriteRepository.findByUser_IdAndItem_Id(me.getId(), itemId))
                .thenReturn(Optional.empty());
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(myItem));

        assertThatThrownBy(() -> favoriteService.addFavorite(itemId))
                .isInstanceOf(CannotFavoriteOwnItemException.class);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("removeFavorite deletes the favorite when present")
    void removeFavoriteDeletes() {
        UUID itemId = UUID.randomUUID();
        User me = userWithId(UUID.randomUUID());

        when(currentUserProvider.getCurrentUser()).thenReturn(me);
        when(favoriteRepository.deleteByUser_IdAndItem_Id(me.getId(), itemId)).thenReturn(1);

        favoriteService.removeFavorite(itemId);

        verify(favoriteRepository).deleteByUser_IdAndItem_Id(me.getId(), itemId);
    }

    @Test
    @DisplayName("removeFavorite is idempotent: removing a non-favorited item does not throw")
    void removeFavoriteIdempotentWhenAbsent() {
        UUID itemId = UUID.randomUUID();
        User me = userWithId(UUID.randomUUID());

        when(currentUserProvider.getCurrentUser()).thenReturn(me);
        when(favoriteRepository.deleteByUser_IdAndItem_Id(me.getId(), itemId)).thenReturn(0);

        favoriteService.removeFavorite(itemId);

        verify(favoriteRepository).deleteByUser_IdAndItem_Id(me.getId(), itemId);
    }

    @Test
    @DisplayName("Should return true when the item is already in the current user's favorites")
    void isFavoritedByCurrentUser_WhenFavorited_ReturnsTrue() {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);

        when(currentUserProvider.getCurrentUser()).thenReturn(mockUser);
        when(favoriteRepository.existsByUser_IdAndItem_Id(userId, itemId)).thenReturn(true);

        boolean result = favoriteService.isFavoritedByCurrentUser(itemId);

        assertTrue(result);
        verify(favoriteRepository).existsByUser_IdAndItem_Id(userId, itemId);
    }

    @Test
    @DisplayName("Should return false when the item is NOT in the current user's favorites")
    void isFavoritedByCurrentUser_WhenNotFavorited_ReturnsFalse() {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);

        when(currentUserProvider.getCurrentUser()).thenReturn(mockUser);
        when(favoriteRepository.existsByUser_IdAndItem_Id(userId, itemId)).thenReturn(false);

        boolean result = favoriteService.isFavoritedByCurrentUser(itemId);

        assertFalse(result);
        verify(favoriteRepository).existsByUser_IdAndItem_Id(userId, itemId);
    }

    @Test
    @DisplayName("Should gracefully return false when no user is authenticated (Exception caught)")
    void isFavoritedByCurrentUser_WhenUserNotAuthenticated_ReturnsFalse() {
        UUID itemId = UUID.randomUUID();

        when(currentUserProvider.getCurrentUser()).thenThrow(new RuntimeException("User not found"));

        boolean result = favoriteService.isFavoritedByCurrentUser(itemId);

        assertFalse(result);
        verifyNoInteractions(favoriteRepository);
    }
}