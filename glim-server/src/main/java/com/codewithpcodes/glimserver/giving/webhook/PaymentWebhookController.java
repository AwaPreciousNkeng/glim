package com.codewithpcodes.glimserver.giving.webhook;

import com.codewithpcodes.glimserver.giving.GivingNotifier;
import com.codewithpcodes.glimserver.giving.GivingService;
import com.codewithpcodes.glimserver.giving.PaymentProvider;
import com.codewithpcodes.glimserver.giving.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks/flutterwave")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentProvider paymentProvider;
    private final TransactionRepository transactionRepository;
    private final GivingService givingService;
    private final GivingNotifier givingNotifier;

    @PostMapping
    @Transactional
    public ResponseEntity<Void> receive(
            @RequestBody String rawBody,
            @RequestHeader(value = "verif-hash", required = false) String signature
    ) {
        var webhook = paymentProvider.parseWebhook(rawBody, signature);
        if (!webhook.valid()) return ResponseEntity.status(403).build();

        var transaction = transactionRepository.findByReference(webhook.reference()).orElse(null);
        if (transaction == null) {
            log.warn("Webhook for unknown reference {}",  webhook.reference());
            return ResponseEntity.ok().build();
        }

        var before = transaction.getState();

        var verified = paymentProvider.verify(webhook.reference());
        givingService.applyVerification(transaction, verified, "WEBHOOK", null);

        if (before != transaction.getState()) {
            givingNotifier.notifyStateChange(transaction);
        }

        return ResponseEntity.ok().build();
    }
}
