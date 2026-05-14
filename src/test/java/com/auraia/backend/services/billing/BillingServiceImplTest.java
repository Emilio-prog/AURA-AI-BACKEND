package com.auraia.backend.services.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auraia.backend.config.AppProperties;
import com.auraia.backend.exceptions.BusinessException;
import com.auraia.backend.models.dto.request.BillingRequests;
import com.auraia.backend.models.dto.response.BillingResponses;
import com.auraia.backend.models.entities.User;
import com.auraia.backend.models.entities.UserSubscription;
import com.auraia.backend.models.enums.Plan;
import com.auraia.backend.models.enums.Role;
import com.auraia.backend.repositories.UserRepository;
import com.auraia.backend.repositories.UserSubscriptionRepository;
import com.auraia.backend.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class BillingServiceImplTest {

    private static final String PERSONAL_PRICE_ID = "price_personal";
    private static final String PREMIUM_PRICE_ID = "price_premium";
    private static final String WEBHOOK_SECRET = "whsec_test_secret";

    @Mock
    UserRepository userRepository;
    @Mock
    UserSubscriptionRepository subscriptionRepository;

    BillingServiceImpl service;
    User user;
    UUID userId;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getBilling().setPersonalPriceId(PERSONAL_PRICE_ID);
        properties.getBilling().setPremiumPriceId(PREMIUM_PRICE_ID);
        properties.getBilling().setStripeSecretKey("sk_test_local");
        properties.getWebhook().setStripeSecret(WEBHOOK_SECRET);

        service = new BillingServiceImpl(properties, userRepository, subscriptionRepository, new ObjectMapper());
        userId = UUID.randomUUID();
        user = User.builder()
            .email("emilio@example.com")
            .passwordHash("hash")
            .name("Emilio")
            .role(Role.USER)
            .plan(Plan.FREE)
            .emailVerified(true)
            .build();
        user.setId(userId);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            new UserPrincipal(userId, user.getEmail(), user.getPasswordHash(), true, List.of()),
            null,
            List.of()
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentBillingReturnsFreeWhenNoSubscriptionExists() {
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUser(user)).thenReturn(Optional.empty());

        BillingResponses.BillingStatusResponse response = service.currentBilling();

        assertThat(response.plan()).isEqualTo(Plan.FREE);
        assertThat(response.status()).isEqualTo("none");
        assertThat(response.customerPortalAvailable()).isFalse();
        assertThat(response.testMode()).isTrue();
        assertThat(response.billingConfigured()).isTrue();
    }

    @Test
    void createCheckoutRejectsFreePlan() {
        assertThatThrownBy(() -> service.createCheckout(new BillingRequests.CheckoutRequest(Plan.FREE)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("error.billing_invalid_plan");
    }

    @Test
    void stripeWebhookActivatesPersonalSubscription() throws Exception {
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByStripeSubscriptionId("sub_test")).thenReturn(Optional.empty());
        when(subscriptionRepository.findByUser(user)).thenReturn(Optional.empty());

        String payload = """
            {
              "id": "evt_test",
              "object": "event",
              "type": "customer.subscription.updated",
              "data": {
                "object": {
                  "id": "sub_test",
                  "object": "subscription",
                  "customer": "cus_test",
                  "status": "active",
                  "current_period_end": 1778576400,
                  "cancel_at_period_end": false,
                  "metadata": {
                    "userId": "%s"
                  },
                  "items": {
                    "data": [
                      {
                        "price": {
                          "id": "%s"
                        }
                      }
                    ]
                  }
                }
              }
            }
            """.formatted(userId, PERSONAL_PRICE_ID);

        service.handleStripeWebhook(payload, signature(payload));

        assertThat(user.getPlan()).isEqualTo(Plan.PERSONAL);
        verify(userRepository).save(user);

        ArgumentCaptor<UserSubscription> captor = ArgumentCaptor.forClass(UserSubscription.class);
        verify(subscriptionRepository).save(captor.capture());
        UserSubscription subscription = captor.getValue();
        assertThat(subscription.getUser()).isSameAs(user);
        assertThat(subscription.getStripeCustomerId()).isEqualTo("cus_test");
        assertThat(subscription.getStripeSubscriptionId()).isEqualTo("sub_test");
        assertThat(subscription.getStripePriceId()).isEqualTo(PERSONAL_PRICE_ID);
        assertThat(subscription.getPlan()).isEqualTo(Plan.PERSONAL);
        assertThat(subscription.getStatus()).isEqualTo("active");
        assertThat(subscription.getCurrentPeriodEnd()).isEqualTo(Instant.ofEpochSecond(1778576400));
    }

    private String signature(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(digest);
    }
}
