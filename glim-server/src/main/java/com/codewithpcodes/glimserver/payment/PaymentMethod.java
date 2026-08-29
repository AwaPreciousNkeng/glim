package com.codewithpcodes.glimserver.payment;

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
@Table(name = "payment_methods",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "msisdn"}))
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * The mobile money number. Deliberately independent of the account
     * identifier — people pay from a number that isn't their own.
     */
    @Column(nullable = false, length = 20)
    private String msisdn;

    @Column(length = 60)
    private String label;

    /** Verified by a successful payment, not by us sending anything. */
    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
