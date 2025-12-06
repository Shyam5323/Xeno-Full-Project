package com.xenotask.xeno.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final ShopifyApiService shopifyApiService;
    private final CustomerUpsertService customerUpsertService;

    public CustomerService(ShopifyApiService shopifyApiService,
                           CustomerUpsertService customerUpsertService) {
        this.shopifyApiService = shopifyApiService;
        this.customerUpsertService = customerUpsertService;
    }

    @Transactional
    public Integer syncCustomers(String tenantId, Integer limit, Integer pages) {
        int total = 0;
        int pageCount = 0;
        String nextCursor = null;
        int effectiveLimit = (limit == null || limit <= 0) ? 50 : limit;
        do {
            ShopifyApiService.PagedResult pr = shopifyApiService.getCursorPage(
                    tenantId,
                    "/customers.json",
                    effectiveLimit,
                    nextCursor,
                    null,
                    "customers");
            JsonNode arr = pr.items();
            if (!arr.isArray() || arr.isEmpty()) break;
            for (JsonNode cNode : arr) {
                customerUpsertService.upsertCustomer(tenantId, cNode);
                total++;
            }
            pageCount++;
            nextCursor = pr.nextPageInfo();
        } while (nextCursor != null && (pages == null || pageCount < pages));
        log.info("Customers synced tenant={} count={} pages={}", tenantId, total, pageCount);
        return total;
    }

    /**
     * Incremental customers sync using updated_at_min
     */
    @Transactional
    public Integer syncCustomersUpdatedSince(String tenantId, LocalDateTime updatedSince, Integer limit) {
        int total = 0;
        String cursor = null;
        int effectiveLimit = (limit == null || limit <= 0) ? 50 : limit;
        String updatedAtMin = updatedSince == null ? null : updatedSince.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Map<String,String> extra = new HashMap<>();
        if (updatedAtMin != null) extra.put("updated_at_min", updatedAtMin);
        do {
            ShopifyApiService.PagedResult pr = shopifyApiService.getCursorPage(
                    tenantId,
                    "/customers.json",
                    effectiveLimit,
                    cursor,
                    extra,
                    "customers");
            JsonNode arr = pr.items();
            if (!arr.isArray() || arr.isEmpty()) break;
            for (JsonNode cNode : arr) { customerUpsertService.upsertCustomer(tenantId, cNode); total++; }
            cursor = pr.nextPageInfo();
        } while (cursor != null);
        log.info("Incremental customers synced tenant={} count={} updated_at_min={}", tenantId, total, updatedAtMin);
        return total;
    }
}
