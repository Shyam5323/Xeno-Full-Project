package com.xenotask.xeno.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customers", uniqueConstraints = {
        @UniqueConstraint(name = "unique_customer_per_tenant", columnNames = {"tenant_id", "shopify_customer_id"})
}, indexes = {
        @Index(name = "idx_customer_tenant", columnList = "tenant_id"),
        @Index(name = "idx_customer_email", columnList = "tenant_id,email"),
        @Index(name = "idx_customer_total_spent", columnList = "tenant_id,total_spent")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "shopify_customer_id", nullable = false)
    private Long shopifyCustomerId;

    @Column(name = "email")
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "accepts_marketing")
    @Builder.Default
    private Boolean acceptsMarketing = Boolean.FALSE;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "total_spent", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "orders_count")
    @Builder.Default
    private Integer ordersCount = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_order_date")
    private LocalDateTime lastOrderDate;

    @Lob
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Lob
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerAddress> addresses;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Order> orders;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<CustomerEvent> events;
}
