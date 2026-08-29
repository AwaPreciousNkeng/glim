package com.codewithpcodes.glimserver.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String key);
    Optional<Transaction> findByReference(String reference);
    Optional<Transaction> findByProviderTransactionId(String providerTransactionId);

    Page<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.userId = :userId
          AND t.state IN ('SUCCESS','ADJUSTED')
          AND (:categoryId IS NULL OR t.category.id = :categoryId)
          AND (:from IS NULL OR t.completedAt >= :from)
          AND (:to   IS NULL OR t.completedAt <= :to)
        ORDER BY t.completedAt DESC
        """)
    Page<Transaction> history(@Param("userId") UUID userId,
                              @Param("categoryId") UUID categoryId,
                              @Param("from") Instant from,
                              @Param("to") Instant to,
                              Pageable pageable);

    /**
     * The polling job's queue. FOR UPDATE SKIP LOCKED so two instances
     * never poll the same transaction and double-apply a state change.
     */
    @Query(value = """
        SELECT * FROM transactions
        WHERE state = 'PENDING' AND initiated_at < :cutoff
        ORDER BY initiated_at
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<Transaction> lockStalePending(@Param("cutoff") Instant cutoff,
                                       @Param("limit") int limit);

    /** The reconciliation screen. */
    Page<Transaction> findByStateOrderByCreatedAtAsc(TransactionState state, Pageable pageable);

    long countByState(TransactionState state);

    // ---------- reporting ----------

    @Query("""
        SELECT COALESCE(SUM(t.amountMinorUnits), 0) FROM Transaction t
        WHERE t.userId = :userId AND t.state IN ('SUCCESS','ADJUSTED')
          AND t.completedAt >= :from AND t.completedAt <= :to
        """)
    long totalForUser(@Param("userId") UUID userId,
                      @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
        SELECT c.code                          AS categoryCode,
               COUNT(*)                        AS count,
               COALESCE(SUM(t.amount_minor_units), 0) AS total
        FROM transactions t
        JOIN giving_categories c ON c.id = t.category_id
        WHERE t.state IN ('SUCCESS','ADJUSTED')
          AND t.completed_at BETWEEN :from AND :to
        GROUP BY c.code
        ORDER BY total DESC
        """, nativeQuery = true)
    List<CategoryTotalProjection> totalsByCategory(@Param("from") Instant from,
                                                   @Param("to") Instant to);

    @Query(value = """
        SELECT DATE_TRUNC('day', completed_at) AS day,
               COALESCE(SUM(amount_minor_units), 0) AS total
        FROM transactions
        WHERE state IN ('SUCCESS','ADJUSTED') AND completed_at BETWEEN :from AND :to
        GROUP BY DATE_TRUNC('day', completed_at)
        ORDER BY day
        """, nativeQuery = true)
    List<DailyTotalProjection> dailyTotals(@Param("from") Instant from,
                                           @Param("to") Instant to);

    /** Sequence for the human-readable reference. */
    @Query(value = "SELECT COUNT(*) + 1 FROM transactions WHERE EXTRACT(YEAR FROM created_at) = :year",
            nativeQuery = true)
    long nextSequenceForYear(@Param("year") int year);
}
