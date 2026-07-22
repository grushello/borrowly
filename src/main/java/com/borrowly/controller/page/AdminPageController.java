package com.borrowly.controller.page;

import com.borrowly.dto.response.AdminStatsResponse;
import com.borrowly.dto.response.CategoryResponse;
import com.borrowly.dto.response.ItemSummaryResponse;
import com.borrowly.dto.response.UserResponse;
import com.borrowly.service.admin.AdminStatsService;
import com.borrowly.service.item.CategoryService;
import com.borrowly.service.item.ItemService;
import com.borrowly.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPageController {

    private static final int USERS_PAGE_SIZE = 10;
    private static final int ITEMS_PAGE_SIZE = 10;

    private final CategoryService categoryService;
    private final UserService userService;
    private final ItemService itemService;
    private final AdminStatsService adminStatsService;

    @GetMapping("/admin")
    public String admin(@RequestParam(defaultValue = "0") int usersPage,
                        @RequestParam(defaultValue = "0") int itemsPage,
                        Model model) {

        Pageable usersPageable = PageRequest.of(
                Math.max(usersPage, 0),
                USERS_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Pageable itemsPageable = PageRequest.of(
                Math.max(itemsPage, 0),
                ITEMS_PAGE_SIZE,
                Sort.by(Sort.Direction.ASC, "title")
        );

        List<CategoryResponse> categories = categoryService.findAll();
        Page<UserResponse> users = userService.listUsers(usersPageable);
        Page<ItemSummaryResponse> items = itemService.adminListItems(itemsPageable);
        AdminStatsResponse stats = adminStatsService.getStats();

        model.addAttribute("categories", categories);
        model.addAttribute("usersPage", users);
        model.addAttribute("itemsPage", items);
        model.addAttribute("stats", stats);

        return "admin/admin";
    }
}