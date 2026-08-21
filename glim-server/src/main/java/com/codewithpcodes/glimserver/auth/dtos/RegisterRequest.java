package com.codewithpcodes.glimserver.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters long")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters long")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Phone number is required")
        @ValidPhoneNumber(defaultRegion = "CM", message = "Please enter a valid phone number")
        String phoneNumber,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be at least 6 characters long")
        String password,

        String preferredLanguage
) {
}
