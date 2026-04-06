package com.mcp.qdrant.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Cache configuration for document listing and metadata caching.
 * Supports per-collection cache configuration with different TTL and size settings.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);
    
    public static final String DOCUMENT_LIST_CACHE_PREFIX = "documentListCache";
    public static final String DOCUMENT_INFO_CACHE_PREFIX = "documentInfoCache";
    
    // Default cache settings
    private static final long DEFAULT_TTL_MINUTES = 5;
    private static final long DEFAULT_MAX_SIZE = 1000;
    
    // Per-collection cache configurations: collectionName -> CacheSettings
    private final Map<String, CacheSettings> collectionCacheConfigs = new ConcurrentHashMap<>();
    
    // Store cache manager for dynamic reconfiguration
    private CaffeineCacheManager cacheManager;
    // Track custom cache names that have been registered
    private final Set<String> registeredCacheNames = ConcurrentHashMap.newKeySet();

    /**
     * Cache settings for a collection
     */
    public static class CacheSettings {
        private long ttlMinutes = DEFAULT_TTL_MINUTES;
        private long maxSize = DEFAULT_MAX_SIZE;
        
        public CacheSettings() {}
        
        public CacheSettings(long ttlMinutes, long maxSize) {
            this.ttlMinutes = ttlMinutes;
            this.maxSize = maxSize;
        }
        
        public long getTtlMinutes() { return ttlMinutes; }
        public void setTtlMinutes(long ttlMinutes) { this.ttlMinutes = ttlMinutes; }
        public long getMaxSize() { return maxSize; }
        public void setMaxSize(long maxSize) { this.maxSize = maxSize; }
    }

    @Bean
    public CacheManager cacheManager() {
        cacheManager = new CaffeineCacheManager();
        
        // Configure default cache
        cacheManager.setCaffeine(createCaffeineConfig(DEFAULT_TTL_MINUTES, DEFAULT_MAX_SIZE));
        
        // Register all known cache names (will be created dynamically as needed)
        List<String> cacheNames = new ArrayList<>();
        cacheNames.add(DOCUMENT_LIST_CACHE_PREFIX);
        cacheNames.add(DOCUMENT_INFO_CACHE_PREFIX);
        cacheManager.setCacheNames(cacheNames);
        registeredCacheNames.addAll(cacheNames);
        
        return cacheManager;
    }
    
    /**
     * Get cache name for document list cache for a specific collection
     */
    public static String getDocumentListCacheName(String collectionName) {
        if (collectionName == null || collectionName.isEmpty()) {
            return DOCUMENT_LIST_CACHE_PREFIX;
        }
        return DOCUMENT_LIST_CACHE_PREFIX + "-" + collectionName;
    }
    
    /**
     * Get cache name for document info cache for a specific collection
     */
    public static String getDocumentInfoCacheName(String collectionName) {
        if (collectionName == null || collectionName.isEmpty()) {
            return DOCUMENT_INFO_CACHE_PREFIX;
        }
        return DOCUMENT_INFO_CACHE_PREFIX + "-" + collectionName;
    }
    
    /**
     * Configure cache settings for a specific collection
     */
    public void configureCollectionCache(String collectionName, long ttlMinutes, long maxSize) {
        collectionCacheConfigs.put(collectionName, new CacheSettings(ttlMinutes, maxSize));
    }
    
    /**
     * Get cache settings for a collection (or default if not configured)
     */
    public CacheSettings getCacheSettings(String collectionName) {
        return collectionCacheConfigs.getOrDefault(collectionName, 
                new CacheSettings(DEFAULT_TTL_MINUTES, DEFAULT_MAX_SIZE));
    }
    
    /**
     * Get the configured cache size (max entries) for a collection.
     * Returns the configured maxSize or DEFAULT_MAX_SIZE if not configured.
     */
    public long getCacheSizeForCollection(String collectionName) {
        CacheSettings settings = collectionCacheConfigs.get(collectionName);
        if (settings != null) {
            return settings.getMaxSize();
        }
        return DEFAULT_MAX_SIZE;
    }
    
    /**
     * Update cache size for a collection based on document count.
     * Size is calculated as max(1000, totalDocuments).
     * Clears existing cache entries.
     */
    public void updateCacheSizeForCollection(String collectionName, int totalDocuments) {
        long newSize = Math.max(DEFAULT_MAX_SIZE, totalDocuments);
        long ttlMinutes = DEFAULT_TTL_MINUTES;
        
        // Update configuration
        CacheSettings settings = new CacheSettings(ttlMinutes, newSize);
        collectionCacheConfigs.put(collectionName, settings);
        
        String listCacheName = getDocumentListCacheName(collectionName);
        String infoCacheName = getDocumentInfoCacheName(collectionName);
        
        // Clear existing caches if they exist
        clearCache(listCacheName);
        clearCache(infoCacheName);
        
        // Create new cache with updated configuration
        Caffeine<Object, Object> caffeine = createCaffeineConfig(ttlMinutes, newSize);
        
        // Register the new cache configuration
        cacheManager.registerCustomCache(listCacheName, caffeine.build());
        cacheManager.registerCustomCache(infoCacheName, caffeine.build());
        registeredCacheNames.add(listCacheName);
        registeredCacheNames.add(infoCacheName);
    }
    
    /**
     * Clear all cache entries for a specific cache name
     */
    private void clearCache(String cacheName) {
        if (registeredCacheNames.contains(cacheName)) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    /**
     * Delete all cache entries for a specific collection.
     * This removes the cache configuration and clears all cached data.
     */
    public void deleteCollectionCache(String collectionName) {
        String listCacheName = getDocumentListCacheName(collectionName);
        String infoCacheName = getDocumentInfoCacheName(collectionName);
        
        // Clear and remove the caches if they exist
        clearCache(listCacheName);
        clearCache(infoCacheName);
        
        // Remove from registered cache names
        registeredCacheNames.remove(listCacheName);
        registeredCacheNames.remove(infoCacheName);
        
        // Remove from collection cache configs
        collectionCacheConfigs.remove(collectionName);
        
        log.info("Deleted cache for collection: {}", collectionName);
    }
    
    /**
     * Initialize cache for a new collection with default settings (size 1000).
     */
    public void initializeCollectionCache(String collectionName) {
        String listCacheName = getDocumentListCacheName(collectionName);
        String infoCacheName = getDocumentInfoCacheName(collectionName);
        
        // Create cache with default configuration (size 1000)
        Caffeine<Object, Object> caffeine = createCaffeineConfig(DEFAULT_TTL_MINUTES, DEFAULT_MAX_SIZE);
        
        // Register the caches
        cacheManager.registerCustomCache(listCacheName, caffeine.build());
        cacheManager.registerCustomCache(infoCacheName, caffeine.build());
        registeredCacheNames.add(listCacheName);
        registeredCacheNames.add(infoCacheName);
        
        // Store default configuration
        collectionCacheConfigs.put(collectionName, new CacheSettings(DEFAULT_TTL_MINUTES, DEFAULT_MAX_SIZE));
        
        log.info("Initialized cache for collection: {} with default size: {}", collectionName, DEFAULT_MAX_SIZE);
    }

    private Caffeine<Object, Object> createCaffeineConfig(long ttlMinutes, long maxSize) {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .recordStats();
    }
}
