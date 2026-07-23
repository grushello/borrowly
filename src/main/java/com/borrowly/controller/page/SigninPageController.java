package com.borrowly.controller.page;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class SigninPageController {
    @GetMapping("/signin")
    public String signin(){
        return "signin";
    }
}
