package com.xenotask.xeno.controller;

import com.xenotask.xeno.dto.request.TenantOnboardRequest;
import com.xenotask.xeno.entity.Tenant;
import com.xenotask.xeno.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping("/onboard")
    public ResponseEntity<Tenant> onboard(@Valid @RequestBody TenantOnboardRequest req) {
        Tenant t = tenantService.onboardTenant( req.getShopDomain(), req.getAccessToken());
        return ResponseEntity.ok(t);
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<Tenant> get(@PathVariable String tenantId) {
        return tenantService.findByTenantId(tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> deboard(@PathVariable String tenantId) {
        tenantService.deboardTenant(tenantId);
        return ResponseEntity.noContent().build();
    }
}

