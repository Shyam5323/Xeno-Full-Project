package com.xenotask.xeno.repository;

import com.xenotask.xeno.entity.OrderLineItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderLineItemRepository extends JpaRepository<OrderLineItem, Integer> {
    List<OrderLineItem> findByOrderId(Integer orderId);
    List<OrderLineItem> findByTenantTenantId(String tenantId);
    List<OrderLineItem> findByProductId(Integer productId);
    List<OrderLineItem> findByVariantId(Integer variantId);

    // Units and distinct orders in range for UPT
    @Query("select coalesce(sum(li.quantity),0), count(distinct o.id) from OrderLineItem li join li.order o where li.tenant.tenantId=:tenantId and o.createdAt between :start and :end")
    List<Object[]> unitsAndOrdersInRange(String tenantId, LocalDateTime start, LocalDateTime end);

    // Top products by revenue in range
    @Query("select li.product.id, li.product.title, coalesce(sum(li.quantity),0) as qty, " +
           "coalesce(sum(li.price * li.quantity) - sum(coalesce(li.totalDiscount,0)),0) as revenue " +
           "from OrderLineItem li join li.order o " +
           "where li.tenant.tenantId=:tenantId and o.createdAt between :start and :end and li.product is not null " +
           "group by li.product.id, li.product.title " +
           "order by revenue desc")
    List<Object[]> topProductsByRevenue(String tenantId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Top products by quantity in range
    @Query("select li.product.id, li.product.title, coalesce(sum(li.quantity),0) as qty, " +
           "coalesce(sum(li.price * li.quantity) - sum(coalesce(li.totalDiscount,0)),0) as revenue " +
           "from OrderLineItem li join li.order o " +
           "where li.tenant.tenantId=:tenantId and o.createdAt between :start and :end and li.product is not null " +
           "group by li.product.id, li.product.title " +
           "order by qty desc")
    List<Object[]> topProductsByQuantity(String tenantId, LocalDateTime start, LocalDateTime end, Pageable pageable);
}
