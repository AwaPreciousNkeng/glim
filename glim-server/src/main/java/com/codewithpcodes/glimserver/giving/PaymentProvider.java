package com.codewithpcodes.glimserver.giving;

import com.codewithpcodes.glimserver.giving.transaction.PaymentType;
import com.codewithpcodes.glimserver.giving.transaction.TransactionState;

public interface PaymentProvider {
    String name();

    ChargeResult createCharge(ChargeRequest request);

    VerificationResult verify(String providerChargeId);

    WebhookResult parseWebhook(String rawBody, String signatureHeader);

    record ChargeRequest(
            String reference,
            long amount,
            String currencyCode,
            String customerEmail,
            String firstName,
            String lastName,
            String phoneCountryCode,
            String phoneNumber,
            String network,          // MTN | ORANGE
            String idempotencyKey) {}


    enum NextActionType { AWAIT_PHONE_AUTHORISATION, REDIRECT, NONE }

    record ChargeResult(
            String providerChargeId,
            String providerCustomerId,
            String providerPaymentMethodId,
            TransactionState state,
            NextActionType nextAction,
            String instruction,      // shown to the member for the push flow
            String redirectUrl,      // set only when nextAction is REDIRECT
            String rawPayload) {}

    record VerificationResult(
            boolean found,
            TransactionState state,
            String providerStatus,
            String providerChargeId,
            Long settledAmount,
            String settledCurrency,
            Long totalFees,
            PaymentType paymentType,
            String payerIdentifier,
            String rawPayload) {}

    record WebhookResult(
            boolean valid,
            String reference,
            String providerChargeId,
            VerificationResult result) {}
}
