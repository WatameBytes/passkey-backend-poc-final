package com.vtd.backend.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class CacheService {
    private final Cache<String, String> cache;

    public CacheService() {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES) // Auto-expire entries after 5 minutes
                .maximumSize(1000) // Set maximum number of entries
                .build();
    }

    public void save(String key, String value) {
        cache.put(key, value);
    }

    public String retrieve(String key) {
        return cache.getIfPresent(key); // Returns null if the key is not present
    }
}
