package com.xenotask.xeno.service;

import com.xenotask.xeno.entity.Customer;
import com.xenotask.xeno.entity.CustomerAddress;
import com.xenotask.xeno.entity.Tenant;
import com.xenotask.xeno.repository.CustomerAddressRepository;
import com.xenotask.xeno.repository.CustomerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CustomerUpsertService {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;
    private final TenantService tenantService;

    public CustomerUpsertService(CustomerRepository customerRepository,
                                 CustomerAddressRepository addressRepository,
                                 TenantService tenantService) {
        this.customerRepository = customerRepository;
        this.addressRepository = addressRepository;
        this.tenantService = tenantService;
    }

    @Transactional
    public Customer upsertCustomer(String tenantId, JsonNode node) {
        Tenant tenant = tenantService.getRequiredByTenantId(tenantId);
        long shopifyCustomerId = node.path("id").asLong();
        Customer customer = customerRepository.findByTenantAndShopifyCustomerId(tenant, shopifyCustomerId)
                .orElseGet(() -> Customer.builder()
                        .tenant(tenant)
                        .shopifyCustomerId(shopifyCustomerId)
                        .ordersCount(0)
                        .totalSpent(BigDecimal.ZERO)
                        .build());

        customer.setEmail(asText(node, "email"));
        customer.setFirstName(asText(node, "first_name"));
        customer.setLastName(asText(node, "last_name"));
        customer.setPhone(asText(node, "phone"));
        customer.setAcceptsMarketing(node.path("accepts_marketing").asBoolean(false));
        customer.setState(asText(node, "state"));
        customer.setTotalSpent(asBigDecimal(node.path("total_spent")));
        customer.setOrdersCount(node.path("orders_count").asInt(customer.getOrdersCount() == null ? 0 : customer.getOrdersCount()));
        customer.setLastOrderDate(parseDate(node.path("last_order_created_at")));
        customer.setTags(asText(node, "tags"));
        customer.setNote(asText(node, "note"));

        Customer saved = customerRepository.save(customer);
        handleAddresses(saved, node.path("addresses"));
        return saved;
    }

    private void handleAddresses(Customer customer, JsonNode addressesNode) {
        if (!addressesNode.isArray()) return;
        // basic strategy: delete & recreate (OPTIMIZE: diff later)
        var existing = addressRepository.findByCustomerId(customer.getId());
        addressRepository.deleteAll(existing);
        for (JsonNode addr : addressesNode) {
            CustomerAddress ca = CustomerAddress.builder()
                    .customer(customer)
                    .tenant(customer.getTenant())
                    .shopifyAddressId(addr.path("id").asLong())
                    .address1(asText(addr, "address1"))
                    .address2(asText(addr, "address2"))
                    .city(asText(addr, "city"))
                    .province(asText(addr, "province"))
                    .country(asText(addr, "country"))
                    .zip(asText(addr, "zip"))
                    .isDefault(addr.path("default").asBoolean(false))
                    .build();
            addressRepository.save(ca);
        }
    }

    private String asText(JsonNode n, String field) { return n.hasNonNull(field) ? n.get(field).asText() : null; }
    private BigDecimal asBigDecimal(JsonNode n) { return n.isMissingNode() || n.isNull() ? BigDecimal.ZERO : new BigDecimal(n.asText("0")); }
    private LocalDateTime parseDate(JsonNode n) { return (n == null || n.isNull() || n.asText().isBlank()) ? null : LocalDateTime.parse(n.asText().replace("Z","")); }
}

