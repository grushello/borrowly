package com.borrowly.controller;

import com.borrowly.dto.response.ItemSummaryResponse;
import com.borrowly.service.item.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class UserItemController {

    private final ItemService itemService;

    @GetMapping("/items")
    public Page<ItemSummaryResponse> getCurrentUserItems(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return itemService.getCurrentUserItems(pageable);
    }
}