package com.borrowly.controller.page;

import com.borrowly.dto.response.UserProfileResponse;
import com.borrowly.dto.response.UserResponse;
import com.borrowly.model.user.User;
import com.borrowly.security.CurrentUserProvider;
import com.borrowly.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class UserPageController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;


    @GetMapping("/user/{id}")
    public String getUserProfile(
            @PathVariable UUID id,
            Model model
    ) {

        UserProfileResponse profile =
                userService.getUserProfile(id);


        boolean isOwner =
                currentUserProvider
                        .getCurrentUserOptional()
                        .map(User::getId)
                        .map(id::equals)
                        .orElse(false);


        model.addAttribute("profile", profile);
        model.addAttribute("isOwner", isOwner);


        return "user/profile";
    }


    @GetMapping("/account")
    public String getAccountPage(Model model) {

        UserResponse account =
                userService.getAccountInfo();


        model.addAttribute("account", account);


        return "user/account";
    }

    @GetMapping("/profile")
    public String getMyProfile() {

        UUID id = currentUserProvider
                .getCurrentUser()
                .getId();

        return "redirect:/user/" + id;
    }
}