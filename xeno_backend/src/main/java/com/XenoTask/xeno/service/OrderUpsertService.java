package com.xenotask.xeno.service;

import com.xenotask.xeno.entity.*;
import com.xenotask.xeno.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class OrderUpsertService {
    private static final Logger log = LoggerFactory.getLogger(OrderUpsertService.class);

    private final OrderRepository orderRepository;
    private final OrderLineItemRepository lineItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final TenantService tenantService;

    public OrderUpsertService(OrderRepository orderRepository,
                              OrderLineItemRepository lineItemRepository,
                              CustomerRepository customerRepository,
                              ProductRepository productRepository,
                              ProductVariantRepository variantRepository,
                              TenantService tenantService) {
        this.orderRepository = orderRepository;
        this.lineItemRepository = lineItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.tenantService = tenantService;
    }

    @Transactional
    public Order upsertOrder(String tenantId, JsonNode node) {
        Tenant tenant = tenantService.getRequiredByTenantId(tenantId);
        long shopifyOrderId = node.path("id").asLong();
        Order order = orderRepository.findByTenantTenantIdAndShopifyOrderId(tenantId, shopifyOrderId)
                .orElseGet(() -> Order.builder().tenant(tenant).shopifyOrderId(shopifyOrderId).build());

        order.setOrderNumber(text(node, "order_number"));
        order.setEmail(text(node, "email"));
        order.setFinancialStatus(text(node, "financial_status"));
        order.setFulfillmentStatus(text(node, "fulfillment_status"));
        order.setTotalPrice(asBig(node.path("total_price")));
        order.setSubtotalPrice(asBig(node.path("subtotal_price")));
        order.setTotalTax(asBig(node.path("total_tax")));
        order.setTotalDiscounts(asBig(node.path("total_discounts")));
        order.setTotalShipping(deriveShipping(node));
        order.setCurrency(text(node, "currency"));
        order.setConfirmed(node.path("confirmed").asBoolean(true));
        order.setCreatedAt(parseDate(node.path("created_at")));
        order.setUpdatedAt(parseDate(node.path("updated_at")));
        order.setCancelledAt(parseDate(node.path("cancelled_at")));
        order.setCancelReason(text(node, "cancel_reason"));
        order.setTags(text(node, "tags"));
        order.setNote(text(node, "note"));

        // attach customer if exists
        JsonNode custNode = node.path("customer");
        if (custNode.isObject()) {
            long shopifyCustomerId = custNode.path("id").asLong();
            Customer customer = customerRepository.findByTenantTenantId(tenantId).stream()
                    .filter(c -> Objects.equals(c.getShopifyCustomerId(), shopifyCustomerId))
                    .findFirst().orElse(null);
            order.setCustomer(customer);
        }

        Order saved = orderRepository.save(order);
        handleLineItems(tenant, saved, node.path("line_items"));
        updateCustomerMetrics(saved.getCustomer());
        return saved;
    }

    private void handleLineItems(Tenant tenant, Order order, JsonNode lineItems) {
        if (!lineItems.isArray()) return;
        // naive: delete existing and recreate
        List<OrderLineItem> existing = lineItemRepository.findByOrderId(order.getId());
        lineItemRepository.deleteAll(existing);
        for (JsonNode li : lineItems) {
            OrderLineItem item = OrderLineItem.builder()
                    .order(order)
                    .tenant(tenant)
                    .shopifyLineItemId(li.path("id").asLong())
                    .title(text(li, "title"))
                    .quantity(li.path("quantity").asInt())
                    .price(asBig(li.path("price")))
                    .totalDiscount(asBigOrZero(li.path("total_discount")))
                    .sku(text(li, "sku"))
                    .vendor(text(li, "vendor"))
                    .build();
            long productId = li.path("product_id").asLong(0);
            long variantId = li.path("variant_id").asLong(0);
            if (productId > 0) productRepository.findByTenantTenantIdAndShopifyProductId(tenant.getTenantId(), productId).ifPresent(item::setProduct);
            if (variantId > 0) variantRepository.findByTenantTenantIdAndShopifyVariantId(tenant.getTenantId(), variantId).ifPresent(item::setVariant);
            lineItemRepository.save(item);
        }
    }

    private void updateCustomerMetrics(Customer customer) {
        if (customer == null) return;
        List<Order> orders = orderRepository.findByTenantTenantId(customer.getTenant().getTenantId());
        BigDecimal spent = orders.stream()
                .filter(o -> o.getCustomer() != null && Objects.equals(o.getCustomer().getId(), customer.getId()))
                .map(o -> o.getTotalPrice() == null ? BigDecimal.ZERO : o.getTotalPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long count = orders.stream().filter(o -> o.getCustomer() != null && Objects.equals(o.getCustomer().getId(), customer.getId())).count();
        customer.setTotalSpent(spent);
        customer.setOrdersCount((int) count);
        customerRepository.save(customer);
    }

    private String text(JsonNode n, String f) { return n.hasNonNull(f) ? n.get(f).asText() : null; }
    private BigDecimal asBig(JsonNode n) { return n.isMissingNode() || n.isNull() || n.asText().isBlank() ? null : new BigDecimal(n.asText()); }
    private BigDecimal asBigOrZero(JsonNode n) { return n.isMissingNode() || n.isNull() || n.asText().isBlank() ? BigDecimal.ZERO : new BigDecimal(n.asText()); }
    private LocalDateTime parseDate(JsonNode n) { return (n == null || n.isNull() || n.asText().isBlank()) ? null : parseFlexible(n.asText()); }
    private LocalDateTime parseFlexible(String ts) {
        try { return java.time.OffsetDateTime.parse(ts).toLocalDateTime(); }
        catch (DateTimeParseException e) { try { return LocalDateTime.parse(ts.replace("Z","")); } catch (Exception e2) { log.warn("Failed to parse date '{}'", ts); return null; } }
    }
    String shipLine = "shipping_lines";
    private BigDecimal deriveShipping(JsonNode order) { return order.has(shipLine) && order.path(shipLine).isArray() && !order.path(shipLine).isEmpty() ? asBig(order.path(shipLine).get(0).path("price")) : null; }
}

