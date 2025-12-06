package com.xenotask.xeno.service;

import com.xenotask.xeno.entity.Product;
import com.xenotask.xeno.entity.ProductVariant;
import com.xenotask.xeno.entity.Tenant;
import com.xenotask.xeno.repository.ProductRepository;
import com.xenotask.xeno.repository.ProductVariantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@Service
public class ProductUpsertService {
    private static final Logger log = LoggerFactory.getLogger(ProductUpsertService.class);

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final TenantService tenantService;

    public ProductUpsertService(ProductRepository productRepository,
                                ProductVariantRepository variantRepository,
                                TenantService tenantService) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.tenantService = tenantService;
    }

    @Transactional
    public Product upsertProduct(String tenantId, JsonNode node) {
        Tenant tenant = tenantService.getRequiredByTenantId(tenantId);
        long shopifyProductId = node.path("id").asLong();
        Product product = productRepository.findByTenantTenantIdAndShopifyProductId(tenantId, shopifyProductId)
                .orElseGet(() -> Product.builder().tenant(tenant).shopifyProductId(shopifyProductId).build());
        product.setTitle(text(node, "title"));
        product.setHandle(text(node, "handle"));
        product.setBodyHtml(text(node, "body_html"));
        product.setVendor(text(node, "vendor"));
        product.setProductType(text(node, "product_type"));
        product.setStatus(text(node, "status"));
        product.setTags(text(node, "tags"));
        product.setCreatedAt(parseDate(node.path("created_at")));
        product.setUpdatedAt(parseDate(node.path("updated_at")));
        product.setPublishedAt(parseDate(node.path("published_at")));
        Product saved = productRepository.save(product);
        handleVariants(tenant, saved, node.path("variants"));
        return saved;
    }

    private void handleVariants(Tenant tenant, Product product, JsonNode variantsNode) {
        if (!variantsNode.isArray()) return;
        for (JsonNode v : variantsNode) {
            long shopifyVariantId = v.path("id").asLong();
            ProductVariant variant = variantRepository.findByTenantTenantIdAndShopifyVariantId(tenant.getTenantId(), shopifyVariantId)
                    .orElseGet(() -> ProductVariant.builder().tenant(tenant).product(product).shopifyVariantId(shopifyVariantId).build());
            variant.setTitle(text(v, "title"));
            variant.setPrice(asBigDecimal(v.path("price")));
            variant.setCompareAtPrice(asBigDecimal(v.path("compare_at_price")));
            variant.setSku(text(v, "sku"));
            variant.setInventoryQuantity(v.path("inventory_quantity").asInt(variant.getInventoryQuantity() == null ? 0 : variant.getInventoryQuantity()));
            variant.setWeight(asBigDecimal(v.path("weight")));
            variant.setRequiresShipping(v.path("requires_shipping").asBoolean(true));
            variant.setTaxable(v.path("taxable").asBoolean(true));
            variant.setCreatedAt(parseDate(v.path("created_at")));
            variant.setUpdatedAt(parseDate(v.path("updated_at")));
            variantRepository.save(variant);
        }
    }

    private String text(JsonNode n, String f) { return n.hasNonNull(f) ? n.get(f).asText() : null; }
    private BigDecimal asBigDecimal(JsonNode n) { return n.isMissingNode() || n.isNull() || n.asText().isBlank() ? null : new BigDecimal(n.asText()); }
    private LocalDateTime parseDate(JsonNode n) {
        if (n == null || n.isNull()) return null;
        String ts = n.asText();
        if (ts.isBlank()) return null;
        try {
            return OffsetDateTime.parse(ts).toLocalDateTime();
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(ts.replace("Z", ""));
            } catch (Exception e2) {
                log.warn("Failed to parse date '{}'", ts);
                return null;
            }
        }
    }
}

