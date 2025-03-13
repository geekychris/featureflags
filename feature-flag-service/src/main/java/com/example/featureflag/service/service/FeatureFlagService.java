package com.example.featureflag.service.service;

import com.example.featureflag.service.dto.FeatureFlagDto;
import com.example.featureflag.service.model.FeatureFlag;
import com.example.featureflag.service.model.FeatureFlagId;
import com.example.featureflag.service.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;
    private final MemcachedService memcachedService;

    /**
     * Get all feature flags for a specific entity
     * 
     * @param entityId the entity identifier
     * @return list of feature flag DTOs
     */
    public List<FeatureFlagDto> getAllFeatureFlagsByEntityId(String entityId) {
        log.info("Getting all feature flags for entity: {}", entityId);
        return featureFlagRepository.findAllByIdEntityId(entityId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific feature flag by entity ID and flag name
     * 
     * @param entityId the entity identifier
     * @param name the feature flag name
     * @return the feature flag DTO
     * @throws NoSuchElementException if the feature flag is not found
     */
    public FeatureFlagDto getFeatureFlag(String entityId, String name) {
        log.info("Getting feature flag: {} for entity: {}", name, entityId);
        return featureFlagRepository.findByIdEntityIdAndIdName(entityId, name)
                .map(this::convertToDto)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("Feature flag '%s' not found for entity '%s'", name, entityId)));
    }

    /**
     * Create or update a feature flag
     * 
     * @param entityId the entity identifier
     * @param name the feature flag name
     * @param value the feature flag data
     * @return the updated/created feature flag DTO
     */
    @Transactional
    public FeatureFlagDto createOrUpdateFeatureFlag(String entityId, String name, String value) {
        log.info("Creating/updating feature flag: {} for entity: {}", name, entityId);
        
        FeatureFlag featureFlag = featureFlagRepository
                .findByIdEntityIdAndIdName(entityId, name)
                .orElse(new FeatureFlag());
        featureFlag.setId(new FeatureFlagId());
        featureFlag.getId().setEntityId(entityId);
        featureFlag.getId().setName(name);
        featureFlag.setValue(value);
        
        LocalDateTime now = LocalDateTime.now();
        if (featureFlag.getCreatedAt() == null) {
            featureFlag.setCreatedAt(now);
        }
        featureFlag.setUpdatedAt(now);
        
        FeatureFlag savedFlag = featureFlagRepository.save(featureFlag);
        log.info("Feature flag saved successfully: {} for entity: {}", name, entityId);
        
        return convertToDto(savedFlag);
    }

    /**
     * Delete a feature flag
     * 
     * @param entityId the entity identifier
     * @param name the feature flag name
     * @throws NoSuchElementException if the feature flag is not found
     */
    @Transactional
    public void deleteFeatureFlag(String entityId, String name) {
        log.info("Deleting feature flag: {} for entity: {}", name, entityId);
        
        if (!featureFlagRepository.existsByIdEntityIdAndIdName(entityId, name)) {
            throw new NoSuchElementException(
                    String.format("Feature flag '%s' not found for entity '%s'", name, entityId));
        }
        
        featureFlagRepository.deleteByIdEntityIdAndIdName(entityId, name);
        log.info("Feature flag deleted successfully: {} for entity: {}", name, entityId);
    }

    /**
     * Publish all feature flags for a specific entity to Memcached
     * 
     * @param entityId the entity identifier
     * @return true if successful, false otherwise
     */
    public boolean publishFeatureFlagsToMemcached(String entityId) {
        log.info("Publishing feature flags to Memcached for entity: {}", entityId);
        List<FeatureFlag> featureFlags = featureFlagRepository.findAllByIdEntityId(entityId);
        
        if (featureFlags.isEmpty()) {
            log.warn("No feature flags found for entity: {}", entityId);
            return false;
        }
        
        boolean result = memcachedService.storeFeatureFlags(entityId, featureFlags);
        log.info("Published {} feature flags to Memcached for entity: {}, result: {}", 
                featureFlags.size(), entityId, result);
        
        return result;
    }

    /**
     * Publish all feature flags to Memcached
     * 
     * @return number of entity sets published
     */
    /*public int publishAllFeatureFlagsToMemcached() {
        log.info("Publishing all feature flags to Memcached");
        
        // Get all distinct entity IDs
        List<String> entityIds = featureFlagRepository.findDistinctIdEntityIds();
        int publishedCount = 0;
        
        for (String entityId : entityIds) {
            boolean published = publishFeatureFlagsToMemcached(entityId);
            if (published) {
                publishedCount++;
            }
        }
        
        log.info("Published feature flags for {} entities to Memcached", publishedCount);
        return publishedCount;
    }

     */

    /**
     * Convert entity to DTO
     */
    private FeatureFlagDto convertToDto(FeatureFlag featureFlag) {
        return FeatureFlagDto.builder()
                .entityId(featureFlag.getId().getEntityId())
                .name(featureFlag.getId().getName())
                .value(featureFlag.getValue())
                .createdAt(featureFlag.getCreatedAt())
                .updatedAt(featureFlag.getUpdatedAt())
                .build();
    }
}

