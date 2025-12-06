package com.xenotask.xeno.messaging;

import com.xenotask.xeno.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SyncJobListener {
    private static final Logger log = LoggerFactory.getLogger(SyncJobListener.class);

    private final SyncService syncService;

    @RabbitListener(queues = "#{tenantQueueProvider.queueNames}")
    public void handleSyncJob(SyncJobMessage message) {
        String type = message.getType();
        if (message.getTenantId() == null) {
            // not expected in per-tenant queues, but guard
            log.warn("Received job without tenantId on per-tenant queue: {}", message);
            return;
        }
        if ("FULL".equalsIgnoreCase(type)) {
            syncService.fullSync(message.getTenantId());
        } else {
            syncService.incrementalSync(message.getTenantId(), message.getSince());
        }
        log.info("Processed sync job type={} tenant={} since={} success", type, message.getTenantId(), message.getSince());
    }
}
