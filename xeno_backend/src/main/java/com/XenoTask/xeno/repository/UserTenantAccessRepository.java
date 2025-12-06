package com.xenotask.xeno.repository;

import com.xenotask.xeno.entity.UserTenantAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTenantAccessRepository extends JpaRepository<UserTenantAccess, Integer> {
    List<UserTenantAccess> findByUserId(Integer userId);
    List<UserTenantAccess> findByTenantTenantId(String tenantId);
    Optional<UserTenantAccess> findByUserIdAndTenantTenantId(Integer userId, String tenantId);
}

