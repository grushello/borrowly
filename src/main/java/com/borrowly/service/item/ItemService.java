package com.borrowly.service.item;

import com.borrowly.dto.request.CreateItemRequest;
import com.borrowly.dto.request.UpdateItemRequest;
import com.borrowly.dto.response.ItemResponse;
import com.borrowly.dto.response.ItemSummaryResponse;
import com.borrowly.model.item.ItemCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface ItemService {
    ItemResponse create(CreateItemRequest request);

    ItemResponse getById(UUID id);

    Page<ItemSummaryResponse> browseItems(
            UUID categoryId,
            ItemCondition condition,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String search,
            Pageable pageable
    );

    Page<ItemSummaryResponse> getCurrentUserItems(Pageable pageable);

    ItemResponse update(UUID id, UpdateItemRequest request);

    ItemResponse archive(UUID id);
    ItemResponse unarchive(UUID id);

    Page<ItemSummaryResponse> adminListItems(Pageable pageable);
}