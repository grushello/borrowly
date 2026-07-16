package com.borrowly.controller;

import com.borrowly.dto.response.ItemSummaryResponse;
import com.borrowly.service.item.ItemService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
@Validated
public class UserItemController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ItemService itemService;


    @GetMapping("/items")
    public Page<ItemSummaryResponse> getCurrentUserItems(

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be 0 or greater")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be greater than 0")
            int size,

            @RequestParam(defaultValue = "createdAt")
            String sort,

            @RequestParam(defaultValue = "DESC")
            Sort.Direction direction
    ) {

        if (size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Size must not exceed 50"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sort)
        );

        return itemService.getCurrentUserItems(pageable);
    }
}