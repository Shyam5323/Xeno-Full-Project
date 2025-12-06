package com.xenotask.xeno.controller;

import com.xenotask.xeno.entity.Product;
import com.xenotask.xeno.security.UserPrincipal;
import com.xenotask.xeno.service.AnalyticsService;
import com.xenotask.xeno.service.ProductService;
import com.xenotask.xeno.service.UserTenantAccessService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "Analytics", description = "Analytics KPIs and reports")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserTenantAccessService userTenantAccessService;
    private final ProductService productService;

    public AnalyticsController(AnalyticsService analyticsService, UserTenantAccessService userTenantAccessService, ProductService productService) {
        this.analyticsService = analyticsService;
        this.userTenantAccessService = userTenantAccessService;
        this.productService = productService;
    }

    @GetMapping("/revenue")
    public ResponseEntity<Map<String,Object>> revenue(@RequestHeader("X-Tenant-ID") String tenant,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        if (!hasAccessToTenant(tenant)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var total = analyticsService.revenueInRange(tenant, start, end);
        return ResponseEntity.ok(Map.of("totalRevenue", total));
    }

    @GetMapping("/revenue/daily")
    public ResponseEntity<List<Map<String,Object>>> daily(@RequestHeader("X-Tenant-ID") String tenant,
                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        if (!hasAccessToTenant(tenant)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(analyticsService.dailyRevenue(tenant, start, end));
    }

    @GetMapping("/orders/status-breakdown")
    public ResponseEntity<List<Map<String,Object>>> status(@RequestHeader("X-Tenant-ID") String tenant) {
        if (!hasAccessToTenant(tenant)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Fetching status breakdown for tenant: {}", tenant);
        return ResponseEntity.ok(analyticsService.statusBreakdown(tenant));
    }

    @GetMapping("/customers/top")
    public ResponseEntity<List<Map<String,Object>>> topCustomers(@RequestHeader("X-Tenant-ID") String tenant,
                                                                 @RequestParam(defaultValue = "5") @Min(1) int limit) {
        if (!hasAccessToTenant(tenant)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(analyticsService.topCustomers(tenant, limit));
    }

    @GetMapping("/customers/stockout")
    public ResponseEntity<List<Map<String,Object>>> stockout(@RequestHeader("X-Tenant-ID") String tenant,
                                                             @RequestParam(defaultValue = "5") @Min(1) int limit) {
        if (!hasAccessToTenant(tenant)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<Map<String,Object>> items = productService.getStockOutProducts(tenant, limit);
        return ResponseEntity.ok(items);

    }

    @GetMapping("/aov")
    public ResponseEntity<Map<String,Object>> aov(@RequestHeader("X-Tenant-ID") String tenant,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        if (!hasAccessToTenant(tenant)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(analyticsService.aov(tenant, start, end));
    }

    @GetMapping("/upt")
    public ResponseEntity<Map<String,Object>> upt(@RequestHeader("X-Tenant-ID") String tenant,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        if (!hasAccessToTenant(tenant)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(analyticsService.upt(tenant, start, end));
    }

    @GetMapping("/products/top")
    public ResponseEntity<List<Map<String,Object>>> topProducts(@RequestHeader("X-Tenant-ID") String tenant,
                                                                @RequestParam(defaultValue = "revenue") String by,
                                                                @RequestParam(defaultValue = "5") @Min(1) int limit,
                                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        if (!hasAccessToTenant(tenant)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(analyticsService.topProducts(tenant, by, limit, start, end));
    }

    @GetMapping("/customers/new-vs-returning")
    public ResponseEntity<Map<String,Object>> newVsReturning(@RequestHeader("X-Tenant-ID") String tenant,
                                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        if (!hasAccessToTenant(tenant)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(analyticsService.newVsReturning(tenant, start, end));
    }

    @GetMapping("/orders/cancellation-rate")
    public ResponseEntity<Map<String,Object>> cancellationRate(@RequestHeader("X-Tenant-ID") String tenant,
                                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        if (!hasAccessToTenant(tenant)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(analyticsService.cancellationRate(tenant, start, end));
    }

    private boolean hasAccessToTenant(String tenantId) {
        try {
            Integer userId = getCurrentUserId();
            return userTenantAccessService.hasAccessToTenant(userId, tenantId);
        } catch (Exception e) {
            log.warn("Error checking tenant access: {}", e.getMessage());
            return false;
        }
    }

    private Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        if (authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return userPrincipal.getUserId();
        }

        throw new IllegalStateException("Invalid authentication principal");
    }
}
