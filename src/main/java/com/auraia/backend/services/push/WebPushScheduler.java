package com.auraia.backend.services.push;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebPushScheduler {

    private final WebPushService webPushService;

    @Scheduled(fixedDelayString = "${app.web-push.scheduler-fixed-delay-ms:60000}")
    public void run() {
        try {
            webPushService.runScheduledReminders(Instant.now());
        } catch (Exception ex) {
            log.warn("Web Push scheduler failed", ex);
        }
    }
}
