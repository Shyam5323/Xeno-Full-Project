package com.xenotask.xeno.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_events", indexes = {
        @Index(name = "idx_event_tenant", columnList = "tenant_id"),
        @Index(name = "idx_event_customer", columnList = "customer_id"),
        @Index(name = "idx_event_type", columnList = "tenant_id,event_type"),
        @Index(name = "idx_event_date", columnList = "tenant_id,created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;

    @Column(name = "event_data", columnDefinition = "JSON")
    private String eventData; // raw JSON string

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // Option B: expect existing MySQL TINYTEXT
    @Column(name = "user_agent", columnDefinition = "TINYTEXT")
    private String userAgent;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
