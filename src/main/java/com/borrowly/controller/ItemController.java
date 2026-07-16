package com.borrowly.controller;

import com.borrowly.dto.request.CreateItemRequest;
import com.borrowly.dto.request.UpdateItemRequest;
import com.borrowly.dto.response.ItemResponse;
import com.borrowly.dto.response.ItemSummaryResponse;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.service.item.ItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/items")
@Validated
public class ItemController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ItemService itemService;


    @PostMapping
    public ItemResponse create(
            @Valid @RequestBody CreateItemRequest request
    ) {
        return itemService.create(request);
    }


    @GetMapping
    public Page<ItemSummaryResponse> browseItems(

            @RequestParam(required = false)
            UUID categoryId,

            @RequestParam(required = false)
            ItemCondition condition,

            @RequestParam(required = false)
            BigDecimal minPrice,

            @RequestParam(required = false)
            BigDecimal maxPrice,

            @RequestParam(required = false)
            String search,

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

        Pageable pageable = PageRequest.of(
                page,
                Math.min(size, MAX_PAGE_SIZE),
                Sort.by(direction, sort)
        );

        return itemService.browseItems(
                categoryId,
                condition,
                minPrice,
                maxPrice,
                search,
                pageable
        );
    }


    @GetMapping("/{id}")
    public ItemResponse getById(
            @PathVariable UUID id
    ) {
        return itemService.getById(id);
    }


    @PatchMapping("/{id}")
    public ItemResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateItemRequest request
    ) {
        return itemService.update(id, request);
    }


    @DeleteMapping("/{id}")
    public ItemResponse archive(
            @PathVariable UUID id
    ) {
        return itemService.archive(id);
    }
}