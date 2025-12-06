package com.xenotask.xeno.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final String alertRecipient;
    private final boolean enabled; // feature flag

    public NotificationService(JavaMailSender mailSender,
                               @Value("divyanshukumar690@gmail.com") String alertRecipient,
                               @Value("${notification.enabled:false}") boolean enabled) {
        this.mailSender = mailSender;
        this.alertRecipient = alertRecipient;
        this.enabled = enabled;
    }

    @Async
    public void sendSyncFailure(String tenantId, String syncType, String message) {
        if (!enabled) {
            log.debug("Notification disabled; skip email tenant={} type={} msg={}", tenantId, syncType, message);
            return;
        }
        if (alertRecipient == null || alertRecipient.isBlank()) {
            log.warn("No alert recipient configured; skipping email for sync failure tenant={} type={} message={}", tenantId, syncType, message);
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(alertRecipient);
            mail.setSubject("Sync Failure - tenant=" + tenantId + " type=" + syncType);
            mail.setText("Tenant: " + tenantId + "\nType: " + syncType + "\nError: " + message);
            mailSender.send(mail);
            log.info("Sent sync failure notification tenant={} type={}", tenantId, syncType);
        } catch (Exception e) {
            log.error("Failed sending sync failure notification tenant={} type={} msg={}", tenantId, syncType, e.getMessage());
        }
    }
}
