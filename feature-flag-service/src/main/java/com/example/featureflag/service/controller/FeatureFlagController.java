package com.example.featureflag.service.controller;

import com.example.featureflag.service.dto.FeatureFlagDto;
import com.example.featureflag.service.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/featureflags")
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    /**
     * Get all feature flags for an entity
     * 
     * @param entityId the entity identifier
     * @return list of feature flags
     */
    @GetMapping("/{entityId}")
    public ResponseEntity<List<FeatureFlagDto>> getAllFeatureFlags(@PathVariable String entityId) {
        log.info("REST request to get all feature flags for entity: {}", entityId);
        List<FeatureFlagDto> flags = featureFlagService.getAllFeatureFlagsByEntityId(entityId);
        return ResponseEntity.ok(flags);
    }

    /**
     * Get a specific feature flag
     * 
     * @param entityId the entity identifier
     * @param name the feature flag name
     * @return the feature flag
     */
    @GetMapping("/{entityId}/{name}")
    public ResponseEntity<FeatureFlagDto> getFeatureFlag(
            @PathVariable String entityId,
            @PathVariable String name) {
        log.info("REST request to get feature flag: {} for entity: {}", name, entityId);
        FeatureFlagDto flag = featureFlagService.getFeatureFlag(entityId, name);
        return ResponseEntity.ok(flag);
    }

    /**
     * Create or update a feature flag
     * 
     * @param entityId the entity identifier
     * @param name the feature flag name
     * @param value of the feature flag data
     * @return the created/updated feature flag
     */
    @PostMapping("/{entityId}/{name}/{value}")
    public ResponseEntity<FeatureFlagDto> createOrUpdateFeatureFlag(
            @PathVariable String entityId,
            @PathVariable String name,
            @PathVariable String value) {
        log.info("REST request to create/update feature flag: {} for entity: {}", name, entityId);
        FeatureFlagDto result = featureFlagService.createOrUpdateFeatureFlag(entityId, name, value);
        // updates to cause a refresh in memcache!
        featureFlagService.publishFeatureFlagsToMemcached(entityId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Delete a feature flag
     * 
     * @param entityId the entity identifier
     * @param name the feature flag name
     * @return no content if successful
     */
    @DeleteMapping("/{entityId}/{name}")
    public ResponseEntity<Void> deleteFeatureFlag(
            @PathVariable String entityId,
            @PathVariable String name) {
        log.info("REST request to delete feature flag: {} for entity: {}", name, entityId);
        featureFlagService.deleteFeatureFlag(entityId, name);
        // updates to cause a refresh in memcache!
        featureFlagService.publishFeatureFlagsToMemcached(entityId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Publish feature flags to Memcached for an entity
     * 
     * @param entityId the entity identifier
     * @return success/failure message
     */
    @PostMapping("/publish/{entityId}")
    public ResponseEntity<Map<String, Object>> publishFeatureFlags(@PathVariable String entityId) {
        log.info("REST request to publish feature flags to Memcached for entity: {}", entityId);
        boolean result = featureFlagService.publishFeatureFlagsToMemcached(entityId);
        
        if (result) {
            return ResponseEntity.ok(
                    Map.of("status", "success", 
                           "message", "Feature flags published to Memcached for entity: " + entityId));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("status", "error", 
                           "message", "Failed to publish feature flags to Memcached for entity: " + entityId));
        }
    }

    /**
     * Publish all feature flags to Memcached
     * 
     * @return success/failure message with count
     */
    /*@PostMapping("/publish/all")
    public ResponseEntity<Map<String, Object>> publishAllFeatureFlags() {
        log.info("REST request to publish all feature flags to Memcached");
        int count = featureFlagService.publishAllFeatureFlagsToMemcached();
        
        return ResponseEntity.ok(
                Map.of("status", "success", 
                       "message", "Published feature flags for " + count + " entities to Memcached",
                       "count", count));
    }
    */

}

