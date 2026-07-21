package com.borrowly.controller.page;

import com.borrowly.model.item.ItemCondition;
import com.borrowly.service.item.CategoryService;
import com.borrowly.service.item.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class CreateItemPageController {
    private final ItemService itemService;
    private final CategoryService categoryService;

    @GetMapping("/createItem")
    public String dashboard(Model model){

        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("conditions", ItemCondition.values());
        return "createItem";
    }
}
