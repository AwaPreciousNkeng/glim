package com.codewithpcodes.glimserver.giving;

import com.codewithpcodes.glimserver.giving.category.GivingCategoryRepository;
import com.codewithpcodes.glimserver.giving.category.GivingCategoryResponse;
import com.codewithpcodes.glimserver.giving.transaction.Transaction;
import com.codewithpcodes.glimserver.giving.transaction.TransactionRepository;
import com.codewithpcodes.glimserver.giving.transaction.TransactionResponse;
import com.codewithpcodes.glimserver.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/giving")
@RequiredArgsConstructor
public class GivingController {

    private final GivingService givingService;
    private final GivingCategoryRepository givingCategoryRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @GetMapping("/categories")
    public ResponseEntity<List<GivingCategoryResponse>> categories(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                givingCategoryRepository.findByActiveTrueOrderByDisplayOrderAsc()
                        .stream()
                        .map(c -> GivingCategoryResponse.from(c, user.getLanguage().name()))
                        .toList()
        );
    }

    @PostMapping
    public ResponseEntity<InitiateGivingResponse> give(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid InitiateGivingRequest request
    ) {
        return ResponseEntity.ok(givingService.initiate(user.getId(), request));
    }

    // Polled by the app after the member returns from checkout.
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> status(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id
    ) {
        Transaction t = transactionRepository.findById(id)
                .filter(x -> x.getUserId().equals(user.getId()))   // ownership check
                .orElseThrow();
        return ResponseEntity.ok(Map.of("state", t.getState().name(), "reference", t.getReference()));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<TransactionResponse>> history(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(transactionRepository.history(user.getId(), categoryId, from, to, pageable)
                .map(t -> TransactionResponse.from(t, user.getLanguage().name())));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "2026") int year
    ) {
        Instant from = Instant.parse(year + "-01-01T00:00:00Z");
        Instant to = Instant.parse(year + "-12-31T23:59:59Z");
        return ResponseEntity.ok(
                Map.of(
                "year", year,
                "total", transactionRepository.totalForUser(user.getId(), from, to),
                "currency", "XAF")
        );
    }

    @GetMapping("/payment-methods")
    public List<?> paymentMethods(@AuthenticationPrincipal User user) {
        return paymentMethodRepository
                .findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId());
    }
}
