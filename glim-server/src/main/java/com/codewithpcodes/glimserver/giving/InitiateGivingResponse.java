package com.codewithpcodes.glimserver.giving;

import com.codewithpcodes.glimserver.giving.transaction.Transaction;

import java.util.UUID;

public record InitiateGivingResponse(
        UUID transactionId,
        String reference,
        String state,
        String nextAction,     // AWAIT_PHONE_AUTHORISATION | REDIRECT | NONE
        String instruction,
        String redirectUrl
) {
    public static InitiateGivingResponse from(Transaction t) {
        return new InitiateGivingResponse(
                t.getId(),
                t.getReference(),
                t.getState().name(),
                "",
                "",
                t.getCheckoutUrl()
        );
    }
}
