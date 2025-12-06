package com.xenotask.xeno.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.xenotask.xeno.repository.ProductVariantRepository;
import com.xenotask.xeno.repository.projection.ProductStockRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ShopifyApiService shopifyApiService;
    private final ProductUpsertService productUpsertService;
    private final ProductVariantRepository productVariantRepository;

    public ProductService(ShopifyApiService shopifyApiService,
                          ProductUpsertService productUpsertService,
                          ProductVariantRepository productVariantRepository) {
        this.shopifyApiService = shopifyApiService;
        this.productUpsertService = productUpsertService;
        this.productVariantRepository = productVariantRepository;
    }

    @Transactional
    public Integer syncProducts(String tenantId, Integer limit, Integer pages) {
        int total = 0;
        int pageCount = 0;
        String nextCursor = null;
        int effectiveLimit = (limit == null || limit <= 0) ? 50 : limit;
        do {
            ShopifyApiService.PagedResult pr = shopifyApiService.getCursorPage(
                    tenantId,
                    "/products.json",
                    effectiveLimit,
                    nextCursor,
                    null,
                    "products");
            JsonNode arr = pr.items();
            if (!arr.isArray() || arr.isEmpty()) break;
            for (JsonNode p : arr) { productUpsertService.upsertProduct(tenantId, p); total++; }
            pageCount++;
            nextCursor = pr.nextPageInfo();
        } while (nextCursor != null && (pages == null || pageCount < pages));
        log.info("Products synced tenant={} count={} pages={}", tenantId, total, pageCount);
        return total;
    }

    /**
     * Incremental products sync using updated_at_min
     */
    @Transactional
    public Integer syncProductsUpdatedSince(String tenantId, LocalDateTime updatedSince, Integer limit) {
        int total = 0;
        String cursor = null;
        int effectiveLimit = (limit == null || limit <= 0) ? 50 : limit;
        String updatedAtMin = updatedSince == null ? null : updatedSince.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Map<String,String> extra = new HashMap<>();
        if (updatedAtMin != null) extra.put("updated_at_min", updatedAtMin);
        do {
            ShopifyApiService.PagedResult pr = shopifyApiService.getCursorPage(
                    tenantId,
                    "/products.json",
                    effectiveLimit,
                    cursor,
                    extra,
                    "products");
            JsonNode arr = pr.items();
            if (!arr.isArray() || arr.isEmpty()) break;
            for (JsonNode p : arr) { productUpsertService.upsertProduct(tenantId, p); total++; }
            cursor = pr.nextPageInfo();
        } while (cursor != null);
        log.info("Incremental products synced tenant={} count={} updated_at_min={}", tenantId, total, updatedAtMin);
        return total;
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> getStockOutProducts(String tenantId, int limit) {
        int effectiveLimit = limit <= 0 ? 5 : limit;
        Pageable pageable = PageRequest.of(0, effectiveLimit);
        List<ProductStockRow> rows = productVariantRepository.findLowestStockProducts(tenantId, pageable);
        return rows.stream()
                .map(r -> {
                    Map<String,Object> m = new HashMap<>();
                    m.put("productId", r.getProductId());
                    m.put("title", r.getTitle());
                    m.put("quantity", r.getQty());
                    return m;
                })
                .collect(Collectors.toList());
    }
}
