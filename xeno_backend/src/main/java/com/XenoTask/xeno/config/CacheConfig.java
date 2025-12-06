package com.xenotask.xeno.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.ttl.default:PT2M}")
    private Duration defaultTtl;
    @Value("${cache.ttl.revenue.daily:PT2M}")
    private Duration revenueDailyTtl;
    @Value("${cache.ttl.revenue.range:PT2M}")
    private Duration revenueRangeTtl;
    @Value("${cache.ttl.orders.statusBreakdown:PT2M}")
    private Duration ordersStatusBreakdownTtl;
    @Value("${cache.ttl.customers.top:PT2M}")
    private Duration customersTopTtl;

    @Bean
    public CacheManager cacheManager(LettuceConnectionFactory redisConnectionFactory) {
        // default TTL
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultTtl);

        // custom TTL per cache
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("revenue:daily", RedisCacheConfiguration.defaultCacheConfig().entryTtl(revenueDailyTtl));
        cacheConfigs.put("revenue:range", RedisCacheConfiguration.defaultCacheConfig().entryTtl(revenueRangeTtl));
        cacheConfigs.put("orders:statusBreakdown", RedisCacheConfiguration.defaultCacheConfig().entryTtl(ordersStatusBreakdownTtl));
        cacheConfigs.put("customers:top", RedisCacheConfiguration.defaultCacheConfig().entryTtl(customersTopTtl));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
