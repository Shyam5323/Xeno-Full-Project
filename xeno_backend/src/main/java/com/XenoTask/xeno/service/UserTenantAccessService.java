package com.xenotask.xeno.service;

import com.xenotask.xeno.dto.response.TenantStatsResponse;
import com.xenotask.xeno.entity.Tenant;
import com.xenotask.xeno.entity.User;
import com.xenotask.xeno.entity.UserTenantAccess;
import com.xenotask.xeno.repository.TenantRepository;
import com.xenotask.xeno.repository.UserRepository;
import com.xenotask.xeno.repository.UserTenantAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserTenantAccessService {

    private static final String ERR_NO_ACCESS = "User does not have access to this tenant";
    private static final String ROLE_ADMIN = "admin";

    private final UserTenantAccessRepository userTenantAccessRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    /**
     * Create a new tenant and link it to the user
     */
    public UserTenantAccess onboardTenant(Integer userId, String shopDomain, String accessToken, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if tenant already exists
        Optional<Tenant> existingTenant = tenantRepository.findByShopDomain(shopDomain);
        Tenant tenant;

        if (existingTenant.isPresent()) {
            tenant = existingTenant.get();
            // Update access token if needed
            tenant.setAccessToken(accessToken);
            tenant.setUpdatedAt(LocalDateTime.now());
            tenantRepository.save(tenant);
        } else {
            // Create new tenant
            tenant = Tenant.builder()
                    .tenantId(generateTenantId(shopDomain))
                    .shopDomain(shopDomain)
                    .shopName(extractShopName(shopDomain))
                    .accessToken(accessToken)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            tenant = tenantRepository.save(tenant);
        }

        // Check if user-tenant relationship already exists
        Optional<UserTenantAccess> existingAccess = userTenantAccessRepository
                .findByUserIdAndTenantTenantId(userId, tenant.getTenantId());

        if (existingAccess.isPresent()) {
            throw new IllegalArgumentException("User already has access to this tenant");
        }

        // Create user-tenant access
        UserTenantAccess userTenantAccess = UserTenantAccess.builder()
                .user(user)
                .tenant(tenant)
                .role(role != null ? role : ROLE_ADMIN)
                .build();

        return userTenantAccessRepository.save(userTenantAccess);
    }

    /**
     * Get all tenants accessible by a user
     */
    @Transactional(readOnly = true)
    public List<UserTenantAccess> getUserTenantAccesses(Integer userId) {
        return userTenantAccessRepository.findByUserId(userId);
    }

    /**
     * Get all users with access to a tenant
     */
    @Transactional(readOnly = true)
    public List<UserTenantAccess> getTenantUserAccesses(String tenantId) {
        return userTenantAccessRepository.findByTenantTenantId(tenantId);
    }

    /**
     * Remove user access to a tenant
     */
    public void removeTenantAccess(Integer userId, String tenantId) {
        UserTenantAccess access = userTenantAccessRepository
                .findByUserIdAndTenantTenantId(userId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User does not have access to this tenant"));

        userTenantAccessRepository.delete(access);
    }

    /**
     * Update user role for a tenant
     */
    public UserTenantAccess updateUserRole(Integer userId, String tenantId, String newRole) {
        UserTenantAccess access = userTenantAccessRepository
                .findByUserIdAndTenantTenantId(userId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User does not have access to this tenant"));

        access.setRole(newRole);
        return userTenantAccessRepository.save(access);
    }

    /**
     * Check if user has access to a specific tenant
     */
    @Transactional(readOnly = true)
    public boolean hasAccessToTenant(Integer userId, String tenantId) {
        return userTenantAccessRepository.findByUserIdAndTenantTenantId(userId, tenantId).isPresent();
    }

    /**
     * Get user access details for a specific tenant
     */
    @Transactional(readOnly = true)
    public Optional<UserTenantAccess> getUserTenantAccess(Integer userId, String tenantId) {
        return userTenantAccessRepository.findByUserIdAndTenantTenantId(userId, tenantId);
    }

    /**
     * Get statistics for a specific tenant that the user has access to
     */
    @Transactional(readOnly = true)
    public TenantStatsResponse getTenantStatistics(Integer userId, String tenantId) {
        UserTenantAccess access = userTenantAccessRepository
                .findByUserIdAndTenantTenantId(userId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(ERR_NO_ACCESS));

        return buildStats(access.getTenant());
    }

    /**
     * Get statistics for all tenants the user has access to
     */
    @Transactional(readOnly = true)
    public List<TenantStatsResponse> getAllTenantStatistics(Integer userId) {
        List<UserTenantAccess> accesses = userTenantAccessRepository.findByUserId(userId);
        return accesses.stream()
                .map(a -> buildStats(a.getTenant()))
                .collect(Collectors.toList());
    }

    private TenantStatsResponse buildStats(Tenant tenant) {
        Long totalOrders = tenant.getOrders() != null ? (long) tenant.getOrders().size() : 0L;
        Long totalCustomers = tenant.getCustomers() != null ? (long) tenant.getCustomers().size() : 0L;

        BigDecimal totalRevenue = BigDecimal.ZERO;
        if (tenant.getOrders() != null) {
            totalRevenue = tenant.getOrders().stream()
                    .map(o -> o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        LocalDateTime lastSyncAt = null;
        if (tenant.getSyncLogs() != null && !tenant.getSyncLogs().isEmpty()) {
            lastSyncAt = tenant.getSyncLogs().stream()
                    .map(sl -> sl.getCompletedAt() != null ? sl.getCompletedAt() : sl.getStartedAt())
                    .filter(dt -> dt != null)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        }

        return TenantStatsResponse.builder()
                .tenantId(tenant.getTenantId())
                .shopDomain(tenant.getShopDomain())
                .shopName(tenant.getShopName())
                .totalOrders(totalOrders)
                .totalCustomers(totalCustomers)
                .totalRevenue(totalRevenue)
                .lastSyncAt(lastSyncAt)
                .isActive(tenant.getIsActive())
                .build();
    }

    private String generateTenantId(String shopDomain) {
        // Generate a unique tenant ID based on shop domain
        return shopDomain.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
    }

    private String extractShopName(String shopDomain) {
        // Extract shop name from domain (remove .myshopify.com if present)
        if (shopDomain.contains(".")) {
            return shopDomain.substring(0, shopDomain.indexOf("."));
        }
        return shopDomain;
    }

    public boolean isOwner(String tenantId, Integer userId) {
        return userTenantAccessRepository
                .findByUserIdAndTenantTenantId(userId, tenantId)
                .map(uta -> ROLE_ADMIN.equalsIgnoreCase(uta.getRole()))
                .orElse(false);
    }
}
