package com.xenotask.xeno.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_variants", uniqueConstraints = {
        @UniqueConstraint(name = "unique_variant_per_tenant", columnNames = {"tenant_id", "shopify_variant_id"})
}, indexes = {
        @Index(name = "idx_variant_product", columnList = "product_id"),
        @Index(name = "idx_variant_sku", columnList = "tenant_id,sku")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "shopify_variant_id", nullable = false)
    private Long shopifyVariantId;

    @Column(name = "title")
    private String title;

    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_at_price", precision = 15, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(name = "sku")
    private String sku;

    @Column(name = "inventory_quantity")
    @Builder.Default
    private Integer inventoryQuantity = 0;

    @Column(name = "weight", precision = 10, scale = 2)
    private BigDecimal weight;

    @Column(name = "requires_shipping")
    @Builder.Default
    private Boolean requiresShipping = Boolean.TRUE;

    @Column(name = "taxable")
    @Builder.Default
    private Boolean taxable = Boolean.TRUE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
