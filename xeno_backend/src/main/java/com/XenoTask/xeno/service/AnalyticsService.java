package com.xenotask.xeno.service;

import com.xenotask.xeno.repository.OrderLineItemRepository;
import com.xenotask.xeno.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final TenantService tenantService;
    private final OrderLineItemRepository orderLineItemRepository;


    @Cacheable(value = "revenue:range", key = "#tenantHeader + ':' + #start + ':' + #end")
    public BigDecimal revenueInRange(String tenantHeader, LocalDateTime start, LocalDateTime end) {
        String tenantId = tenantService.resolveTenantIdOrDomain();
        return orderRepository.sumRevenueInRange(tenantId, start, end);
    }

    @Cacheable(value = "revenue:daily", key = "#tenantHeader + ':' + #startDate + ':' + #endDate")
    public List<Map<String,Object>> dailyRevenue(String tenantHeader, LocalDate startDate, LocalDate endDate) {
        String tenantId = tenantService.resolveTenantIdOrDomain();
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusSeconds(1);
        return orderRepository.dailyRevenueSeries(tenantId, start, end).stream()
                .map(row -> Map.of(
                        "date", row[0].toString(),
                        "revenue", row[1]))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "orders:statusBreakdown", key = "#tenantHeader")
    public List<Map<String,Object>> statusBreakdown(String tenantHeader) {
        String tenantId = tenantService.resolveTenantIdOrDomain();
        return orderRepository.countByFinancialStatus(tenantId).stream()
                .map(r -> Map.of("status", r[0], "count", r[1]))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "customers:top", key = "#tenantHeader + ':' + #limit")
    public List<Map<String,Object>> topCustomers(String tenantHeader, int limit) {
        String tenantId = tenantService.resolveTenantIdOrDomain();
        return orderRepository.topCustomersByOrders(tenantId, PageRequest.of(0, limit)).stream()
                .map(r -> Map.of(
                        "customerId", r[0],
                        "firstName", r[1],
                        "lastName", r[2],
                        "totalSpent", r[3]))
                .collect(Collectors.toList());
    }

    // AOV (Average Order Value) and related totals
    @Cacheable(value = "analytics:aov", key = "#tenantHeader + ':' + #start + ':' + #end")
    public Map<String,Object> aov(String tenantHeader, LocalDateTime start, LocalDateTime end) {
        String tenantId = tenantService.resolveTenantIdOrDomain();
        List<Object[]> rows = orderRepository.aovStats(tenantId, start, end);
        BigDecimal revenue = BigDecimal.ZERO;
        long orders = 0L;
        BigDecimal discounts = BigDecimal.ZERO;
        if (!rows.isEmpty()) {
            Object[] r = rows.get(0);
            revenue = (BigDecimal) r[0];
            orders = ((Number) r[1]).longValue();
            discounts = (BigDecimal) r[2];
        }
        BigDecimal aov = orders == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);
        Map<String,Object> resp = new HashMap<>();
        resp.put("aov", aov);
        resp.put("orders", orders);
        resp.put("revenue", revenue);
        resp.put("discounts", discounts);
        return resp;
    }

    // UPT (Units Per Transaction)
    @Cacheable(value = "analytics:upt", key = "#tenantHeader + ':' + #start + ':' + #end")
    public Map<String,Object> upt(String tenantHeader, LocalDateTime start, LocalDateTime end) {
        String tenantId = tenantService.resolveTenantIdOrDomain();
        List<Object[]> rows = orderLineItemRepository.unitsAndOrdersInRange(tenantId, start, end);
        long units = 0L;
        long orders = 0L;
        if (!rows.isEmpty()) {
            Object[] r = rows.get(0);
            units = ((Number) r[0]).longValue();
            orders = ((Number) r[1]).longValue();
        }
        BigDecimal upt = orders == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(units).divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);
        Map<String,Object> resp = new HashMap<>();
        resp.put("upt", upt);
        resp.put("units", units);
        resp.put("orders", orders);
        return resp;
    }

    // Top products by revenue or quantity
    @Cacheable(value = "analytics:topProducts", key = "#tenantHeader + ':' + #by + ':' + #limit + ':' + #start + ':' + #end")
    public List<Map<String,Object>> topProducts(String tenantHeader, String by, int limit, LocalDateTime start, LocalDateTime end) {
        String tenantId = tenantService.resolveTenantIdOrDomain();
        PageRequest pageable = PageRequest.of(0, Math.max(1, limit));
        List<Object[]> rows;
        if ("quantity".equalsIgnoreCase(by)) {
            rows = orderLineItemRepository.topProductsByQuantity(tenantId, start, end, pageable);
        } else {
            rows = orderLineItemRepository.topProductsByRevenue(tenantId, start, end, pageable);
        }
        List<Map<String,Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String,Object> m = new HashMap<>();
            m.put("productId", r[0]);
            m.put("title", r[1]);
            m.put("qty", r[2]);
            m.put("revenue", r[3]);
            result.add(m);
        }
        return result;
    }

    // New vs Returning customers in range
    @Cacheable(value = "analytics:newVsReturning", key = "#tenantHeader + ':' + #start + ':' + #end")
    public Map<String,Object> newVsReturning(String tenantHeader, LocalDateTime start, LocalDateTime end) {
        String tenantId = tenantService.resolveTenantIdOrDomain();
        List<Object[]> rows = orderRepository.ordersPerCustomerInRange(tenantId, start, end);
        long newbies = 0L;
        long returning = 0L;
        for (Object[] r : rows) {
            long cnt = ((Number) r[1]).longValue();
            if (cnt == 1L) newbies++;
            else if (cnt > 1L) returning++;
        }
        return Map.of("new", newbies, "returning", returning);
    }

    // Cancellation rate in range
    @Cacheable(value = "analytics:cancellationRate", key = "#tenantHeader + ':' + #start + ':' + #end")
    public Map<String,Object> cancellationRate(String tenantHeader, LocalDateTime start, LocalDateTime end) {
        String tenantId = tenantService.resolveTenantIdOrDomain();
        long total = orderRepository.countOrdersInRange(tenantId, start, end);
        long cancelled = orderRepository.countCancelledInRange(tenantId, start, end);
        BigDecimal rate = total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(cancelled)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
        return Map.of("cancelled", cancelled, "total", total, "rate", rate);
    }
}
