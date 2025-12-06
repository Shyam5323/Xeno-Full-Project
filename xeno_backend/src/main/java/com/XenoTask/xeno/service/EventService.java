package com.xenotask.xeno.service;

import com.xenotask.xeno.entity.Customer;
import com.xenotask.xeno.entity.CustomerEvent;
import com.xenotask.xeno.repository.CustomerEventRepository;
import com.xenotask.xeno.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class EventService {

    private final CustomerEventRepository customerEventRepository;
    private final CustomerRepository customerRepository;
    private final TenantService tenantService;
    private final ObjectMapper objectMapper;

    public EventService(CustomerEventRepository customerEventRepository,
                        CustomerRepository customerRepository,
                        TenantService tenantService,
                        ObjectMapper objectMapper) {
        this.customerEventRepository = customerEventRepository;
        this.customerRepository = customerRepository;
        this.tenantService = tenantService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CustomerEvent recordEvent(String tenantHeaderOrDomain,
                                     Integer customerId,
                                     String eventType,
                                     Map<String,Object> data,
                                     String sessionId,
                                     String ip,
                                     String userAgent) {
        String tenantId = tenantService.resolveTenantIdOrDomain();
        Customer customer = null;
        if (customerId != null) {
            customer = customerRepository.findById(customerId).orElse(null);
        }
        CustomerEvent ev = CustomerEvent.builder()
                .tenant(tenantService.getRequiredByTenantId(tenantId))
                .customer(customer)
                .eventType(eventType)
                .eventData(writeJson(data))
                .sessionId(sessionId)
                .ipAddress(ip)
                .userAgent(userAgent)
                .build();
        // created_at handled by DB default
        return customerEventRepository.save(ev);
    }

    private String writeJson(Map<String,Object> map) {
        if (map == null) return null;
        try { return objectMapper.writeValueAsString(map);} catch (Exception e) { return null; }
    }
}

