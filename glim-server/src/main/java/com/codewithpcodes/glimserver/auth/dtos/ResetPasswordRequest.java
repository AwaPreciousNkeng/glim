package com.codewithpcodes.glimserver.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Code is required")
        @Pattern(regexp = "\\d{6}")
        String code,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72, message = "Password must be at least 6 characters long")
        String newPassword,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be at least 6 characters long")
        String confirmNewPassword
        ) {
}
