package com.xenotask.xeno.service;

import com.xenotask.xeno.entity.Tenant;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DataSyncService {
    private static final Logger log = LoggerFactory.getLogger(DataSyncService.class);
    private final TenantService tenantService;
    private final SyncService syncService;

    /** Full sync for all active tenants */
    public void fullSyncAll() {
        List<Tenant> tenants = tenantService.listActiveTenants();
        for (Tenant t : tenants) {
            try {
                syncService.fullSync(t.getTenantId());
            } catch (Exception ex) {
                log.error("Full sync failed for tenant={} msg={}", t.getTenantId(), ex.getMessage());
            }
        }
    }

    /** Incremental sync for all active tenants */
    public void incrementalSyncAll(LocalDateTime since) {
        List<Tenant> tenants = tenantService.listActiveTenants();
        for (Tenant t : tenants) {
            try {
                syncService.incrementalSync(t.getTenantId(), since);
            } catch (Exception ex) {
                log.error("Incremental sync failed tenant={} msg={}", t.getTenantId(), ex.getMessage());
            }
        }
    }


}
