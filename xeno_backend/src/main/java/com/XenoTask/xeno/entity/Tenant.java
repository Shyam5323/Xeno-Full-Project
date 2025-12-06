package com.xenotask.xeno.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "tenants", indexes = {
        @Index(name = "idx_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_shop_domain", columnList = "shop_domain")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @Column(name = "tenant_id", length = 255, nullable = false, updatable = false)
    private String tenantId;

    @Column(name = "shop_domain", length = 255, nullable = false, unique = true)
    private String shopDomain;

    @Column(name = "shop_name")
    private String shopName;

    @Lob
    @Column(name = "access_token")
    @JsonIgnore // do not expose encrypted token in API responses
    private String accessToken;

    @Column(name = "webhook_secret")
    private String webhookSecret;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "plan_name", length = 100)
    private String planName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "timezone", length = 100)
    private String timezone;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Customer> customers;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Product> products;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Order> orders;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CustomerEvent> customerEvents;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<SyncLog> syncLogs;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<UserTenantAccess> userTenantAccesses;
}
