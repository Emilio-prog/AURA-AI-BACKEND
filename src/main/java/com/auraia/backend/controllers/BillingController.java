package com.auraia.backend.controllers;

import com.auraia.backend.models.dto.request.BillingRequests;
import com.auraia.backend.models.dto.response.BillingResponses;
import com.auraia.backend.services.billing.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingService billingService;

    @Operation(summary = "Get current user's billing status")
    @GetMapping("/me")
    public BillingResponses.BillingStatusResponse currentBilling() {
        return billingService.currentBilling();
    }

    @Operation(summary = "Create a Stripe Checkout session")
    @PostMapping("/checkout")
    public BillingResponses.RedirectResponse createCheckout(
            @Valid @RequestBody BillingRequests.CheckoutRequest request) {
        return billingService.createCheckout(request);
    }

    @Operation(summary = "Synchronize a completed Stripe Checkout session")
    @PostMapping("/checkout/sync")
    public BillingResponses.BillingStatusResponse syncCheckout(
            @Valid @RequestBody BillingRequests.CheckoutSyncRequest request) {
        return billingService.syncCheckout(request);
    }

    @Operation(summary = "Create a Stripe Customer Portal session")
    @PostMapping("/portal")
    public BillingResponses.RedirectResponse createCustomerPortal() {
        return billingService.createCustomerPortal();
    }
}
