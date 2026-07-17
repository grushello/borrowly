package com.borrowly.controller;

import com.borrowly.dto.response.FavoriteResponse;
import com.borrowly.service.favorite.FavoriteResult;
import com.borrowly.service.favorite.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{itemId}")
    public ResponseEntity<FavoriteResponse> addFavorite(@PathVariable UUID itemId) {
        FavoriteResult result = favoriteService.addFavorite(itemId);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.favorite());
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable UUID itemId) {
        favoriteService.removeFavorite(itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<FavoriteResponse>> listFavorites(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(favoriteService.listForCurrentUser(pageable));
    }
}