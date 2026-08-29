package com.codewithpcodes.glimserver.payment.flutterwave;

import com.codewithpcodes.glimserver.exceptions.PaymentProviderException;
import com.codewithpcodes.glimserver.payment.PaymentProvider;
import com.codewithpcodes.glimserver.payment.PaymentType;
import com.codewithpcodes.glimserver.payment.TransactionState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlutterwaveProvider implements PaymentProvider {

    private final FlutterWaveProperties props;
    private final FlutterwaveTokenManager tokenManager;
    private final ObjectMapper objectMapper;
    private RestClient client;

    private RestClient client() {
        if (client == null) {
            client = RestClient.builder().baseUrl(props.getBaseUrl()).build();
        }
        return client;
    }

    @Override
    public String name() {
        return "FLUTTERWAVE";
    }

    @Override
    public ChargeResult createCharge(ChargeRequest request) {
        try {
            String customerId = createCustomer(request);
            String paymentMethodId = createMobileMoneyMethod(request);
            return charge(request, customerId, paymentMethodId);

        } catch (PaymentProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Flutterwave charge failed for {}", request.reference(), e);
            throw new PaymentProviderException("Payment service is unavailable. Please try again.");
        }
    }

    private String createCustomer(PaymentProvider.ChargeRequest request) {
        Map<String, Object> body = Map.of(
                "email", request.customerEmail(),
                "name", Map.of(
                        "first", request.firstName(),
                        "last", request.lastName()),
                "phone", Map.of(
                        "country_code", request.phoneCountryCode(),
                        "number", request.phoneNumber()));
        JsonNode response = post("/customers", body,
                request.idempotencyKey() + "-cus", request.reference());

        return response.path("data").path("id").asText();
    }

    private String createMobileMoneyMethod(PaymentProvider.ChargeRequest request) {
        Map<String, Object> body = Map.of(
                "type", "mobile_money",
                "mobile_money", Map.of(
                        "country_code", request.phoneCountryCode(),
                        "network", request.network(),
                        "phone_number", request.phoneNumber())
        );

        JsonNode response = post("/payment-methods", body, request.idempotencyKey() + "-pmd", request.reference());
        return response.path("data").path("id").asText();
    }

    private ChargeResult charge(ChargeRequest request, String customerId, String paymentMethodId) {
        Map<String, Object> body = Map.of(
                "currency", request.currencyCode(),
                "customer_id", customerId,
                "payment_method_id", paymentMethodId,
                "amount", request.amount(),
                "reference", request.reference());

        JsonNode response = post("/charges", body,
                request.idempotencyKey(), request.reference());

        JsonNode data = response.path("data");
        JsonNode nextAction = data.path("next_action");
        String actionType = nextAction.path("type").asText("");

        NextActionType action;
        String instruction = null;
        String redirectUrl = null;

        switch (actionType) {
            case "payment_instruction" -> {
                action = NextActionType.AWAIT_PHONE_AUTHORISATION;
                instruction = nextAction.path("payment_instruction").path("note").asText(null);
            }
            case "redirect_url" -> {
                action = NextActionType.REDIRECT;
                redirectUrl = nextAction.path("redirect_url").path("url").asText(null);
            }
            default -> action = NextActionType.NONE;
        }

        return new ChargeResult(
                data.path("id").asText(),
                customerId,
                paymentMethodId,
                mapStatus(data.path("status").asText()),
                action,
                instruction,
                redirectUrl,
                data.toString());
    }

    @Override
    public VerificationResult verify(String providerChargeId) {
        try {
            JsonNode response = client().get()
                    .uri("/charges/{id}", providerChargeId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenManager.accessToken())
                    .header("X-Trace-Id", UUID.randomUUID().toString())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !"success".equals(response.path("status").asText())) {
                return notFound();
            }
            return mapCharge(response.path("data"));

        } catch (Exception e) {
            // Deliberately not a throw. A provider outage must leave the
            // transaction PENDING for the next poll, never mark it failed.
            log.warn("Flutterwave verify failed for {}: {}", providerChargeId, e.getMessage());
            return notFound();
        }
    }

    private VerificationResult mapCharge(JsonNode data) {
        String status = data.path("status").asText("");

        // Fees come back as an array of typed amounts, not one number.
        long totalFees = 0;
        for (JsonNode fee : data.path("fees")) {
            totalFees += fee.path("amount").asLong(0);
        }

        JsonNode method = data.path("payment_method_details");

        return new VerificationResult(
                true,
                mapStatus(status),
                status,
                data.path("id").asText(null),
                data.path("amount").isMissingNode() ? null : data.path("amount").asLong(),
                data.path("currency").asText("XAF"),
                totalFees,
                mapPaymentType(method.path("type").asText("")),
                maskPayer(method),
                data.toString());
    }

    private TransactionState mapStatus(String status) {
        return switch (status.toLowerCase()) {
            case "succeeded", "successful" -> TransactionState.SUCCESS;
            case "failed", "cancelled" -> TransactionState.FAILED;
            default -> TransactionState.PENDING;
        };
    }

    private PaymentType mapPaymentType(String type) {
        return switch (type.toLowerCase()) {
            case "mobile_money" -> PaymentType.MOBILE_MONEY;
            case "card" -> PaymentType.CARD;
            case "bank_transfer" -> PaymentType.BANK_TRANSFER;
            case "ussd" -> PaymentType.USSD;
            default -> null;
        };
    }

    /**
     * Never store a full number or PAN.
     */
    private String maskPayer(JsonNode method) {
        String phone = method.path("mobile_money").path("phone_number").asText(null);
        if (phone != null && phone.length() >= 4) {
            return "***" + phone.substring(phone.length() - 4);
        }
        String last4 = method.path("card").path("last4").asText(null);
        return last4 == null ? null : "****" + last4;
    }

    private VerificationResult notFound() {
        return new VerificationResult(false, TransactionState.PENDING, "UNKNOWN",
                null, null, null, null, null, null, null);
    }

    @Override
    public WebhookResult parseWebhook(String rawBody, String signature) {
        if (!signatureValid(rawBody, signature)) {
            log.warn("Rejected Flutterwave webhook with invalid signature");
            return new WebhookResult(false, null, null, null);
        }

        try {
            JsonNode payload = objectMapper.readTree(rawBody);

            if (!"charge.completed".equals(payload.path("type").asText())) {
                return new WebhookResult(true, null, null, null);   // valid but not ours to act on
            }

            JsonNode data = payload.path("data");

            return new WebhookResult(
                    true,
                    data.path("reference").asText(null),
                    data.path("id").asText(null),
                    mapCharge(data));

        } catch (Exception e) {
            log.error("Malformed Flutterwave webhook", e);
            return new WebhookResult(false, null, null, null);
        }
    }

    /**
     * The docs give two conflicting methods: HMAC-SHA256 of the raw body
     * (base64) compared against `flutterwave-signature`, and a plain equality
     * check against the secret hash. We accept either, comparing in constant
     * time. Test both against a real webhook and drop whichever isn't used.
     */
    private boolean signatureValid(String rawBody, String signature) {
        if (signature == null) return false;

        if (constantTimeEquals(signature, props.getWebhookSecretHash())) return true;

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    props.getWebhookSecretHash().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String computed = Base64.getEncoder()
                    .encodeToString(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
            return constantTimeEquals(signature, computed);
        } catch (Exception e) {
            log.error("Signature verification error", e);
            return false;
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode post(String path, Object body, String idempotencyKey, String traceId) {

        JsonNode response = client().post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenManager.accessToken())
                .header("X-Trace-Id", traceId)
                .header("X-Idempotency-Key", idempotencyKey)
                .headers(h -> {
                    if (props.getScenarioKey() != null && !props.getScenarioKey().isBlank()) {
                        h.add("X-Scenario-Key", props.getScenarioKey());
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    if (res.getStatusCode().value() == 401) tokenManager.invalidate();
                    throw new PaymentProviderException(readError(res.getBody()));
                })
                .body(JsonNode.class);

        if (response == null) {
            throw new PaymentProviderException("No response from the payment service.");
        }
        return response;
    }

    private String readError(InputStream body) {
        try {
            JsonNode error = objectMapper.readTree(body).path("error");
            String message = error.path("message").asText("");
            return message.isBlank() ? "Payment could not be started." : message;
        } catch (Exception e) {
            return "Payment could not be started.";
        }
    }
}

