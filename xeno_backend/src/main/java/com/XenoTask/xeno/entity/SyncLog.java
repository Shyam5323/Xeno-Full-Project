package com.xenotask.xeno.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_logs", indexes = {
        @Index(name = "idx_sync_tenant", columnList = "tenant_id"),
        @Index(name = "idx_sync_status", columnList = "tenant_id,status"),
        @Index(name = "idx_sync_date", columnList = "tenant_id,started_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "sync_type", length = 100, nullable = false)
    private String syncType; // customers, orders, products, webhooks

    @Column(name = "status", length = 50, nullable = false)
    private String status; // success, error, in_progress

    @Column(name = "records_processed")
    @Builder.Default
    private Integer recordsProcessed = 0;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "started_at", insertable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
