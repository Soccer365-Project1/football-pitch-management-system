package com.fpms.config;

import java.time.Duration;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableCaching
public class CatalogCacheConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
            @Value("${app.cache.catalog-ttl:PT15M}") Duration ttl) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                        .prefixCacheNameWith("fpms:catalog:v1:")
                        .entryTtl(ttl)
                        .disableCachingNullValues())
                .transactionAware()
                .build();
    }

    @Bean
    public KeyGenerator catalogKeyGenerator() {
        // Include the method to separate list/detail/public DTOs in the same cache.
        return (target, method, params) -> method.getName() + ":" + Arrays.deepToString(params);
    }
}
