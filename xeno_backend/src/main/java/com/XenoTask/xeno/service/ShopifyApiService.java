package com.xenotask.xeno.service;

import com.xenotask.xeno.entity.Tenant;
import com.xenotask.xeno.security.CryptoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ShopifyApiService {
    private static final Logger log = LoggerFactory.getLogger(ShopifyApiService.class);
    public static final String SHOPIFY_API_VERSION = "2024-07"; // keep configurable if needed

    private static final Pattern LINK_REL_PATTERN = Pattern.compile("<([^>]+)>; rel=\"(next|previous)\"");

    private final RestTemplate restTemplate;
    private final TenantService tenantService;
    private final ObjectMapper objectMapper;
    private final CryptoService cryptoService;

    public ShopifyApiService(RestTemplate restTemplate, TenantService tenantService, ObjectMapper objectMapper, CryptoService cryptoService) {
        this.restTemplate = restTemplate;
        this.tenantService = tenantService;
        this.objectMapper = objectMapper;
        this.cryptoService = cryptoService;
    }

    private String baseUrl(String shopDomain) {
        return "https://" + shopDomain + "/admin/api/" + SHOPIFY_API_VERSION;
    }

    /** Generic GET (no deprecated page param allowed) */
    public JsonNode get(String tenantId, String endpointPath, Map<String, String> queryParams) {
        if (queryParams != null && queryParams.containsKey("page")) {
            throw new IllegalArgumentException(
                    "'page' parameter is deprecated by Shopify. Use cursor pagination with page_info."
            );
        }

        Tenant tenant = tenantService.getRequiredByTenantId(tenantId);
        URI uri = buildUri(tenant.getShopDomain(), endpointPath, queryParams);
        HttpHeaders headers = buildHeaders(tenant);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                ResponseEntity<String> resp =
                        restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
                logRateLimit(resp.getHeaders());
                return objectMapper.readTree(resp.getBody());
            } catch (HttpStatusCodeException ex) {
                handleRetryOrThrow(ex, attempt);
            } catch (Exception e) {
                handleRetryOrThrow(e, attempt);
            }
        }

        return objectMapper.nullNode(); // should never happen
    }

    private void handleRetryOrThrow(Exception ex, int attempt) {
        if (attempt < 2) {
            backoff(attempt + 1);
        } else if (ex instanceof HttpStatusCodeException httpEx) {
            throw httpEx;
        } else {
            throw new IllegalStateException("Failed calling Shopify API after retries", ex);
        }
    }

    /** Cursor-based page fetch for any collection endpoint (e.g. /customers.json, /orders.json) */
    public PagedResult getCursorPage(String tenantId,
                                     String endpointPath,
                                     Integer limit,
                                     String pageInfo,
                                     Map<String,String> extraParams,
                                     String rootArrayName) {
        Tenant tenant = tenantService.getRequiredByTenantId(tenantId);
        Map<String,String> params = new LinkedHashMap<>();
        if (limit != null) params.put("limit", String.valueOf(limit));
        if (pageInfo != null && !pageInfo.isBlank()) params.put("page_info", pageInfo);
        if (extraParams != null) extraParams.forEach((k,v) -> { if (v != null) params.put(k, v); });

        URI uri = buildUri(tenant.getShopDomain(), endpointPath, params);
        ResponseEntity<String> resp = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(buildHeaders(tenant)), String.class);
        logRateLimit(resp.getHeaders());

        JsonNode body;
        try { body = objectMapper.readTree(resp.getBody()); } catch (Exception e) { throw new IllegalStateException("Parse error", e); }
        JsonNode dataArray = rootArrayName == null ? body : body.path(rootArrayName);
        Map<String,String> cursors = extractPageInfoCursors(resp.getHeaders());
        return new PagedResult(dataArray, cursors.get("next"), cursors.get("previous"));
    }

    /** Fetch all pages for a collection endpoint using cursor pagination. Use cautiously (may be large). */
    public void iterateAll(String tenantId,
                           String endpointPath,
                           Integer limit,
                           Map<String,String> extraParams,
                           String rootArrayName,
                           java.util.function.Consumer<JsonNode> batchConsumer) {
        String next = null;
        do {
            PagedResult page = getCursorPage(tenantId, endpointPath, limit, next, extraParams, rootArrayName);
            if (page.items().isArray()) batchConsumer.accept(page.items());
            next = page.nextPageInfo();
        } while (next != null);
    }

    private Map<String,String> extractPageInfoCursors(HttpHeaders headers) {
        String link = headers.getFirst("Link");
        Map<String,String> map = new HashMap<>();
        if (link == null) return map;
        Matcher m = LINK_REL_PATTERN.matcher(link);
        while (m.find()) {
            String url = m.group(1);
            String rel = m.group(2);
            String pageInfo = extractQueryParam(url, "page_info");
            if (pageInfo != null) map.put(rel, pageInfo);
        }
        return map;
    }

    private String extractQueryParam(String url, String key) {
        int q = url.indexOf('?');
        if (q < 0) return null;
        String[] parts = url.substring(q + 1).split("&");
        for (String p : parts) {
            int eq = p.indexOf('=');
            if (eq > 0) {
                String k = p.substring(0, eq);
                String v = p.substring(eq + 1);
                if (k.equals(key)) return v;
            }
        }
        return null;
    }

    private URI buildUri(String shopDomain, String path, Map<String,String> params) {
        UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(baseUrl(shopDomain) + path);
        if (params != null) params.forEach(b::queryParam);
        return b.build().toUri();
    }

    private HttpHeaders buildHeaders(Tenant tenant) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        String token = resolveAccessToken(tenant.getAccessToken());
        headers.set("X-Shopify-Access-Token", token);
        return headers;
    }

    private String resolveAccessToken(String stored) {
        if (stored == null) return null;
        // New format: iv:ct (Base64:Base64). Try to decrypt; if it fails, assume plaintext for backward compatibility.
        if (stored.contains(":")) {
            try {
                return cryptoService.decrypt(stored);
            } catch (RuntimeException e) {
                log.warn("Decrypt failed; falling back to raw token (check key/config)");
                return stored;
            }
        }
        return stored;
    }

    private void backoff(int attempt) {
        try { Thread.sleep(Duration.ofSeconds(attempt * 2L).toMillis()); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private void logRateLimit(HttpHeaders headers) {
        String limit = headers.getFirst("X-Shopify-Shop-Api-Call-Limit");
        if (limit != null) log.debug("Shopify call limit: {}", limit);
    }

    /** Record representing one cursor page */
    public record PagedResult(JsonNode items, String nextPageInfo, String previousPageInfo) {}
}
