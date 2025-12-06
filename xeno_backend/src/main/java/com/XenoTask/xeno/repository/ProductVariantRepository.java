package com.xenotask.xeno.repository;

import com.xenotask.xeno.entity.ProductVariant;
import com.xenotask.xeno.repository.projection.ProductStockRow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    Optional<ProductVariant> findByTenantTenantIdAndShopifyVariantId(String tenantId, Long shopifyVariantId);
    List<ProductVariant> findByProductId(Integer productId);
    List<ProductVariant> findByTenantTenantId(String tenantId);
    List<ProductVariant> findByTenantTenantIdAndSku(String tenantId, String sku);

    @Query("select pv.product.id as productId, pv.product.title as title, sum(coalesce(pv.inventoryQuantity,0)) as qty " +
           "from ProductVariant pv " +
           "where pv.tenant.tenantId = :tenantId " +
           "group by pv.product.id, pv.product.title " +
           "order by qty asc")
    List<ProductStockRow> findLowestStockProducts(String tenantId, Pageable pageable);
}
