package com.codewithpcodes.glimserver.giving;

import com.codewithpcodes.glimserver.audit.AuditService;
import com.codewithpcodes.glimserver.giving.transaction.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final TransactionRepository transactionRepository;
    private final TransactionStateHistoryRepository transactionStateHistoryRepository;
    private final PaymentProvider paymentProvider;
    private final GivingService givingService;
    private final TransactionStateService transactionStateService;
    private final AuditService auditService;
    private final GivingNotifier notifier;

    public Page<Transaction> queue(Pageable pageable) {
        return transactionRepository.findByStateOrderByCreatedAtAsc(TransactionState.NEEDS_REVIEW, pageable);
    }

    // Force a fresh check right now
    @Transactional
    public Transaction reverify(UUID transactionId, UUID actorID) {
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow();

        var result = paymentProvider.verify(transaction.getReference());
        givingService.applyVerification(transaction, result, TransactionTrigger.MANUAL, actorID);

        auditService.record(actorID, "TRANSACTION_REVERIFY", "Transaction",
                transaction.getId(), null, result.providerStatus());

        return transaction;
    }

    @Transactional
    public Transaction resolveManually(UUID transactionID, TransactionState outcome,
                                       String reason, UUID actorID) {
        if (outcome != TransactionState.SUCCESS && outcome != TransactionState.FAILED) {
            throw new IllegalArgumentException("Manual resolution must be SUCCESS or FAILED.");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required.");
        }

        Transaction transaction = transactionRepository.findById(transactionID).orElseThrow();
        var before = transaction.getState();

        boolean changed = transactionStateService.transition(
                transaction,
                outcome,
                TransactionTrigger.MANUAL,
                reason,
                actorID
        );

        if (!changed) {
            throw new IllegalStateException(
                    "Cannot move %s from %s to %s.".formatted(
                            transaction.getReference(), before, outcome
                    )
            );
        }
        auditService.record(actorID, "TRANSACTION_MANUAL_RESOLVE", "Transaction",
                transactionID, before.name(), outcome.name() + "-" + reason);

        notifier.notifyStateChange(transaction);
        return transaction;
    }

    public List<TransactionStateHistory> historyOf(UUID transactionID) {
        return transactionStateHistoryRepository.findByTransactionIdOrderByCreatedAtAsc(transactionID);
    }
}
