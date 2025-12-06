package com.xenotask.xeno.controller;

import com.xenotask.xeno.dto.request.TenantOnboardRequest;
import com.xenotask.xeno.dto.request.UpdateRoleRequest;
import com.xenotask.xeno.dto.response.TenantAccessResponse;
import com.xenotask.xeno.dto.response.TenantStatsResponse;
import com.xenotask.xeno.entity.UserTenantAccess;
import com.xenotask.xeno.security.UserPrincipal;
import com.xenotask.xeno.service.UserTenantAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tenant-access")
@RequiredArgsConstructor
public class TenantOnboardingController {

    private static final String ROLE_ADMIN = "admin";
    private static final String ACCESS_NOT_FOUND = "Access not found for this tenant";

    private final UserTenantAccessService userTenantAccessService;

    /**
     * Onboard a new tenant for the authenticated user
     */
    @PostMapping("/onboard")
    public ResponseEntity<TenantAccessResponse> onboardTenant(@Valid @RequestBody TenantOnboardRequest request) {
        Integer userId = getCurrentUserId();

        UserTenantAccess access = userTenantAccessService.onboardTenant(
                userId,
                request.getShopDomain(),
                request.getAccessToken(),
                ROLE_ADMIN // Default role for tenant owner
        );

        TenantAccessResponse response = mapToTenantAccessResponse(access);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all tenants accessible by the authenticated user
     */
    @GetMapping("/my-tenants")
    public ResponseEntity<List<TenantAccessResponse>> getMyTenants() {
        Integer userId = getCurrentUserId();

        List<UserTenantAccess> accesses = userTenantAccessService.getUserTenantAccesses(userId);
        List<TenantAccessResponse> responses = accesses.stream()
                .map(this::mapToTenantAccessResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get specific tenant access details
     */
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<TenantAccessResponse> getTenantAccess(@PathVariable String tenantId) {
        Integer userId = getCurrentUserId();

        UserTenantAccess access = userTenantAccessService.getUserTenantAccess(userId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(ACCESS_NOT_FOUND));

        TenantAccessResponse response = mapToTenantAccessResponse(access);
        return ResponseEntity.ok(response);
    }

    /**
     * Update user role for a specific tenant (admin only)
     */
    @PutMapping("/tenant/{tenantId}/role")
    public ResponseEntity<TenantAccessResponse> updateRole(
            @PathVariable String tenantId,
            @Valid @RequestBody UpdateRoleRequest request) {
        Integer userId = getCurrentUserId();

        // Check if current user has admin access to this tenant
        UserTenantAccess currentAccess = userTenantAccessService.getUserTenantAccess(userId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(ACCESS_NOT_FOUND));

        if (!ROLE_ADMIN.equals(currentAccess.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UserTenantAccess updatedAccess = userTenantAccessService.updateUserRole(userId, tenantId, request.getRole());
        TenantAccessResponse response = mapToTenantAccessResponse(updatedAccess);

        return ResponseEntity.ok(response);
    }

    /**
     * Remove access to a tenant
     */
    @DeleteMapping("/tenant/{tenantId}")
    public ResponseEntity<Void> removeTenantAccess(@PathVariable String tenantId) {
        Integer userId = getCurrentUserId();

        userTenantAccessService.removeTenantAccess(userId, tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if user has access to a specific tenant
     */
    @GetMapping("/tenant/{tenantId}/check-access")
    public ResponseEntity<Boolean> checkTenantAccess(@PathVariable String tenantId) {
        Integer userId = getCurrentUserId();

        boolean hasAccess = userTenantAccessService.hasAccessToTenant(userId, tenantId);
        return ResponseEntity.ok(hasAccess);
    }

    /**
     * Get all users with access to a specific tenant (admin only)
     */
    @GetMapping("/tenant/{tenantId}/users")
    public ResponseEntity<List<TenantAccessResponse>> getTenantUsers(@PathVariable String tenantId) {
        Integer userId = getCurrentUserId();

        // Check if current user has admin access to this tenant
        UserTenantAccess currentAccess = userTenantAccessService.getUserTenantAccess(userId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(ACCESS_NOT_FOUND));

        if (!ROLE_ADMIN.equals(currentAccess.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<UserTenantAccess> accesses = userTenantAccessService.getTenantUserAccesses(tenantId);
        List<TenantAccessResponse> responses = accesses.stream()
                .map(this::mapToTenantAccessResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get statistics for a specific tenant
     */
    @GetMapping("/tenant/{tenantId}/stats")
    public ResponseEntity<TenantStatsResponse> getTenantStats(@PathVariable String tenantId) {
        Integer userId = getCurrentUserId();

        if (!userTenantAccessService.hasAccessToTenant(userId, tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        TenantStatsResponse stats = userTenantAccessService.getTenantStatistics(userId, tenantId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get statistics for all user's tenants
     */
    @GetMapping("/my-tenant-stats")
    public ResponseEntity<List<TenantStatsResponse>> getAllMyTenantStats() {
        Integer userId = getCurrentUserId();

        List<TenantStatsResponse> stats = userTenantAccessService.getAllTenantStatistics(userId);
        return ResponseEntity.ok(stats);
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        if (authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUserId();
        }

        throw new IllegalStateException("Invalid authentication principal");
    }

    private TenantAccessResponse mapToTenantAccessResponse(UserTenantAccess access) {
        return TenantAccessResponse.builder()
                .accessId(access.getId())
                .tenantId(access.getTenant().getTenantId())
                .shopDomain(access.getTenant().getShopDomain())
                .shopName(access.getTenant().getShopName())
                .role(access.getRole())
                .createdAt(access.getCreatedAt())
                .isActive(access.getTenant().getIsActive())
                .build();
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<String> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler({IllegalStateException.class})
    public ResponseEntity<String> handleUnauthorized(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }
}
