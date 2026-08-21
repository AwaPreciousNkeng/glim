package com.codewithpcodes.glimserver.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeEmailRequest(
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be at least 6 characters long")
        String password,

        @NotBlank(message = "New Email is required")
        @Email(message = "Invalid email format")
        String newEmail
) {
}
