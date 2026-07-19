package com.borrowly.controller.page;

import com.borrowly.dto.response.ItemResponse;
import com.borrowly.security.CurrentUserProvider;
import com.borrowly.service.favorite.FavoriteService;
import com.borrowly.service.item.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ItemPageController {
    private final ItemService itemService;
    private final FavoriteService favoriteService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/item/{id}")
    public String getItemDetail(@PathVariable("id") UUID id, Model model, Principal principal) {
        ItemResponse item = itemService.getById(id);

        boolean isFavorite = false;
        boolean isOwner = false;
        if (principal != null) {
            isFavorite = favoriteService.isFavoritedByCurrentUser(id);
            UUID currentUserId = currentUserProvider.getCurrentUser().getId();
            assert currentUserId != null;
            isOwner = currentUserId.equals(item.owner().id());
        }

        model.addAttribute("isFavorite", isFavorite);
        model.addAttribute("item", item);
        model.addAttribute("isOwner", isOwner);
        return "item";
    }
}
