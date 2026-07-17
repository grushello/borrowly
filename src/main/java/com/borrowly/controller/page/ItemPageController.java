package com.borrowly.controller.page;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ItemPageController {
    @GetMapping("/item/{id}")
    public String getItemDetail(@PathVariable("id") String id, Model model) {
        model.addAttribute("fakeItemId", id);
        return "item";
    }
}
