package com.xenotask.xeno.repository;

import com.xenotask.xeno.entity.Customer;
import com.xenotask.xeno.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByTenantAndShopifyCustomerId(Tenant tenant, Long shopifyCustomerId);
    List<Customer> findByTenantTenantId(String tenantId);
    Optional<Customer> findByTenantTenantIdAndEmail(String tenantId, String email);

    @Query("select c from Customer c where c.tenant.tenantId = :tenantId and c.totalSpent > :minSpent")
    List<Customer> findHighValueCustomers(String tenantId, java.math.BigDecimal minSpent);
}

