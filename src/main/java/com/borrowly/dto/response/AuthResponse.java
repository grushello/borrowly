package com.borrowly.dto.response;

import com.borrowly.model.user.UserRole;

public record AuthResponse(String token, String email, UserRole role) {}
