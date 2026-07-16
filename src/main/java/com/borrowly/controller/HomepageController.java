package com.borrowly.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomepageController {

    @GetMapping("/")
    public String index(){
        return "index";
    }

    @GetMapping("/signup")
    public String signup(){
        return "signup";
    }

    @GetMapping("/dashboard")
    public String dashboard(){
        return "dashboard";
    }
}
