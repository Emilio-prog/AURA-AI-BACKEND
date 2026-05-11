package com.auraia.backend.controllers;

import com.auraia.backend.services.billing.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhooks")
public class StripeWebhookController {

    private final BillingService billingService;

    @Operation(summary = "Stripe webhook receiver")
    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripe(
            @RequestHeader("Stripe-Signature") String stripeSignature,
            @RequestBody String payload) {
        billingService.handleStripeWebhook(payload, stripeSignature);
        return ResponseEntity.ok().build();
    }
}
