package com.codewithpcodes.glimserver.giving;


import com.codewithpcodes.glimserver.exceptions.InvalidGivingException;
import com.codewithpcodes.glimserver.exceptions.PartnershipRequiredException;
import com.codewithpcodes.glimserver.giving.category.GivingCategory;
import com.codewithpcodes.glimserver.giving.category.GivingCategoryRepository;
import com.codewithpcodes.glimserver.giving.partnership.PartnershipGate;
import com.codewithpcodes.glimserver.giving.transaction.*;
import com.codewithpcodes.glimserver.user.User;
import com.codewithpcodes.glimserver.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.Year;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class GivingService {
    private final GivingCategoryRepository givingCategoryRepository;
    private final PartnershipGate partnershipGate;
    private final TransactionStateService transactionStateService;
    private final PaymentProvider paymentProvider;
    private final GivingProperties props;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();
    private final PaymentMethodRepository paymentMethodRepository;

    @Transactional
    public InitiateGivingResponse initiate(UUID userId, InitiateGivingRequest request) {
        var existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            Transaction t = existing.get();
            return InitiateGivingResponse.from(t);
        }

        User user = userRepository.findById(userId).orElseThrow();
        GivingCategory category = givingCategoryRepository.findById(request.categoryId())
                .filter(GivingCategory::isActive)
                .orElseThrow(() -> new InvalidGivingException("That giving category is not available"));

        long minimum = category.getMinimumAmount() == null
                ? props.getMinimumAmount()
                : category.getMinimumAmount();

        if (request.amount() < minimum) {
            throw new InvalidGivingException("The minimum amount for %s is XAF %,d."
                    .formatted(category.name("ENGLISH"), minimum));
        }

        if (category.isRequiresPartnership() && !partnershipGate.hasPartnership(userId)) {
            throw new PartnershipRequiredException("Please register as a partner before making a partnership payment.");
        }

        int year = Year.now().getValue();

        String reference = "GLIM-TXN-%d-%06d".formatted(year, transactionRepository.nextSequenceForYear(year));

        Transaction transaction = Transaction.builder()
                .userId(userId)
                .category(category)
                .amountMinorUnits(request.amount())
                .currencyCode("XAF")
                .idempotencyKey(request.idempotencyKey())
                .reference(reference)
                .verificationCode(randomCode())
                .providerName(paymentProvider.name())
                .source(TransactionSource.ONLINE)
                .state(TransactionState.INITIATED)
                .initiatedAt(Instant.now())
                .build();
        transactionRepository.save(transaction);

        var session = paymentProvider.createCharge(new PaymentProvider.ChargeRequest(
                reference,
                request.amount(),
                "XAF",
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                request.payerPhone()
        ))
    }

    @Transactional
    public void applyVerification(
            Transaction transaction,
            PaymentProvider.VerificationResult result,
            String trigger,
            UUID actorId
    ) {
        if (!result.found()) return;
        transaction.setProviderStatus(result.providerStatus());
        transaction.setLastPolledAt(Instant.now());

        if (result.providerChargeId() != null) {
            transaction.setProviderChargeId(result.providerChargeId());
        }
        if (result.rawPayload() != null) {
            transaction.setProviderPayload(result.rawPayload());
        }
        if (result.paymentType() != null) {
            transaction.setPaymentType(result.paymentType());
        }

        if (result.payerIdentifier() != null) {
            transaction.setPayerIdentifier(result.payerIdentifier());
        }

        if (result.state() == TransactionState.SUCCESS) {
            transaction.setSettledAmountMinorUnits(result.settledAmount());
            transaction.setSettledCurrency(result.settledCurrency());
            transaction.setFeeMinorUnits(result.totalFees());

            if (result.settledAmount() != null && result.settledAmount() < transaction.getAmountMinorUnits()) {
                log.warn("Amount mismatch on {}: expected {}, settled {}",
                        transaction.getReference(),
                        transaction.getAmountMinorUnits(),
                        result.settledAmount());
            }
        }
         transactionStateService.transition(transaction, result.state(), trigger,
                 "Provider reported " + result.providerStatus(), actorId);
    }


    private void savePaymentMethod(UUID userID, String msisdn) {
        paymentMethodRepository.findByUserIdAndMsisdn(userID, msisdn)
                .ifPresentOrElse(
                        existing ->
                                existing.setVerifiedAt(Instant.now()),
                        () -> paymentMethodRepository.save(PaymentMethod.builder()
                                        .userId(userID).msisdn(msisdn).label("My number")
                                .build())
                );
    }
    private String randomCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
