package com.example.featureflag.service.service;

import com.example.featureflag.service.dto.FeatureFlagDto;
import com.example.featureflag.service.model.FeatureFlag;
import com.example.featureflag.service.repository.FeatureFlagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {
/*
    @Mock
    private FeatureFlagRepository featureFlagRepository;

    @Mock
    private MemcachedService memcachedService;

    @InjectMocks
    private FeatureFlagService featureFlagService;

    private final String entityId = "user123";
    private final String flagName = "dark-mode";
    private final String flagValue = "true";
    private FeatureFlag featureFlag;
    private List<FeatureFlag> featureFlags;

    @BeforeEach
    void setUp() {
        featureFlag = new FeatureFlag();
        featureFlag.setEntityId(entityId);
        featureFlag.setName(flagName);
        featureFlag.setValue(flagValue);

        FeatureFlag featureFlag2 = new FeatureFlag();
        featureFlag2.setEntityId(entityId);
        featureFlag2.setName("premium-user");
        featureFlag2.setValue("false");

        featureFlags = Arrays.asList(featureFlag, featureFlag2);
    }

    @Test
    @DisplayName("Should get all feature flags by entityId")
    void shouldGetAllFeatureFlagsByEntityId() {
        // Given
        when(featureFlagRepository.findAllByEntityId(entityId)).thenReturn(featureFlags);

        // When
        List<FeatureFlag> result = featureFlagService.getFeatureFlagsByEntityId(entityId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrderElementsOf(featureFlags);
        verify(featureFlagRepository, times(1)).findAllByEntityId(entityId);
    }

    @Test
    @DisplayName("Should get feature flag by entityId and name")
    void shouldGetFeatureFlagByEntityIdAndName() {
        // Given
        when(featureFlagRepository.findByEntityIdAndName(entityId, flagName)).thenReturn(Optional.of(featureFlag));

        // When
        Optional<FeatureFlag> result = featureFlagService.getFeatureFlag(entityId, flagName);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEntityId()).isEqualTo(entityId);
        assertThat(result.get().getName()).isEqualTo(flagName);
        assertThat(result.get().getValue()).isEqualTo(flagValue);
        verify(featureFlagRepository, times(1)).findByEntityIdAndName(entityId, flagName);
    }

    @Test
    @DisplayName("Should return empty optional when feature flag doesn't exist")
    void shouldReturnEmptyOptionalWhenFeatureFlagDoesNotExist() {
        // Given
        when(featureFlagRepository.findByEntityIdAndName(entityId, flagName)).thenReturn(Optional.empty());

        // When
        Optional<FeatureFlag> result = featureFlagService.getFeatureFlag(entityId, flagName);

        // Then
        assertThat(result).isEmpty();
        verify(featureFlagRepository, times(1)).findByEntityIdAndName(entityId, flagName);
    }

    @Test
    @DisplayName("Should create a feature flag when it doesn't exist")
    void shouldCreateFeatureFlagWhenDoesNotExist() {
        // Given
        FeatureFlagDto dto = new FeatureFlagDto();
        dto.setValue(flagValue);
        
        when(featureFlagRepository.findByEntityIdAndName(entityId, flagName)).thenReturn(Optional.empty());
        when(featureFlagRepository.save(any(FeatureFlag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        FeatureFlag result = featureFlagService.createOrUpdateFeatureFlag(entityId, flagName, dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEntityId()).isEqualTo(entityId);
        assertThat(result.getName()).isEqualTo(flagName);
        assertThat(result.getValue()).isEqualTo(flagValue);
        
        // Verify repository calls
        verify(featureFlagRepository, times(1)).findByEntityIdAndName(entityId, flagName);
        verify(featureFlagRepository, times(1)).save(any(FeatureFlag.class));
        
        // Verify no memcached publish was made - most likely it's handled in a separate method
        verifyNoInteractions(memcachedService);
    }

    @Test
    @DisplayName("Should update a feature flag when it exists")
    void shouldUpdateFeatureFlagWhenExists() {
        // Given
        FeatureFlagDto dto = new FeatureFlagDto();
        dto.setValue("false");  // Changed value
        
        when(featureFlagRepository.findByEntityIdAndName(entityId, flagName)).thenReturn(Optional.of(featureFlag));
        when(featureFlagRepository.save(any(FeatureFlag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        FeatureFlag result = featureFlagService.createOrUpdateFeatureFlag(entityId, flagName, dto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEntityId()).isEqualTo(entityId);
        assertThat(result.getName()).isEqualTo(flagName);
        assertThat(result.getValue()).isEqualTo("false");  // Updated value
        
        // Verify repository calls
        verify(featureFlagRepository, times(1)).findByEntityIdAndName(entityId, flagName);
        verify(featureFlagRepository, times(1)).save(any(FeatureFlag.class));
        
        // Verify no memcached publish was made - most likely it's handled in a separate method
        verifyNoInteractions(memcachedService);
    }

    @Test
    @DisplayName("Should delete feature flag by entityId and name")
    void shouldDeleteFeatureFlagByEntityIdAndName() {
        // Given
        when(featureFlagRepository.findByEntityIdAndName(entityId, flagName)).thenReturn(Optional.of(featureFlag));
        doNothing().when(featureFlagRepository).delete(any(FeatureFlag.class));

        // When
        boolean result = featureFlagService.deleteFeatureFlag(entityId, flagName);

        // Then
        assertThat(result).isTrue();
        verify(featureFlagRepository, times(1)).findByEntityIdAndName(entityId, flagName);
        verify(featureFlagRepository, times(1)).delete(featureFlag);
    }

    @Test
    @DisplayName("Should return false when trying to delete non-existent feature flag")
    void shouldReturnFalseWhenDeletingNonExistentFeatureFlag() {
        // Given
        when(featureFlagRepository.findByEntityIdAndName(entityId, flagName)).thenReturn(Optional.empty());

        // When
        boolean result = featureFlagService.deleteFeatureFlag(entityId, flagName);

        // Then
        assertThat(result).isFalse();
        verify(featureFlagRepository, times(1)).findByEntityIdAndName(entityId, flagName);
        verify(featureFlagRepository, never()).delete(any(FeatureFlag.class));
    }

    @Test
    @DisplayName("Should publish feature flags to Memcached for an entityId")
    void shouldPublishFeatureFlagsToMemcachedForEntityId() {
        // Given
        when(featureFlagRepository.findAllByEntityId(entityId)).thenReturn(featureFlags);
        doNothing().when(memcachedService).storeFeatureFlags(eq(entityId), any());

        // When
        boolean result = featureFlagService.publishToMemcached(entityId);

        // Then
        assertThat(result).isTrue();
        verify(featureFlagRepository, times(1)).findAllByEntityId(entityId);
        verify(memcachedService, times(1)).storeFeatureFlags(eq(entityId), any());
    }

    @Test
    @DisplayName("Should handle exception when publishing to Memcached")
    void shouldHandleExceptionWhenPublishingToMemcached() {
        // Given
        when(featureFlagRepository.findAllByEntityId(entityId)).thenReturn(featureFlags);
        doThrow(new RuntimeException("Memcached error")).when(memcachedService).storeFeatureFlags(eq(entityId), any());

        // When/Then
        assertThatThrownBy(() -> featureFlagService.publishToMemcached(entityId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Memcached error");
        
        verify(featureFlagRepository, times(1)).findAllByEntityId(entityId);
        verify(memcachedService, times(1)).storeFeatureFlags(eq(entityId), any());
    }

    @Test
    @DisplayName("Should publish all feature flags to Memcached")
    void shouldPublishAllFeatureFlagsToMemcached() {
        // Given
        List<String> entityIds = Arrays.asList("user123", "user456", "user789");
        
        when(featureFlagRepository.findAllEntityIds()).thenReturn(entityIds);
        when(featureFlagRepository.findAllByEntityId(anyString())).thenReturn(featureFlags);
        doNothing().when(memcachedService).storeFeatureFlags(anyString(), any());

        // When
        int result = featureFlagService.publishAllToMemcached();

        // Then
        assertThat(result).isEqualTo(3);  // 3 entity IDs processed
        verify(featureFlagRepository, times(1)).findAllEntityIds();
        verify(featureFlagRepository, times(3)).findAllByEntityId(anyString());
        verify(memcachedService, times(3)).storeFeatureFlags(anyString(), any());
    }

    @Test
    @DisplayName("Should handle empty entity list when publishing all")
    void shouldHandleEmptyEntityListWhenPublishingAll() {
        // Given
        when(featureFlagRepository.findAllEntityIds()).thenReturn(List.of());

        // When
        int result = featureFlagService.publishAllToMemcached();

        // Then
        assertThat(result).isZero();
        verify(featureFlagRepository, times(1)).findAllEntityIds();
        verify(featureFlagRepository, never()).findAllByEntityId(anyString());
        verify(memcachedService, never()).storeFeatureFlags(anyString(), any());
    }

 */
}

