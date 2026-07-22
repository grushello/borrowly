package com.borrowly.controller;

import com.borrowly.dto.response.UserResponse;
import com.borrowly.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public Page<UserResponse> listUsers(
            @PageableDefault(sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return userService.listUsers(pageable);
    }

    @PatchMapping("/{id}/disable")
    public UserResponse disableUser(@PathVariable UUID id) {
        return userService.disableUser(id);
    }

    @PatchMapping("/{id}/enable")
    public UserResponse enableUser(@PathVariable UUID id) {
        return userService.enableUser(id);
    }
}