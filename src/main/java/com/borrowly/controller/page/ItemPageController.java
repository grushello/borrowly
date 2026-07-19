package com.borrowly.controller.page;

import com.borrowly.dto.response.ItemResponse;
import com.borrowly.service.favorite.FavoriteService;
import com.borrowly.service.item.ItemService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/item/{id}")
    public String getItemDetail(@PathVariable("id") UUID id, Model model, Principal principal) {
        ItemResponse item = itemService.getById(id);

        boolean isFavorite = false;
        if (principal != null) {
            isFavorite = favoriteService.isFavoritedByCurrentUser(id);
        }
        
        model.addAttribute("isFavorite", isFavorite);
        model.addAttribute("item", item);
        return "item";
    }
}
