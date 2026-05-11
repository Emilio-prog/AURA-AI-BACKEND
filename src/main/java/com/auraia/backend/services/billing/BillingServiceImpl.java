package com.auraia.backend.services.billing;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.exceptions.BusinessException;
import com.auraia.backend.exceptions.UnauthorizedException;
import com.auraia.backend.models.dto.request.BillingRequests;
import com.auraia.backend.models.dto.response.BillingResponses;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserSubscription;
import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.repositories.UserSubscriptionRepository;
import com.auraia.backend.security.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private static final String STATUS_NONE = "none";
    private static final String CHECKOUT_SUCCESS = "/#/dashboard/billing?checkout=success&session_id={CHECKOUT_SESSION_ID}";
    private static final String CHECKOUT_CANCEL = "/#/dashboard/billing?checkout=cancel";
    private static final String PORTAL_RETURN = "/#/dashboard/billing";

    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public BillingResponses.BillingStatusResponse currentBilling() {
        User user = currentUser();
        return subscriptionRepository.findByUser(user)
            .map(this::toResponse)
            .orElseGet(() -> new BillingResponses.BillingStatusResponse(
                user.getPlan(),
                user.getPlan() == Plan.FREE ? STATUS_NONE : "active",
                null,
                false,
                false,
                isStripeTestMode(),
                isStripeConfigured()
            ));
    }

    @Override
    @Transactional
    public BillingResponses.RedirectResponse createCheckout(BillingRequests.CheckoutRequest request) {
        Plan requestedPlan = paidPlan(request.plan());
        configureStripe();

        User user = currentUser();
        UserSubscription localSubscription = findOrCreate(user);
        String customerId = ensureCustomer(user, localSubscription);

        if (hasActiveSubscription(localSubscription)) {
            return createPortalForCustomer(customerId);
        }

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .setSuccessUrl(frontendUrl(CHECKOUT_SUCCESS))
                .setCancelUrl(frontendUrl(CHECKOUT_CANCEL))
                .setAllowPromotionCodes(true)
                .putMetadata("userId", user.getId().toString())
                .putMetadata("plan", requestedPlan.name())
                .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("userId", user.getId().toString())
                    .putMetadata("plan", requestedPlan.name())
                    .build())
                .addLineItem(SessionCreateParams.LineItem.builder()
                    .setPrice(priceIdFor(requestedPlan))
                    .setQuantity(1L)
                    .build())
                .build();

            Session session = Session.create(params);
            return new BillingResponses.RedirectResponse(session.getUrl());
        } catch (StripeException ex) {
            throw new BusinessException("error.billing_unavailable", ex.getMessage());
        }
    }

    @Override
    @Transactional
    public BillingResponses.BillingStatusResponse syncCheckout(BillingRequests.CheckoutSyncRequest request) {
        configureStripe();
        User user = currentUser();

        try {
            Session session = Session.retrieve(request.sessionId());
            JsonNode sessionNode = objectMapper.readTree(session.toJson());
            String sessionUserId = sessionNode.path("metadata").path("userId").asText(null);
            if (!user.getId().toString().equals(sessionUserId)) {
                throw new BusinessException("error.billing_invalid_session");
            }

            String stripeSubscriptionId = sessionNode.path("subscription").asText(null);
            if (!isBlank(stripeSubscriptionId)) {
                syncStripeSubscription(stripeSubscriptionId);
                return currentBilling();
            }

            handleCheckoutCompleted(sessionNode);
            return currentBilling();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("error.billing_unavailable", ex.getMessage());
        }
    }

    @Override
    @Transactional
    public BillingResponses.RedirectResponse createCustomerPortal() {
        User user = currentUser();
        UserSubscription localSubscription = subscriptionRepository.findByUser(user)
            .orElseThrow(() -> new BusinessException("error.billing_customer_required"));
        if (isBlank(localSubscription.getStripeCustomerId())) {
            throw new BusinessException("error.billing_customer_required");
        }
        configureStripe();
        return createPortalForCustomer(localSubscription.getStripeCustomerId());
    }

    @Override
    @Transactional
    public void handleStripeWebhook(String payload, String stripeSignature) {
        String webhookSecret = appProperties.getWebhook().getStripeSecret();
        if (isBlank(webhookSecret)) {
            throw new BusinessException("error.billing_unavailable");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, stripeSignature, webhookSecret);
        } catch (SignatureVerificationException ex) {
            throw new BusinessException("error.stripe_signature_invalid", ex.getMessage());
        }

        try {
            JsonNode object = objectMapper.readTree(payload).path("data").path("object");
            switch (event.getType()) {
                case "checkout.session.completed" -> handleCheckoutCompleted(object);
                case "customer.subscription.created",
                     "customer.subscription.updated",
                     "customer.subscription.deleted" -> syncSubscriptionObject(object);
                case "invoice.paid",
                     "invoice.payment_failed" -> refreshSubscriptionFromInvoice(object);
                default -> {
                    // Other Stripe events are intentionally ignored in B8.
                }
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("error.billing_unavailable", ex.getMessage());
        }
    }

    private BillingResponses.RedirectResponse createPortalForCustomer(String customerId) {
        try {
                com.stripe.param.billingportal.SessionCreateParams.Builder paramsBuilder =
                com.stripe.param.billingportal.SessionCreateParams.builder()
                    .setCustomer(customerId)
                    .setReturnUrl(frontendUrl(PORTAL_RETURN));

            String portalConfigurationId = appProperties.getBilling().getPortalConfigurationId();
            if (!isBlank(portalConfigurationId)) {
                paramsBuilder.setConfiguration(portalConfigurationId);
            }

            com.stripe.param.billingportal.SessionCreateParams params = paramsBuilder.build();
            com.stripe.model.billingportal.Session session =
                com.stripe.model.billingportal.Session.create(params);
            return new BillingResponses.RedirectResponse(session.getUrl());
        } catch (StripeException ex) {
            throw new BusinessException("error.billing_unavailable", ex.getMessage());
        }
    }

    private void handleCheckoutCompleted(JsonNode session) {
        String userId = session.path("metadata").path("userId").asText(null);
        if (isBlank(userId)) {
            return;
        }

        userRepository.findByIdAndDeletedAtIsNull(java.util.UUID.fromString(userId))
            .ifPresent(user -> {
                UserSubscription local = findOrCreate(user);
                setIfPresent(session.path("customer").asText(null), local::setStripeCustomerId);
                setIfPresent(session.path("subscription").asText(null), local::setStripeSubscriptionId);
                Plan plan = planFromName(session.path("metadata").path("plan").asText(null));
                if (plan != Plan.FREE) {
                    local.setPlan(plan);
                    local.setStripePriceId(priceIdFor(plan));
                }
                local.setStatus("checkout_completed");
                subscriptionRepository.save(local);
            });
    }

    private void refreshSubscriptionFromInvoice(JsonNode invoice) {
        String subscriptionId = invoice.path("subscription").asText(null);
        if (isBlank(subscriptionId)) {
            subscriptionId = invoice.path("parent").path("subscription_details").path("subscription").asText(null);
        }
        if (isBlank(subscriptionId)) {
            return;
        }
        syncStripeSubscription(subscriptionId);
    }

    private void syncStripeSubscription(String stripeSubscriptionId) {
        configureStripe();
        try {
            com.stripe.model.Subscription stripeSubscription =
                com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
            String payload = stripeSubscription.toJson();
            syncSubscriptionObject(objectMapper.readTree(payload));
        } catch (Exception ex) {
            throw new BusinessException("error.billing_unavailable", ex.getMessage());
        }
    }

    private void syncSubscriptionObject(JsonNode subscription) {
        String stripeSubscriptionId = subscription.path("id").asText(null);
        String stripeCustomerId = subscription.path("customer").asText(null);
        String userId = subscription.path("metadata").path("userId").asText(null);

        User user = findUserForWebhook(userId, stripeCustomerId);
        if (user == null) {
            return;
        }

        UserSubscription local = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
            .or(() -> subscriptionRepository.findByUser(user))
            .orElseGet(() -> UserSubscription.builder()
                .user(user)
                .plan(Plan.FREE)
                .status(STATUS_NONE)
                .build());

        String priceId = firstPriceId(subscription);
        Plan paidPlan = planFromPriceId(priceId);
        String status = subscription.path("status").asText(STATUS_NONE);

        local.setUser(user);
        setIfPresent(stripeCustomerId, local::setStripeCustomerId);
        setIfPresent(stripeSubscriptionId, local::setStripeSubscriptionId);
        setIfPresent(priceId, local::setStripePriceId);
        local.setPlan(paidPlan);
        local.setStatus(status);
        local.setCurrentPeriodEnd(epochSeconds(subscription.path("current_period_end").asLong(0)));
        local.setCancelAtPeriodEnd(subscription.path("cancel_at_period_end").asBoolean(false));
        local.setCanceledAt(epochSeconds(subscription.path("canceled_at").asLong(0)));

        user.setPlan(isPaidStatus(status) ? paidPlan : Plan.FREE);
        userRepository.save(user);
        subscriptionRepository.save(local);
    }

    private User findUserForWebhook(String userId, String stripeCustomerId) {
        if (!isBlank(userId)) {
            return userRepository.findByIdAndDeletedAtIsNull(java.util.UUID.fromString(userId)).orElse(null);
        }
        if (!isBlank(stripeCustomerId)) {
            return subscriptionRepository.findByStripeCustomerId(stripeCustomerId)
                .map(UserSubscription::getUser)
                .orElse(null);
        }
        return null;
    }

    private UserSubscription findOrCreate(User user) {
        return subscriptionRepository.findByUser(user)
            .orElseGet(() -> subscriptionRepository.save(UserSubscription.builder()
                .user(user)
                .plan(Plan.FREE)
                .status(STATUS_NONE)
                .build()));
    }

    private String ensureCustomer(User user, UserSubscription localSubscription) {
        if (!isBlank(localSubscription.getStripeCustomerId())) {
            return localSubscription.getStripeCustomerId();
        }

        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getName())
                .putMetadata("userId", user.getId().toString())
                .build();
            Customer customer = Customer.create(params);
            localSubscription.setStripeCustomerId(customer.getId());
            subscriptionRepository.save(localSubscription);
            return customer.getId();
        } catch (StripeException ex) {
            throw new BusinessException("error.billing_unavailable", ex.getMessage());
        }
    }

    private BillingResponses.BillingStatusResponse toResponse(UserSubscription subscription) {
        return new BillingResponses.BillingStatusResponse(
            isPaidStatus(subscription.getStatus()) ? subscription.getPlan() : Plan.FREE,
            subscription.getStatus(),
            subscription.getCurrentPeriodEnd(),
            subscription.isCancelAtPeriodEnd(),
            !isBlank(subscription.getStripeCustomerId()) && isStripeConfigured(),
            isStripeTestMode(),
            isStripeConfigured()
        );
    }

    private Plan paidPlan(Plan plan) {
        if (plan == Plan.PERSONAL || plan == Plan.PREMIUM) {
            return plan;
        }
        throw new BusinessException("error.billing_invalid_plan");
    }

    private String priceIdFor(Plan plan) {
        return switch (plan) {
            case PERSONAL -> requiredPriceId(appProperties.getBilling().getPersonalPriceId());
            case PREMIUM -> requiredPriceId(appProperties.getBilling().getPremiumPriceId());
            case FREE -> throw new BusinessException("error.billing_invalid_plan");
        };
    }

    private String requiredPriceId(String priceId) {
        if (isBlank(priceId)) {
            throw new BusinessException("error.billing_unavailable");
        }
        return priceId;
    }

    private Plan planFromPriceId(String priceId) {
        if (!isBlank(priceId) && priceId.equals(appProperties.getBilling().getPersonalPriceId())) {
            return Plan.PERSONAL;
        }
        if (!isBlank(priceId) && priceId.equals(appProperties.getBilling().getPremiumPriceId())) {
            return Plan.PREMIUM;
        }
        return Plan.FREE;
    }

    private Plan planFromName(String value) {
        if (isBlank(value)) {
            return Plan.FREE;
        }
        try {
            return paidPlan(Plan.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (RuntimeException ex) {
            return Plan.FREE;
        }
    }

    private String firstPriceId(JsonNode subscription) {
        JsonNode items = subscription.path("items").path("data");
        if (items.isArray() && !items.isEmpty()) {
            return items.get(0).path("price").path("id").asText(null);
        }
        return null;
    }

    private Instant epochSeconds(long seconds) {
        return seconds <= 0 ? null : Instant.ofEpochSecond(seconds);
    }

    private boolean hasActiveSubscription(UserSubscription subscription) {
        return !isBlank(subscription.getStripeSubscriptionId()) && isPaidStatus(subscription.getStatus());
    }

    private boolean isPaidStatus(String status) {
        return "active".equals(status) || "trialing".equals(status);
    }

    private String frontendUrl(String path) {
        String baseUrl = appProperties.getFrontendBaseUrl();
        if (isBlank(baseUrl)) {
            return path;
        }
        return baseUrl.replaceAll("/+$", "") + path;
    }

    private boolean isStripeTestMode() {
        String key = appProperties.getBilling().getStripeSecretKey();
        return !isBlank(key) && (key.startsWith("sk_test_") || key.startsWith("rk_test_"));
    }

    private boolean isStripeConfigured() {
        return !isBlank(appProperties.getBilling().getStripeSecretKey())
            && !isBlank(appProperties.getBilling().getPersonalPriceId())
            && !isBlank(appProperties.getBilling().getPremiumPriceId());
    }

    private void configureStripe() {
        String key = appProperties.getBilling().getStripeSecretKey();
        if (isBlank(key)) {
            throw new BusinessException("error.billing_unavailable");
        }
        Stripe.apiKey = key;
    }

    private User currentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(SecurityUtils.currentUserId())
            .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }

    private void setIfPresent(String value, java.util.function.Consumer<String> consumer) {
        if (!isBlank(value)) {
            consumer.accept(value);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
