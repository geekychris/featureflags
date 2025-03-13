package com.example.featureflag.service.service;

import com.example.featureflag.service.config.MemcachedConfig;
import com.example.featureflag.service.model.FeatureFlag;
import com.example.featureflag.service.model.FeatureFlagId;
import net.spy.memcached.MemcachedClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemcachedServiceTest {

    @Mock
    private MemcachedClient memcachedClient;

    @Mock
    private MemcachedConfig memcachedConfig;

    @InjectMocks
    private MemcachedService memcachedService;

    private static final int EXPIRATION_TIME = 3600;
    private static final String ENTITY_ID = "test-entity";
    private static final String KEY = "feature_flags:test-entity";

    private List<FeatureFlag> sampleFeatureFlags;
    private Map<String, String> sampleFeatureFlagMap;

    @BeforeEach
    void setUp() {
        // Setup sample data
        sampleFeatureFlags = new ArrayList<>();
        FeatureFlag flag1 = new FeatureFlag();
        flag1.setValue("true");
        
        FeatureFlagId id1 = new FeatureFlagId();
        id1.setEntityId(ENTITY_ID);
        id1.setName("feature1");
        flag1.setId(id1);
        
        FeatureFlag flag2 = new FeatureFlag();
        flag2.setValue("false");
        
        FeatureFlagId id2 = new FeatureFlagId();
        id2.setEntityId(ENTITY_ID);
        id2.setName("feature2");
        flag2.setId(id2);

        sampleFeatureFlags.add(flag1);
        sampleFeatureFlags.add(flag2);

        // Setup expected map
        sampleFeatureFlagMap = new HashMap<>();
        sampleFeatureFlagMap.put("feature1", "true");
        sampleFeatureFlagMap.put("feature2", "false");

        // Setup config
        when(memcachedConfig.getExpirationTime()).thenReturn(EXPIRATION_TIME);
    }

    @Test
    void storeFeatureFlagsSuccessfully() {
        // Arrange
        when(memcachedClient.set(eq(KEY), eq(EXPIRATION_TIME), any(Map.class))).thenReturn(null);

        // Act
        memcachedService.storeFeatureFlags(ENTITY_ID, sampleFeatureFlags);

        // Assert
        verify(memcachedClient).set(eq(KEY), eq(EXPIRATION_TIME), any(Map.class));
    }

    @Test
    void storeFeatureFlagsHandlesException() {
        // Arrange
        when(memcachedClient.set(eq(KEY), eq(EXPIRATION_TIME), any(Map.class)))
                .thenThrow(new RuntimeException("Connection error"));

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> memcachedService.storeFeatureFlags(ENTITY_ID, sampleFeatureFlags));
        verify(memcachedClient).set(eq(KEY), eq(EXPIRATION_TIME), any(Map.class));
    }

    @Test
    void storeMultipleFeatureFlags() {
        // Arrange
        Map<String, List<FeatureFlag>> entityFeatureFlags = new HashMap<>();
        entityFeatureFlags.put(ENTITY_ID, sampleFeatureFlags);
        entityFeatureFlags.put("another-entity", new ArrayList<>());
        
        when(memcachedClient.set(anyString(), anyInt(), any())).thenReturn(null);

        // Act
        memcachedService.storeMultipleFeatureFlags(entityFeatureFlags);

        // Assert
        verify(memcachedClient, times(2)).set(anyString(), anyInt(), any());
    }

    @Test
    void retrieveFeatureFlagsSuccessfully() {
        // Arrange
        when(memcachedClient.get(KEY)).thenReturn(sampleFeatureFlagMap);

        // Act
        Map<String, String> result = memcachedService.retrieveFeatureFlags(ENTITY_ID);

        // Assert
        assertThat(result).isNotNull()
                          .isNotEmpty()
                          .hasSize(2)
                          .containsEntry("feature1", "true")
                          .containsEntry("feature2", "false");
        verify(memcachedClient).get(KEY);
    }

    @Test
    void retrieveFeatureFlagsHandlesNullResult() {
        // Arrange
        when(memcachedClient.get(KEY)).thenReturn(null);

        // Act
        Map<String, String> result = memcachedService.retrieveFeatureFlags(ENTITY_ID);

        // Assert
        assertThat(result).isNotNull().isEmpty();
        verify(memcachedClient).get(KEY);
    }

    @Test
    void retrieveFeatureFlagsHandlesNonMapResult() {
        // Arrange
        when(memcachedClient.get(KEY)).thenReturn("not a map");

        // Act
        Map<String, String> result = memcachedService.retrieveFeatureFlags(ENTITY_ID);

        // Assert
        assertThat(result).isNotNull().isEmpty();
        verify(memcachedClient).get(KEY);
    }

    @Test
    void retrieveFeatureFlagsHandlesException() {
        // Arrange
        when(memcachedClient.get(KEY)).thenThrow(new RuntimeException("Connection error"));

        // Act
        Map<String, String> result = memcachedService.retrieveFeatureFlags(ENTITY_ID);

        // Assert
        assertThat(result).isNotNull().isEmpty();
        verify(memcachedClient).get(KEY);
    }
}

/*
package com.example.featureflag.service.service;

import com.example.featureflag.service.model.FeatureFlag;
import net.spy.memcached.MemcachedClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemcachedServiceTest {

    @Mock
    private MemcachedClient memcachedClient;

    @InjectMocks
    private MemcachedService memcachedService;

    private final String entityId = "user123";
    private FeatureFlag featureFlag1;
    private FeatureFlag featureFlag2;
    private List<FeatureFlag> featureFlags;
    private Map<String, String> featureFlagMap;

    @Mock
    private Future<Boolean> future;

    @BeforeEach
    void setUp() {
        featureFlag1 = new FeatureFlag();
        featureFlag1.setEntityId(entityId);
        featureFlag1.setName("dark-mode");
        featureFlag1.setValue("true");

        featureFlag2 = new FeatureFlag();
        featureFlag2.setEntityId(entityId);
        featureFlag2.setName("premium-user");
        featureFlag2.setValue("false");

        featureFlags = Arrays.asList(featureFlag1, featureFlag2);

        featureFlagMap = new HashMap<>();
        featureFlagMap.put("dark-mode", "true");
        featureFlagMap.put("premium-user", "false");
    }

    @Test
    @DisplayName("Should store feature flags in Memcached")
    void shouldStoreFeatureFlagsInMemcached() throws Exception {
        // Given
        when(future.get(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(memcachedClient.set

*/