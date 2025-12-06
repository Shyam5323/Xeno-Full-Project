package com.xenotask.xeno.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantStatsResponse {
    private String tenantId;
    private String shopDomain;
    private String shopName;
    private Long totalOrders;
    private Long totalCustomers;
    private BigDecimal totalRevenue;
    private LocalDateTime lastSyncAt;
    private Boolean isActive;
}
