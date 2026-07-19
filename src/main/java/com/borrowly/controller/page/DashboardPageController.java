package com.borrowly.controller.page;

import com.borrowly.dto.response.FavoriteResponse;
import com.borrowly.service.favorite.FavoriteService;
import com.borrowly.service.user.UserService;
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

    @GetMapping("/dashboard")
    public String dashboard(Model model){
        Pageable widgetPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<FavoriteResponse> favoritesPage = favoriteService.listForCurrentUser(widgetPageable);

        model.addAttribute("favorites", favoritesPage.getContent());
        return "dashboard";
    }
}
