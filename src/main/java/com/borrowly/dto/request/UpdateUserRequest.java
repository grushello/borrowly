package com.borrowly.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest (
        @Size(max = 100, message = "Name cannot have more than 100 characters.")
        String firstName,

        @Size(max = 100, message = "Last name cannot have more than 100 characters.")
        String lastName,

        @Size(max = 30, message = "Phone number cannot have more than 30 characters.")
        @Pattern(regexp = "^\\+?[0-9\\-\\s()]{7,20}$", message = "Invalid phone format.")
        String phone
){
}
