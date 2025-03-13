package com.example.featureflag.client;

import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Client for retrieving feature flags from Memcached.
 * Supports retrieving and merging multiple feature flag sets.
 */
public class FeatureFlagClient {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagClient.class);
    private static final String CACHE_KEY_PREFIX = "feature_flags:";
    private final MemcachedClient memcachedClient;
    private final int timeoutMillis;

    public FeatureFlagClient(MemcachedClient memcachedClient, int timeoutMillis) {
        this.memcachedClient = memcachedClient;
        //XXX some kind of timeout passed in autowiring
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Retrieves feature flags for a single entity ID from Memcached.
     *
     * @param entityId the entity ID to fetch feature flags for
     * @return a map of feature flag names to values
     */
    public Map<String, String> getFeatureFlags(String entityId) {
        if (entityId == null || entityId.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity ID cannot be null or empty");
        }

        String cacheKey = generateCacheKey(entityId);
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> featureFlags = (Map<String, String>) getFromMemcached(cacheKey);
            if (featureFlags == null) {
                log.warn("No feature flags found in cache for entity: {}", entityId);
                return Collections.emptyMap();
            }
            return Collections.unmodifiableMap(featureFlags);
        } catch (Exception e) {
            log.error("Error retrieving feature flags for entity {}: {}", entityId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Retrieves and merges feature flags for multiple entity IDs.
     * Later entities in the list will override flags from earlier entities when there are conflicts.
     *
     * @param entityIds list of entity IDs to fetch and merge feature flags for
     * @return a merged map of feature flag names to values
     */
    public Map<String, String> getMergedFeatureFlags(List<String> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            throw new IllegalArgumentException("Entity ID list cannot be null or empty");
        }

        Map<String, String> mergedFlags = new HashMap<>();
        
        // Process entities in order, allowing later entities to override earlier ones
        for (String entityId : entityIds) {
            Map<String, String> entityFlags = getFeatureFlags(entityId);
            mergedFlags.putAll(entityFlags);
        }

        return Collections.unmodifiableMap(mergedFlags);
    }

    /**
     * Checks if a specific feature flag is enabled for an entity.
     * A feature is considered enabled if it exists and its value is "true" (case-insensitive).
     *
     * @param entityId the entity ID to check
     * @param featureName the name of the feature flag to check
     * @param defaultValue the default value if the feature flag is not found
     * @return true if the feature is enabled, false otherwise
     */
    public boolean isFeatureEnabled(String entityId, String featureName, boolean defaultValue) {
        Map<String, String> featureFlags = getFeatureFlags(entityId);
        
        if (!featureFlags.containsKey(featureName)) {
            return defaultValue;
        }
        
        String value = featureFlags.get(featureName);
        return Boolean.parseBoolean(value);
    }

    /**
     * Checks if a specific feature flag is enabled across multiple entities.
     * Entities are checked in reverse order, so later entities take precedence.
     *
     * @param entityIds the list of entity IDs to check
     * @param featureName the name of the feature flag to check
     * @param defaultValue the default value if the feature flag is not found
     * @return true if the feature is enabled, false otherwise
     */
    public boolean isFeatureEnabled(List<String> entityIds, String featureName, boolean defaultValue) {
        if (entityIds == null || entityIds.isEmpty()) {
            return defaultValue;
        }

        // Create a new list to avoid modifying the input list
        List<String> reversedEntityIds = new ArrayList<>(entityIds);
        Collections.reverse(reversedEntityIds);
        
        // Check entities in reverse order (highest priority first)
        for (String entityId : reversedEntityIds) {
            Map<String, String> featureFlags = getFeatureFlags(entityId);
            
            if (featureFlags.containsKey(featureName)) {
                String value = featureFlags.get(featureName);
                return Boolean.parseBoolean(value);
            }
        }
        
        return defaultValue;
    }

    /**
     * Gets a feature flag value for an entity.
     *
     * @param entityId the entity ID to check
     * @param featureName the name of the feature flag to retrieve
     * @param defaultValue the default value if the feature flag is not found
     * @return the feature flag value or default value if not found
     */
    public String getFeatureValue(String entityId, String featureName, String defaultValue) {
        Map<String, String> featureFlags = getFeatureFlags(entityId);
        return featureFlags.getOrDefault(featureName, defaultValue);
    }

    /**
     * Gets a feature flag value across multiple entities.
     * Entities are checked in reverse order, so later entities take precedence.
     *
     * @param entityIds the list of entity IDs to check
     * @param featureName the name of the feature flag to retrieve
     * @param defaultValue the default value if the feature flag is not found
     * @return the feature flag value or default value if not found
     */
    public String getFeatureValue(List<String> entityIds, String featureName, String defaultValue) {
        if (entityIds == null || entityIds.isEmpty()) {
            return defaultValue;
        }

        // Create a new list to avoid modifying the input list
        List<String> reversedEntityIds = new ArrayList<>(entityIds);
        Collections.reverse(reversedEntityIds);
        
        // Check entities in reverse order (highest priority first)
        for (String entityId : reversedEntityIds) {
            Map<String, String> featureFlags = getFeatureFlags(entityId);
            
            if (featureFlags.containsKey(featureName)) {
                return featureFlags.get(featureName);
            }
        }
        
        return defaultValue;
    }

    /**
     * Generates a cache key for an entity ID.
     */
    private String generateCacheKey(String entityId) {
        return CACHE_KEY_PREFIX + entityId;
    }

    /**
     * Retrieves a value from Memcached with timeout handling.
     */
    private Object getFromMemcached(String key) {
        try {
            Future<Object> future = memcachedClient.asyncGet(key);
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while fetching from Memcached: {}", e.getMessage());
            return null;
        } catch (ExecutionException e) {
            log.error("Error executing Memcached get: {}", e.getMessage());
            return null;
        } catch (TimeoutException e) {
            log.error("Timeout while fetching from Memcached: {}", e.getMessage());
            return null;
        }
    }
}

