package com.borrowly.dto.response;

import com.borrowly.model.user.UserRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse (UUID id,
                            String firstName,
                            String lastName,
                            String email,
                            String phone,
                            UserRole role,
                            BigDecimal currentBalance,
                            Boolean enabled,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt
){}
