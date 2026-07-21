package com.borrowly.controller;

import com.borrowly.dto.request.UpdateUserRequest;
import com.borrowly.dto.response.UserProfileResponse;
import com.borrowly.dto.response.UserResponse;
import com.borrowly.dto.response.UserSummaryResponse;
import com.borrowly.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Private: logged-in user
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getAccountInfo() {
        return ResponseEntity.ok(userService.getAccountInfo());
    }

    // Private: logged-in user
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateAccountInfo(@Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateAccountInfo(request));
    }

    // Public: profile
    @GetMapping("/{id}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    // Public: summary
    @GetMapping("/{id}")
    public ResponseEntity<UserSummaryResponse> getUserSummary(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                userService.getUserSummary(id)
        );
    }
}