package com.codewithpcodes.glimserver.giving.transaction;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "transaction_state_history",
        indexes = @Index(name = "idx_txn_history", columnList = "transaction_id, created_at"))
public class TransactionStateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", length = 20)
    private TransactionState fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false, length = 20)
    private TransactionState toState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionTrigger trigger;

    @Column(length = 500)
    private String reason;

    /** Null for automated changes, set for manual ones. */
    @Column(name = "actor_id")
    private UUID actorId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
