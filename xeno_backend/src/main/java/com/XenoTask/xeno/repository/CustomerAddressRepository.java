package com.xenotask.xeno.repository;

import com.xenotask.xeno.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Integer> {
    List<CustomerAddress> findByCustomerId(Integer customerId);
    List<CustomerAddress> findByTenantTenantId(String tenantId);
    List<CustomerAddress> findByCustomerIdAndIsDefaultTrue(Integer customerId);
}

