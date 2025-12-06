package com.xenotask.xeno.controller;

import com.xenotask.xeno.service.DataSyncService;
import com.xenotask.xeno.service.SyncService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Tag(name = "Sync", description = "Manual data sync endpoints")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final DataSyncService dataSyncService;
    private final SyncService syncService;

    String k1 = "status";
    String v1 = "started";


    // Per-tenant full sync
    @PostMapping("/full")
    public ResponseEntity<Map<String,Object>> full(@RequestHeader("X-Tenant-ID") String tenantId) {
        syncService.fullSync(tenantId);
        return ResponseEntity.ok(Map.of(k1,v1,"type","full","tenantId", tenantId));
    }

    // Per-tenant incremental sync
    @PostMapping("/incremental")
    public ResponseEntity<Map<String,Object>> incremental(@RequestHeader("X-Tenant-ID") String tenantId,
                                                          @RequestParam("since") @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        syncService.incrementalSync(tenantId, since);
        return ResponseEntity.ok(Map.of(k1,v1,"type","incremental","since", since, "tenantId", tenantId));
    }

    // Global full sync (all active tenants) - admin use
    @PostMapping("/full/all")
    public ResponseEntity<Map<String,Object>> fullAll() {
        dataSyncService.fullSyncAll();
        return ResponseEntity.ok(Map.of(k1,v1,"type","full_all"));
    }

    // Global incremental sync (all active tenants)
    @PostMapping("/incremental/all")
    public ResponseEntity<Map<String,Object>> incrementalAll(@RequestParam("since") @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        dataSyncService.incrementalSyncAll(since);
        return ResponseEntity.ok(Map.of(k1,v1,"type","incremental_all","since", since));
    }
}
