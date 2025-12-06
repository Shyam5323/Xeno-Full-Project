package com.xenotask.xeno.controller;

import com.xenotask.xeno.messaging.SyncJobMessage;
import com.xenotask.xeno.messaging.TenantQueueProvider;
import com.xenotask.xeno.service.TenantService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sync/jobs")
@RequiredArgsConstructor
@Validated
public class SyncJobController {

    static final String SYNC_TYPE = "INCREMENTAL"; // FULL / INCREMENTAL

    private static final Logger log = LoggerFactory.getLogger(SyncJobController.class);

    private final RabbitTemplate rabbitTemplate;
    private final TenantService tenantService;
    private final TenantQueueProvider tenantQueueProvider;

    private String queueName(String tenantId) { return "sync.jobs." + tenantId; }

    @PostMapping
    public ResponseEntity<Map<String,Object>> enqueue(@RequestBody SyncJobRequest request) {
        String type = request.getType() == null ? SYNC_TYPE : request.getType().toUpperCase();
        if (!type.equals("FULL") && !type.equals(SYNC_TYPE)) {
            return ResponseEntity.badRequest().body(Map.of("error","type must be FULL or INCREMENTAL"));
        }
        LocalDateTime since = request.getSince();
        if (type.equals(SYNC_TYPE) && since == null) {
            since = LocalDateTime.now().minusMinutes(30); // default window
        }
        int enqueued = 0;
        if (request.getTenantId() == null || request.getTenantId().equalsIgnoreCase("ALL")) {
            var tenants = tenantService.listActiveTenants();
            for (var t : tenants) {
                enqueueOne(t.getTenantId(), type, since);
                enqueued++;
            }
        } else {
            tenantService.getRequiredByTenantId(request.getTenantId());
            enqueueOne(request.getTenantId(), type, since);
            enqueued = 1;
        }
        Map<String,Object> resp = new HashMap<>();
        resp.put("status","enqueued");
        resp.put("jobs", enqueued);
        resp.put("type", type);
        resp.put("since", since);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/single/{tenantId}")
    public ResponseEntity<Map<String,Object>> enqueueSingle(@PathVariable String tenantId,
                                                             @RequestParam(defaultValue = SYNC_TYPE) String type,
                                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        type = type.toUpperCase();
        if (!type.equals("FULL") && !type.equals(SYNC_TYPE)) {
            return ResponseEntity.badRequest().body(Map.of("error","type must be FULL or INCREMENTAL"));
        }
        if (type.equals(SYNC_TYPE) && since == null) since = LocalDateTime.now().minusMinutes(30);
        tenantService.getRequiredByTenantId(tenantId);
        enqueueOne(tenantId, type, since);
        return ResponseEntity.ok(Map.of("status","enqueued","tenantId", tenantId,"type", type,"since", since));
    }

    private void enqueueOne(String tenantId, String type, LocalDateTime since) {
        tenantQueueProvider.ensureQueue(tenantId);
        SyncJobMessage msg = SyncJobMessage.builder()
                .tenantId(tenantId)
                .type(type)
                .since(type.equals(SYNC_TYPE) ? since : null)
                .build();
        rabbitTemplate.convertAndSend(queueName(tenantId), msg);
        log.info("Manually enqueued job type={} tenant={} since={}", type, tenantId, msg.getSince());
    }

    @Data
    public static class SyncJobRequest {
        @NotBlank(message = "type is required if provided")
        private String type; // FULL / INCREMENTAL
        private String tenantId; // null or 'ALL' => all tenants
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime since; // optional for incremental
    }
}

