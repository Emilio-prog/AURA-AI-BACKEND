package com.auraia.backend.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String apiBasePath = "/api/v1";
    private String frontendBaseUrl = "http://localhost:5173";
    private List<String> adminEmails = new ArrayList<>();
    private Cors cors = new Cors();
    private Jwt jwt = new Jwt();
    private Email email = new Email();
    private Ai ai = new Ai();
    private RateLimit rateLimit = new RateLimit();
    private Webhook webhook = new Webhook();
    private Billing billing = new Billing();
    private Turnstile turnstile = new Turnstile();
    private ContentEncryption contentEncryption = new ContentEncryption();
    private DevDemoUser devDemoUser = new DevDemoUser();

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173");
    }

    @Data
    public static class Jwt {
        private String secret;
        private long accessTokenExpirationMs = 900000;
        private long refreshTokenExpirationMs = 604800000;
    }

    @Data
    public static class Email {
        private boolean enabled = true;
        private boolean autoVerifyWhenDisabled = false;
        private String from = "no-reply@aura.ai";
        private long verificationTokenTtlHours = 24;
        private long passwordResetTokenTtlMinutes = 30;
    }

    @Data
    public static class Ai {
        private boolean enabled = false;
        private String geminiApiKey;
        private String geminiModel = "gemini-flash-latest";
        private int maxHistoryMessages = 12;
        private long chatRateLimitCapacity = 20;
        private long chatRateLimitRefillMinutes = 5;
    }

    @Data
    public static class RateLimit {
        private long authCapacity = 5;
        private long authRefillMinutes = 1;
    }

    @Data
    public static class Webhook {
        private String resendSecret;
        private String stripeSecret;
    }

    @Data
    public static class Billing {
        private String stripeSecretKey;
        private String personalPriceId;
        private String premiumPriceId;
        private String portalConfigurationId;
    }

    @Data
    public static class Turnstile {
        private boolean enabled = false;
        private String siteKey;
        private String secretKey;
        private int timeoutMs = 4000;
    }

    @Data
    public static class ContentEncryption {
        private String key;
        private boolean required = false;
    }

    @Data
    public static class DevDemoUser {
        private boolean enabled = false;
        private String email = "demo@aura.ai";
        private String password = "";
    }
}
