package com.example.featureflag.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Feature Flags
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlagDto implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * The entity identifier associated with this feature flag
     */
    private String entityId;
    
    /**
     * The name of the feature flag
     */
    private String name;
    
    /**
     * The value of the feature flag
     */
    private String value;
    
    /**
     * The creation timestamp
     */
    private LocalDateTime createdAt;
    
    /**
     * The last update timestamp
     */
    private LocalDateTime updatedAt;
}

