package com.xenotask.xeno.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_tenant_access", uniqueConstraints = {
        @UniqueConstraint(name = "unique_user_tenant", columnNames = {"user_id", "tenant_id"})
}, indexes = {
        @Index(name = "idx_access_user", columnList = "user_id"),
        @Index(name = "idx_access_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTenantAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "role", length = 50)
    @Builder.Default
    private String role = "admin"; // admin, viewer, etc.

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
