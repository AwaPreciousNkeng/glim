package com.codewithpcodes.glimserver.giving;

import com.codewithpcodes.glimserver.giving.transaction.Transaction;
import com.codewithpcodes.glimserver.giving.transaction.TransactionRepository;
import com.codewithpcodes.glimserver.giving.transaction.TransactionState;
import com.codewithpcodes.glimserver.giving.transaction.TransactionStateHistory;
import com.codewithpcodes.glimserver.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/giving")
@RequiredArgsConstructor
public class AdminGivingController {

    private final ReconciliationService reconciliationService;
    private final TransactionRepository transactionRepository;

    @GetMapping("/reconciliation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Transaction>> queue(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reconciliationService.queue(pageable));
    }

    @PostMapping("/reconciliation/{id}/reverify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Transaction> reverify(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(reconciliationService.reverify(id, user.getId()));
    }

    @PostMapping("/reconciliation/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Transaction> resolve(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody @Valid ResolveRequest resolveRequest
    ) {
        return ResponseEntity.ok(reconciliationService.resolveManually(
                id, TransactionState.valueOf(resolveRequest.outcome()), resolveRequest.reason(), user.getId()
        ));
    }

    @GetMapping("/transactions/{id}/history")
    @PreAuthorize("hasAnyRole('PASTOR', 'ADMIN')")
    public ResponseEntity<List<TransactionStateHistory>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(reconciliationService.historyOf(id));
    }

    // ---------- reports ----------

    // Aggregates only — safe for the finance officer
    @GetMapping("/reports/by-category")
    @PreAuthorize("hasAnyRole('FINANCE','PASTOR','ADMIN')")
    public List<CategoryTotalProjection> byCategory(@RequestParam Instant from,
                                                    @RequestParam Instant to) {
        return transactionRepository.totalsByCategory(from, to);
    }

    @GetMapping("/reports/daily")
    @PreAuthorize("hasAnyRole('FINANCE','PASTOR','ADMIN')")
    public List<DailyTotalProjection> daily(@RequestParam Instant from,
                                            @RequestParam Instant to) {
        return transactionRepository.dailyTotals(from, to);
    }

    /**
     * Named, per-member detail. PASTOR only — your decision, and the reason
     * this endpoint is separated from the aggregate reports above.
     */
    @GetMapping("/members/{userId}/transactions")
    @PreAuthorize("hasAnyRole('PASTOR','ADMIN')")
    public Page<Transaction> memberGiving(
            @PathVariable UUID userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
