package com.borrowly.service.auth;

import com.borrowly.dto.request.LoginRequest;
import com.borrowly.dto.request.RegisterRequest;
import com.borrowly.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}