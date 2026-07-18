package com.borrowly.controller.page;

import com.borrowly.dto.response.ItemResponse;
import com.borrowly.service.item.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ItemPageController {
    private final ItemService itemService;

    @GetMapping("/item/{id}")
    public String getItemDetail(@PathVariable("id") UUID id, Model model) {
        ItemResponse item = itemService.getById(id);

        model.addAttribute("item", item);
        return "item";
    }
}
