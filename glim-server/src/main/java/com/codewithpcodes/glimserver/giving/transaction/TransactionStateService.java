package com.codewithpcodes.glimserver.giving.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionStateService {

    private final TransactionStateHistoryRepository transactionStateHistoryRepository;

    @Transactional
    public boolean transition(
            Transaction transaction,
            TransactionState nextState,
            String trigger,
            String reason,
            UUID actorId
    ) {
        TransactionState current = transaction.getState();

        if (current == nextState) return false;

        if (!current.canTransitionTo(nextState)) {
            log.warn("Rejected illegal transition from {} to {} on transaction {}", current, nextState, transaction.getId());
            return false;
        }

        transaction.setState(nextState);

        if (nextState.countsAsReceived() && transaction.getCompletedAt() == null) {
            transaction.setCompletedAt(Instant.now());
        }

        transactionStateHistoryRepository.save(TransactionStateHistory.builder()
                .transactionId(transaction.getId())
                .fromState(current)
                .toState(nextState)
                .trigger(trigger)
                .reason(reason)
                .actorId(actorId)
                .build()
        );

        log.info("Transaction {} {} -> {} ({})", transaction.getReference(), current, nextState, trigger);
        return true;
    }
}
