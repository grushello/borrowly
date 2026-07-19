package com.borrowly.controller.page;

import com.borrowly.dto.response.ItemResponse;
import com.borrowly.model.item.ItemCondition;
import com.borrowly.service.CategoryService;
import com.borrowly.service.item.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class UpdateItemPageController {
    private final ItemService itemService;
    private final CategoryService categoryService;

    @GetMapping("/update/{id}")
    public String dashboard(@PathVariable("id") UUID id, Model model){
        ItemResponse item = itemService.getById(id);

        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("conditions", ItemCondition.values());
        model.addAttribute("item", item);
        return "updateItem";
    }
}
