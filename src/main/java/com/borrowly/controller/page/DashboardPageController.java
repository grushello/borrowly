package com.borrowly.controller.page;

import com.borrowly.dto.response.FavoriteResponse;
import com.borrowly.dto.response.ItemSummaryResponse;
import com.borrowly.dto.response.UserResponse;
import com.borrowly.mapper.UserMapper;
import com.borrowly.model.user.User;
import com.borrowly.security.CurrentUserProvider;
import com.borrowly.service.favorite.FavoriteService;
import com.borrowly.service.item.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardPageController {
    private final FavoriteService favoriteService;
    private final ItemService itemService;
    private final CurrentUserProvider currentUserProvider;
    private final UserMapper userMapper;

    @GetMapping("/dashboard")
    public String dashboard(Model model){
        Pageable widgetPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<FavoriteResponse> favoritesPage = favoriteService.listForCurrentUser(widgetPageable);
        Page<ItemSummaryResponse> itemsPage = itemService.getCurrentUserItems(widgetPageable);
        User user = currentUserProvider.getCurrentUser();
        UserResponse userResponse = userMapper.toResponse(user);

        model.addAttribute("favorites", favoritesPage.getContent());
        model.addAttribute("userItemListings", itemsPage.getContent());
        model.addAttribute("user", userResponse);
        return "dashboard";
    }
}
