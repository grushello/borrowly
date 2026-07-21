package com.borrowly.service.user;

import com.borrowly.dto.request.UpdateUserRequest;
import com.borrowly.dto.response.UserResponse;
import com.borrowly.dto.response.UserSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    Page<UserResponse> listUsers(Pageable pageable);

    UserResponse disableUser(UUID id);

    UserResponse enableUser(UUID id);

    UserResponse getProfile();

    UserResponse updateProfile(UpdateUserRequest request);

    UserSummaryResponse getUserSummary(UUID id);
}