package com.xenotask.xeno.service;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CacheService {
    private final CacheManager cacheManager;

    public CacheService(CacheManager cacheManager) { this.cacheManager = cacheManager; }

    public void evictCache(String name) {
        if (cacheManager.getCache(name) != null) Objects.requireNonNull(cacheManager.getCache(name)).clear();
    }

    public void evictAll() {
        cacheManager.getCacheNames().forEach(n -> {
            if (cacheManager.getCache(n) != null) Objects.requireNonNull(cacheManager.getCache(n)).clear();
        });
    }
}

