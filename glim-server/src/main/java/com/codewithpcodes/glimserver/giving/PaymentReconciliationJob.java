package com.codewithpcodes.glimserver.giving;

import com.codewithpcodes.glimserver.giving.transaction.Transaction;
import com.codewithpcodes.glimserver.giving.transaction.TransactionRepository;
import com.codewithpcodes.glimserver.giving.transaction.TransactionState;
import com.codewithpcodes.glimserver.giving.transaction.TransactionStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {

    private final TransactionRepository transactionRepository;
    private final PaymentProvider paymentProvider;
    private final GivingService givingService;
    private final TransactionStateService transactionStateService;
    private final GivingNotifier givingNotifier;
    private final GivingProperties givingProperties;

    // Catches the orphaned-payment case: money moved, webhook never arrived.
    @Scheduled(fixedDelay = 120_000)
    @Transactional
    public void pollPending() {
        Instant cutoff = Instant.now().minus(givingProperties.getPendingTimeoutMinutes(), ChronoUnit.MINUTES);
        var stale = transactionRepository.lockStalePending(cutoff, 50);
        if (stale.isEmpty()) return;

        log.info("Polling {} pending transactions", stale.size());

        for (Transaction transaction : stale) {
            transaction.setPollAttempts(transaction.getPollAttempts() + 1);

            var before = transaction.getState();
            var result = paymentProvider.verify(transaction.getReference());
            givingService.applyVerification(transaction, result, "POLLING", null);

            if (before != transaction.getState()) {
                givingNotifier.notifyStateChange(transaction);
                continue;
            }

            escalateIfStuck(transaction);
        }
    }

    // After enough failed polls, hand it to a human
    private void escalateIfStuck(Transaction transaction) {
        boolean tooOld = transaction.getInitiatedAt()
                .isBefore(Instant.now().minus(givingProperties.getReviewTimeoutMinutes(), ChronoUnit.MINUTES));
        boolean tooManyAttempts = transaction.getPollAttempts() >= givingProperties.getMaxPollAttempts();
        if (tooOld || tooManyAttempts) {
            transactionStateService.transition(
                    transaction,
                    TransactionState.NEEDS_REVIEW,
                    "SYSTEM",
                    "Unresolved after %d polls".formatted(transaction.getPollAttempts()),
                    null
            );
            givingNotifier.alertReconciler(transaction);
        }
    }
}
