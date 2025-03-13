package com.example.featureflag.service.service;

import com.example.featureflag.service.config.MemcachedConfig;
import com.example.featureflag.service.model.FeatureFlag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.MemcachedClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemcachedService {
    
    private final MemcachedClient memcachedClient;
    private final MemcachedConfig memcachedConfig;
    
    /**
     * Generate a cache key for feature flags by entityId
     * 
     * @param entityId the entity ID
     * @return the cache key
     */
    private String generateFeatureFlagKey(String entityId) {
        return "feature_flags:" + entityId;
    }
    
    /**
     * Store a map of feature flags for an entity in Memcached
     * 
     * @param entityId the entity ID
     * @param featureFlags the list of feature flags
     */
    public boolean storeFeatureFlags(String entityId, List<FeatureFlag> featureFlags) {
        String key = generateFeatureFlagKey(entityId);
        Map<String, String> featureFlagMap = convertToMap(featureFlags);
        
        try {
            memcachedClient.set(key, memcachedConfig.getExpirationTime(), featureFlagMap);
            log.info("Stored {} feature flags for entity {} in Memcached", featureFlags.size(), entityId);
            return true;
        } catch (Exception e) {
            log.error("Failed to store feature flags in Memcached for entity {}: {}", entityId, e.getMessage(), e);
        }
        return false;
    }
    
    /**
     * Store multiple sets of feature flags in Memcached
     * 
     * @param entityFeatureFlags a map of entity IDs to their feature flags
     */
    public void storeMultipleFeatureFlags(Map<String, List<FeatureFlag>> entityFeatureFlags) {
        entityFeatureFlags.forEach(this::storeFeatureFlags);
        log.info("Stored feature flags for {} entities in Memcached", entityFeatureFlags.size());
    }

    public void deleteFeatureFlags(String entityId) {
        String key = generateFeatureFlagKey(entityId);
        try {
            memcachedClient.delete(key);
        }
        catch (Exception e) {
            log.error("Failed to delete feature flags in Memcached for entity {}: {}", entityId, e.getMessage(), e);
        }
    }

    /**
     * Retrieve feature flags for an entity from Memcached
     * 
     * @param entityId the entity ID
     * @return a map of feature flag names to values, or empty map if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> retrieveFeatureFlags(String entityId) {
        String key = generateFeatureFlagKey(entityId);
        
        try {
            Object result = memcachedClient.get(key);
            if (result != null && result instanceof Map) {
                Map<String, String> flags = (Map<String, String>) result;
                log.info("Retrieved {} feature flags for entity {} from Memcached", flags.size(), entityId);
                return flags;
            }
        } catch (Exception e) {
            log.error("Failed to retrieve feature flags from Memcached for entity {}: {}", entityId, e.getMessage(), e);
        }
        
        return new HashMap<>();
    }
    
    /**
     * Convert a list of FeatureFlag entities to a map of name-value pairs
     * 
     * @param featureFlags the list of feature flags
     * @return a map of feature flag names to values
     */
    private Map<String, String> convertToMap(List<FeatureFlag> featureFlags) {
        Map<String, String> featureFlagMap = new HashMap<>();
        for (FeatureFlag flag : featureFlags) {
            featureFlagMap.put(flag.getId().getName(), flag.getValue());
        }
        return featureFlagMap;
    }
}

