package com.xenotask.xeno.repository;

import com.xenotask.xeno.entity.CustomerEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CustomerEventRepository extends JpaRepository<CustomerEvent, Integer> {
    List<CustomerEvent> findByTenantTenantId(String tenantId);
    List<CustomerEvent> findByTenantTenantIdAndEventType(String tenantId, String eventType);
    List<CustomerEvent> findByTenantTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime start, LocalDateTime end);
    List<CustomerEvent> findByCustomerId(Integer customerId);
}

