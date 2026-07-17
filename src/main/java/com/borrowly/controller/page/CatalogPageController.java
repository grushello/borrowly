package com.borrowly.controller.page;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class CatalogPageController {
    @GetMapping("/")
    public String index(@PageableDefault(size = 12, sort = "title") Pageable pageable,
                        Model model){
        return "index";
    }
}
