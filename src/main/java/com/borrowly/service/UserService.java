package com.borrowly.service;

import com.borrowly.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    Page<UserResponse> listUsers(Pageable pageable);

    UserResponse disableUser(UUID id);

    UserResponse enableUser(UUID id);
}