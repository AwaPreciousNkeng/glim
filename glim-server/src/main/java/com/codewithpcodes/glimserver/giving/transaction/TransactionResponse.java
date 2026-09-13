package com.codewithpcodes.glimserver.giving.transaction;

import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String reference,
        String category,
        long amount,
        String currency,
        String state,
        String paymentType,
        String payerIdentifier,
        Instant createdAt,
        Instant completedAt
) {
    public static TransactionResponse from(Transaction transaction, String language) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getReference(),
                transaction.getCategory().name(language),
                transaction.getAmountMinorUnits(),
                transaction.getCurrencyCode(),
                transaction.getState().name(),
                transaction.getPaymentType() == null ? null : transaction.getPaymentType().name(),
                transaction.getPayerIdentifier(),
                transaction.getCreatedAt(),
                transaction.getCompletedAt()
        );
    }
}
