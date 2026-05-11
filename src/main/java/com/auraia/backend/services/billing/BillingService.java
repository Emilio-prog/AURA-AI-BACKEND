package com.auraia.backend.services.billing;

import com.auraia.backend.models.dto.request.BillingRequests;
import com.auraia.backend.models.dto.response.BillingResponses;

public interface BillingService {

    BillingResponses.BillingStatusResponse currentBilling();

    BillingResponses.RedirectResponse createCheckout(BillingRequests.CheckoutRequest request);

    BillingResponses.BillingStatusResponse syncCheckout(BillingRequests.CheckoutSyncRequest request);

    BillingResponses.RedirectResponse createCustomerPortal();

    void handleStripeWebhook(String payload, String stripeSignature);
}
