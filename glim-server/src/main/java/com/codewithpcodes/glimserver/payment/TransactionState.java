package com.codewithpcodes.glimserver.payment;

import java.util.Map;
import java.util.Set;

public enum TransactionState {
    INITIATED,
    PENDING,
    SUCCESS,

    // Provider reported a decline, or the member canceled. Terminal.
    FAILED,

    // Never resolved within the timeout window. Terminal.
    EXPIRED,

    // Polling gave up while still ambiguous. NOT terminal — a human decides.
    NEEDS_REVIEW,

    // Money returned to the member. Reached only from SUCCESS.
    REVERSED,

    // Amount corrected. Reached only from SUCCESS.
    ADJUSTED;

    private static final Map<TransactionState, Set<TransactionState>> ALLOWED =
            java.util.Map.of(
                    INITIATED,    Set.of(PENDING, FAILED),
                    PENDING,      Set.of(SUCCESS, FAILED, EXPIRED, NEEDS_REVIEW),
                    NEEDS_REVIEW, Set.of(SUCCESS, FAILED),
                    SUCCESS,      Set.of(REVERSED, ADJUSTED),
                    FAILED,       Set.of(),
                    EXPIRED,      Set.of(NEEDS_REVIEW),
                    REVERSED,     Set.of(),
                    ADJUSTED,     Set.of(REVERSED)
            );

    public boolean canTransitionTo(TransactionState next) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(next);
    }

    public boolean isTerminal() {
        return this == FAILED || this == EXPIRED || this == REVERSED;
    }

    public boolean countsAsReceived() {
        return this == SUCCESS || this == ADJUSTED;
    }
}
