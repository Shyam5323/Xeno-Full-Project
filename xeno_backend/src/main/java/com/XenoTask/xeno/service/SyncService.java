package com.xenotask.xeno.service;

import com.xenotask.xeno.entity.SyncLog;
import com.xenotask.xeno.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Supplier;

@Service
@Slf4j
@RequiredArgsConstructor
public class SyncService {
    private final CustomerService customerService;
    private final ProductService productService;
    private final OrderService orderService;
    private final TenantService tenantService;
    private final SyncLogRepository syncLogRepository;
    private final NotificationService notificationService;


    @Transactional
    public void fullSync(String tenantId) {
        tenantService.getRequiredByTenantId(tenantId); // validate exists
        runLogged(tenantId, "customers", () -> customerService.syncCustomers(tenantId, 250, null));
        runLogged(tenantId, "products", () -> productService.syncProducts(tenantId, 250, null));
        runLogged(tenantId, "orders", () -> orderService.syncOrders(tenantId, null, null, 100, null));
        log.info("Full sync finished tenant={}", tenantId);
    }

    /** Incremental sync since provided timestamp for a single tenant */
    @Transactional
    public void incrementalSync(String tenantId, LocalDateTime since) {
        tenantService.getRequiredByTenantId(tenantId);
        runLogged(tenantId, "customers", () -> customerService.syncCustomersUpdatedSince(tenantId, since, 250));
        runLogged(tenantId, "products", () -> productService.syncProductsUpdatedSince(tenantId, since, 250));
        runLogged(tenantId, "orders", () -> orderService.syncOrdersUpdatedSince(tenantId, since, 100, null));
        log.info("Incremental sync finished tenant={} since={} (updated_at_min)", tenantId, since);
    }
    private void runLogged(String tenantId, String type, Supplier<Integer> work) {
        SyncLog logRow = SyncLog.builder()
                .tenant(tenantService.getRequiredByTenantId(tenantId))
                .syncType(type)
                .status("in_progress")
                .recordsProcessed(0)
                .build();
        logRow = syncLogRepository.save(logRow);
        try {
            int processed = work.get();
            logRow.setRecordsProcessed(processed);
            logRow.setStatus("success");
            logRow.setCompletedAt(LocalDateTime.now());
            syncLogRepository.save(logRow);
        } catch (Exception ex) {
            log.error("Sync segment failed tenant={} type={} msg={}", tenantId, type, ex.getMessage(), ex);
            logRow.setStatus("error");
            logRow.setCompletedAt(LocalDateTime.now());
            logRow.setErrorMessage(ex.getMessage());
            syncLogRepository.save(logRow);
            notificationService.sendSyncFailure(tenantId, type, ex.getMessage());
            throw ex; // bubble to caller
        }
    }
}
