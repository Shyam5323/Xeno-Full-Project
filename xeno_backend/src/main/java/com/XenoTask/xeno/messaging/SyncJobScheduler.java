package com.xenotask.xeno.messaging;

import com.xenotask.xeno.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class SyncJobScheduler {
    private static final Logger log = LoggerFactory.getLogger(SyncJobScheduler.class);

    private final RabbitTemplate rabbitTemplate;
    private final TenantService tenantService;

    @Value("${sync.scheduler.enabled:true}")
    private boolean enabled;
    @Value("${sync.scheduler.mode:FULL}")
    private String mode; // INCREMENTAL or FULL
    @Value("${sync.scheduler.intervalMinutes:30}")
    private int intervalMinutes; // used to calculate since for incremental
    @Value("${sync.scheduler.targetTenant:ALL}")
    private String targetTenant; // specific tenant id or ALL
    @Value("${sync.scheduler.cron:0 0/30 * * * *}")
    private String cronExpression; // for documentation only

    @Value("${sync.messaging.queue-prefix:sync.jobs.}")
    private String queuePrefix;

    private String queueName(String tenantId) { return queuePrefix + tenantId; }

    // Runs per schedule (default every 30 minutes). Use sync.scheduler.cron to override.
    @Scheduled(cron = "${sync.scheduler.cron:0 0/30 * * * *}")
    public void enqueueSync() {
        if (!enabled) return;
        LocalDateTime now = LocalDateTime.now();
        if ("ALL".equalsIgnoreCase(targetTenant)) {
            tenantService.listActiveTenants().forEach(t -> sendForTenant(t.getTenantId(), now));
        } else {
            sendForTenant(targetTenant, now);
        }
    }

    private void sendForTenant(String tenantId, LocalDateTime now) {
        SyncJobMessage msg;
        if ("FULL".equalsIgnoreCase(mode)) {
            msg = SyncJobMessage.builder().type("FULL").tenantId(tenantId).build();
        } else {
            msg = SyncJobMessage.builder().type("INCREMENTAL").tenantId(tenantId).since(now.minusMinutes(intervalMinutes)).build();
        }
        rabbitTemplate.convertAndSend(queueName(tenantId), msg);
        log.info("Enqueued {} sync job for tenant={} since={}", msg.getType(), tenantId, msg.getSince());
    }
}
