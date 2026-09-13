package com.codewithpcodes.glimserver.giving;

import jakarta.validation.constraints.NotBlank;

public record ResolveRequest(
        @NotBlank(message = "The outcome is required.")
        String outcome,
        @NotBlank(message = "The reason is required.")
        String reason
) {
}
