package com.borrowly.controller.page;

import com.borrowly.dto.response.CategoryResponse;
import com.borrowly.dto.response.ItemSummaryResponse;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.service.CategoryService;
import com.borrowly.service.item.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class CatalogPageController {
    private final ItemService itemService;
    private final CategoryService categoryService;

    @GetMapping("/")
    public String index(@RequestParam(required = false) UUID categoryId,
                        @RequestParam(required = false) ItemCondition condition,
                        @RequestParam(required = false) BigDecimal minPrice,
                        @RequestParam(required = false) BigDecimal maxPrice,
                        @RequestParam(required = false) String search,
                        @PageableDefault(size = 12, sort = "title") Pageable pageable,
                        Model model){

        Page<ItemSummaryResponse> itemPage = itemService.browseItems(categoryId, condition, minPrice, maxPrice, search, pageable);
        List<CategoryResponse> categories = categoryService.findAll();

        model.addAttribute("itemPage", itemPage);
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentMinPrice", minPrice);
        model.addAttribute("currentMaxPrice", maxPrice);
        model.addAttribute("itemCondition", condition);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categories", categories);

        return "index";
    }
}
