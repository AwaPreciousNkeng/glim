package com.codewithpcodes.glimserver.giving.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionStateHistoryRepository extends JpaRepository<TransactionStateHistory, UUID> {
    List<TransactionStateHistory> findByTransactionIdOrderByCreatedAtAsc(UUID transactionId);
}
