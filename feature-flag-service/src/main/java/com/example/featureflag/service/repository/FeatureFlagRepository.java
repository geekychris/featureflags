package com.example.featureflag.service.repository;

import com.example.featureflag.service.model.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {

    boolean existsByIdEntityIdAndIdName(String entityId, String name);
    /**
     * Find all feature flags by entityId
     * 
     * @param entityId the ID of the entity
     * @return a list of feature flags associated with the entity
     */
    List<FeatureFlag> findAllByIdEntityId(String entityId);


    /**
     * Find a specific feature flag by entityId and name
     * 
     * @param entityId the ID of the entity
     * @param name the name of the feature flag
     * @return an Optional containing the feature flag if found
     */
    Optional<FeatureFlag> findByIdEntityIdAndIdName(String entityId, String name);

    /**
     * Delete a feature flag by entityId and name
     * 
     * @param entityId the ID of the entity
     * @param name the name of the feature flag
     */
    void deleteByIdEntityIdAndIdName(String entityId, String name);
}

