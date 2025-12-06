package com.xenotask.xeno.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders", uniqueConstraints = {
        @UniqueConstraint(name = "unique_order_per_tenant", columnNames = {"tenant_id", "shopify_order_id"})
}, indexes = {
        @Index(name = "idx_order_tenant", columnList = "tenant_id"),
        @Index(name = "idx_order_customer", columnList = "customer_id"),
        @Index(name = "idx_order_date", columnList = "tenant_id,created_at"),
        @Index(name = "idx_order_status", columnList = "tenant_id,financial_status"),
        @Index(name = "idx_order_total", columnList = "tenant_id,total_price")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "shopify_order_id", nullable = false)
    private Long shopifyOrderId;

    @Column(name = "order_number", length = 50)
    private String orderNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "financial_status", length = 50)
    private String financialStatus;

    @Column(name = "fulfillment_status", length = 50)
    private String fulfillmentStatus;

    @Column(name = "total_price", precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "subtotal_price", precision = 15, scale = 2)
    private BigDecimal subtotalPrice;

    @Column(name = "total_tax", precision = 15, scale = 2)
    private BigDecimal totalTax;

    @Column(name = "total_discounts", precision = 15, scale = 2)
    private BigDecimal totalDiscounts;

    @Column(name = "total_shipping", precision = 15, scale = 2)
    private BigDecimal totalShipping;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "confirmed")
    @Builder.Default
    private Boolean confirmed = Boolean.TRUE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 100)
    private String cancelReason;

    @Lob
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Lob
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLineItem> lineItems;
}
