package com.xenotask.xeno.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;


@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final ShopifyApiService shopifyApiService;
    private final OrderUpsertService orderUpsertService;

    public OrderService(ShopifyApiService shopifyApiService,
                        OrderUpsertService orderUpsertService) {
        this.shopifyApiService = shopifyApiService;
        this.orderUpsertService = orderUpsertService;
    }

    @Transactional
    public Integer syncOrders(String tenantId, LocalDateTime createdAfter, LocalDateTime createdBefore, Integer limit, Integer pages) {
        int total = 0;
        int pageCount = 0;
        String nextCursor = null;
        int effectiveLimit = (limit == null || limit <= 0) ? 50 : limit;
        Map<String,String> baseParams = new LinkedHashMap<>();
        if (createdAfter != null) baseParams.put("created_at_min", createdAfter.toString());
        if (createdBefore != null) baseParams.put("created_at_max", createdBefore.toString());
        do {
            ShopifyApiService.PagedResult pr = shopifyApiService.getCursorPage(
                    tenantId,
                    "/orders.json",
                    effectiveLimit,
                    nextCursor,
                    baseParams,
                    "orders");
            JsonNode arr = pr.items();
            if (!arr.isArray() || arr.isEmpty()) break;
            for (JsonNode o : arr) { orderUpsertService.upsertOrder(tenantId, o); total++; }
            pageCount++;
            nextCursor = pr.nextPageInfo();
        } while (nextCursor != null && (pages == null || pageCount < pages));
        log.info("Orders synced tenant={} count={} pages={}", tenantId, total, pageCount);
        return total;
    }

    /**
     * Incremental orders sync using updated_at_min (falls back to created_at_min if desired)
     */
    @Transactional
    public Integer syncOrdersUpdatedSince(String tenantId, LocalDateTime updatedSince, Integer limit, Integer pages) {
        int total = 0;
        int pageCount = 0;
        String nextCursor = null;
        int effectiveLimit = (limit == null || limit <= 0) ? 50 : limit;
        String updatedAtMin = updatedSince == null ? null : updatedSince.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Map<String,String> baseParams = new LinkedHashMap<>();
        if (updatedAtMin != null) baseParams.put("updated_at_min", updatedAtMin);
        do {
            ShopifyApiService.PagedResult pr = shopifyApiService.getCursorPage(
                    tenantId,
                    "/orders.json",
                    effectiveLimit,
                    nextCursor,
                    baseParams,
                    "orders");
            JsonNode arr = pr.items();
            if (!arr.isArray() || arr.isEmpty()) break;
            for (JsonNode o : arr) { orderUpsertService.upsertOrder(tenantId, o); total++; }
            pageCount++;
            nextCursor = pr.nextPageInfo();
        } while (nextCursor != null && (pages == null || pageCount < pages));
        log.info("Incremental orders synced tenant={} count={} updated_at_min={}", tenantId, total, updatedAtMin);
        return total;
    }
}
