package com.codewithpcodes.glimserver.giving;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record InitiateGivingRequest(
        @NotNull
        UUID categoryId,

        @Positive(message = "The amount has to be a positive number.")
        long amount,

        @NotBlank(message = "The idempotency key is required.")
        @Size(max = 80)
        String idempotencyKey,

        @NotBlank(message = "The payer's phone number is required.")
        String payerPhone,

        @NotBlank(message = "The mobile network is required")
        @Pattern(regexp = "MTN|ORANGE")
        String network,

        boolean savePayerPhone
) {
}
