package com.xenotask.xeno.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantOnboardRequest {
    @NotBlank
    private String shopDomain;
    @NotBlank
    private String accessToken;
}

