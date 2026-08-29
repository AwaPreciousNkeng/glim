package com.codewithpcodes.glimserver.payment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "transactions",
        indexes = {
                @Index(name = "idx_txn_user_time",  columnList = "user_id, created_at"),
                @Index(name = "idx_txn_state",      columnList = "state, created_at"),
                @Index(name = "idx_txn_category",   columnList = "category_id, completed_at"),
                @Index(name = "idx_txn_provider",   columnList = "provider_charge_id")
        })
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private GivingCategory category;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "XAF";

    @Column(name = "settled_amount_minor_units")
    private Long settledAmountMinorUnits;

    @Column(name = "settled_currency", length = 3)
    private String settledCurrency;

    @Column(name = "fee_minor_units")
    private Long feeMinorUnits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionState state = TransactionState.INITIATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 20)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionSource source = TransactionSource.ONLINE;

    /**
     * Sent to the provider as tx_ref. UNIQUE, so a double-tap on Give
     * cannot produce two charges.
     */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 80)
    private String idempotencyKey;

    /** Human-readable: GLIM-TXN-2026-004312. What appears on the receipt. */
    @Column(nullable = false, unique = true, length = 40)
    private String reference;

    /** Short code printed on the receipt so it can be checked against records. */
    @Column(name = "verification_code", length = 12)
    private String verificationCode;

    @Column(name = "provider_name", length = 30)
    private String providerName;

    @Column(name = "provider_charge_id", length = 60)
    private String providerChargeId;

    @Column(name = "provider_customer_id", length = 60)
    private String providerCustomerId;

    @Column(name = "provider_payment_method_id", length = 60)
    private String providerPaymentMethodId;

    /** MTN or ORANGE, chosen by the member. */
    @Column(name = "network", length = 20)
    private String network;

    @Column(name = "provider_status", length = 60)
    private String providerStatus;

    /** The full payload. You will need this during reconciliation disputes. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_payload", columnDefinition = "jsonb")
    private String providerPayload;

    /** Masked — never the full number. */
    @Column(name = "payer_identifier", length = 40)
    private String payerIdentifier;

    @Column(name = "checkout_url", length = 1000)
    private String checkoutUrl;

    /** Set only for manual cash entry. */
    @Column(name = "recorded_by")
    private UUID recordedBy;

    @Column(length = 500)
    private String note;

    @Column(name = "initiated_at")
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_polled_at")
    private Instant lastPolledAt;

    @Column(name = "poll_attempts", nullable = false)
    @Builder.Default
    private int pollAttempts = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public boolean isResolved() {
        return state.isTerminal() || state.countsAsReceived();
    }

}
