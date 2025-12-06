package com.xenotask.xeno.repository;

import com.xenotask.xeno.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {
    Optional<Tenant> findByShopDomain(String shopDomain);
    boolean existsByShopDomain(String shopDomain);
    List<Tenant> findAllByIsActiveTrue();
}
