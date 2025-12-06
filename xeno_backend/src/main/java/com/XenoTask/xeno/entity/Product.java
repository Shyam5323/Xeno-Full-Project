package com.xenotask.xeno.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products", uniqueConstraints = {
        @UniqueConstraint(name = "unique_product_per_tenant", columnNames = {"tenant_id", "shopify_product_id"})
}, indexes = {
        @Index(name = "idx_product_tenant", columnList = "tenant_id"),
        @Index(name = "idx_product_status", columnList = "tenant_id,status"),
        @Index(name = "idx_product_type", columnList = "tenant_id,product_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "shopify_product_id", nullable = false)
    private Long shopifyProductId;

    @Column(name = "title", length = 500, nullable = false)
    private String title;

    @Column(name = "handle")
    private String handle;

    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(name = "vendor")
    private String vendor;

    @Column(name = "product_type")
    private String productType;

    @Column(name = "status", length = 50)
    private String status; // active, archived, draft

    @Lob
    @Column(name = "tags")
    private String tags;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants;

    @OneToMany(mappedBy = "product")
    private List<OrderLineItem> lineItems;
}
