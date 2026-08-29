package com.codewithpcodes.glimserver.payment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "giving_categories")
public class GivingCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "name_en", nullable = false, length = 80)
    private String nameEn;

    @Column(name = "name_fr", nullable = false, length = 80)
    private String nameFr;

    @Column(name = "description_en", length = 300)
    private String descriptionEn;

    @Column(name = "description_fr", length = 300)
    private String descriptionFr;


    /** True only for PARTNERSHIP — blocks giving unless a partnership record exists. */
    @Column(name = "requires_partnership", nullable = false)
    @Builder.Default
    private boolean requiresPartnership = false;

    /**
     * True only for PARTNERSHIP. Stops an ordinary tithe from silently
     * renewing someone's partner status.
     */
    @Column(name = "counts_toward_partnership", nullable = false)
    @Builder.Default
    private boolean countsTowardPartnership = false;

    /** Per-category floor. Falls back to the global minimum when null. */
    @Column(name = "minimum_amount")
    private Long minimumAmount;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(name = "icon_name", length = 40)
    private String iconName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public String name(String language) {
        return "FRENCH".equalsIgnoreCase(language) ? nameFr : nameEn;
    }
}