package com.borrowly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 100, message = "Name cannot have more than 100 characters.") String name,
        @Size(max = 500, message = "Description cannot have more than 500 characters.") String description
) {
}
