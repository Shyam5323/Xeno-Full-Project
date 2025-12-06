package com.xenotask.xeno.repository;

import com.xenotask.xeno.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    Optional<Product> findByTenantTenantIdAndShopifyProductId(String tenantId, Long shopifyProductId);
    List<Product> findByTenantTenantId(String tenantId);
    List<Product> findByTenantTenantIdAndStatus(String tenantId, String status);
}

