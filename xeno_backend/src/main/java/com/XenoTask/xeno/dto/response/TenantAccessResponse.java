package com.xenotask.xeno.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantAccessResponse {
    private Integer accessId;
    private String tenantId;
    private String shopDomain;
    private String shopName;
    private String role;
    private LocalDateTime createdAt;
    private Boolean isActive;
}
