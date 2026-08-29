package com.codewithpcodes.glimserver.auth.dtos;

import com.codewithpcodes.glimserver.user.Role;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeRoleRequest(
        @NotNull(message = "UserId Is not provided.")
        UUID userId,
        @NotNull(message = "User's new role is not provided")
        Role newRole
) {
}
