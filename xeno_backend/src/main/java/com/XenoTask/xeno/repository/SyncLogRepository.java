package com.xenotask.xeno.repository;

import com.xenotask.xeno.entity.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Integer> {
    List<SyncLog> findByTenantTenantId(String tenantId);
    List<SyncLog> findByTenantTenantIdAndStatus(String tenantId, String status);
    List<SyncLog> findByTenantTenantIdAndStartedAtBetween(String tenantId, LocalDateTime start, LocalDateTime end);
}

