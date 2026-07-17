package com.borrowly.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @Pattern(
                regexp = "^(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$",
                message = "Password must contain at least one special character"
        )
        String password,

        @Size(max = 30)
        @Pattern(
                regexp = "^\\+?(?=(?:[^0-9]*[0-9]){7,})[0-9\\-\\s()]{7,20}$",
                message = "Phone must be a valid phone number"
        )
        String phone
) {
        public RegisterRequest {
                phone = (phone == null || phone.isBlank()) ? null : phone.trim();
        }
}