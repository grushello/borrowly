package com.borrowly.controller;

import com.borrowly.dto.response.ItemResponse;
import com.borrowly.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class CatalogPageController {

//    private final ItemService itemService;

    @GetMapping("/")
    public String index(@PageableDefault(size = 12, sort = "title") Pageable pageable,
                        Model model){
//        Page<ItemResponse> itemPage = itemService.findAll(pageable);
//        model.addAttribute("itemPage", itemPage);
        return "index";
    }

    @GetMapping("/signup")
    public String signup(){
        return "signup";
    }

    @GetMapping("/signin")
    public String signin(){
        return "signin";
    }

    @GetMapping("/dashboard")
    public String dashboard(){
        return "dashboard";
    }

    @GetMapping("/item/{id}")
    public String getItemDetail(@PathVariable("id") String id, Model model) {

        // ItemResponse item = itemService.findById(id);
        model.addAttribute("fakeItemId", id);
        return "item";
    }
}
