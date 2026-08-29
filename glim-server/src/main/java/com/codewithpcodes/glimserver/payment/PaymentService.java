package com.codewithpcodes.glimserver.payment;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class PaymentService {

    private final TransactionRepository transactionRepository;

    @Transactional
    public InitiateGivingResponse inititate(UUID userId, InitiateGivingRequest request) {
        var existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            Transaction t = existing.get();
            return InitiateGivingResponse
        }
    }
}
