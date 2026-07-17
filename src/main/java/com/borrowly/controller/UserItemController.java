package com.borrowly.controller;

import com.borrowly.dto.response.ItemSummaryResponse;
import com.borrowly.service.item.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
@Validated
public class UserItemController {

    private final ItemService itemService;


    @GetMapping("/items")
    public Page<ItemSummaryResponse> getCurrentUserItems(

            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        return itemService.getCurrentUserItems(pageable);
    }
}