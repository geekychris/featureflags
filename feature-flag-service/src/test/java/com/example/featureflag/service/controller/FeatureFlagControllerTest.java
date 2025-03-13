package com.example.featureflag.service.controller;

import com.example.featureflag.service.dto.FeatureFlagDto;
import com.example.featureflag.service.model.FeatureFlag;
import com.example.featureflag.service.service.FeatureFlagService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeatureFlagController.class)
public class FeatureFlagControllerTest {
/*
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FeatureFlagService featureFlagService;

    private FeatureFlagDto testFeatureFlag1;
    private FeatureFlagDto testFeatureFlag2;

    @BeforeEach
    void setUp() {
        // Common test data setup
        testFeatureFlag1 = new FeatureFlagDto();
        testFeatureFlag1.setName("testFlag1");
        testFeatureFlag1.setValue("true");

        testFeatureFlag2 = new FeatureFlagDto();
        testFeatureFlag2.setName("testFlag2");
        testFeatureFlag2.setValue("false");
    }

    // Test GET all feature flags for an entity
    @Test
    void getFeatureFlags_Success() throws Exception {
        String entityId = "entity1";
        List<FeatureFlagDto> featureFlags = Arrays.asList(testFeatureFlag1, testFeatureFlag2);
        
        when(featureFlagService.getFeatureFlag(entityId)).thenReturn(featureFlags);

        mockMvc.perform(get("/featureflags/{entityId}", entityId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("testFlag1")))
                .andExpect(jsonPath("$[0].value", is("true")))
                .andExpect(jsonPath("$[1].name", is("testFlag2")))
                .andExpect(jsonPath("$[1].value", is("false")));

        verify(featureFlagService).getFeatureFlags(entityId);
    }

    @Test
    void getFeatureFlags_EmptyList() throws Exception {
        String entityId = "unknownEntity";
        when(featureFlagService.getFeatureFlags(entityId)).thenReturn(List.of());

        mockMvc.perform(get("/featureflags/{entityId}", entityId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(featureFlagService).getFeatureFlags(entityId);
    }

    // Test GET a specific feature flag
    @Test
    void getFeatureFlag_Success() throws Exception {
        String entityId = "entity1";
        String flagName = "testFlag1";
        
        when(featureFlagService.getFeatureFlag(entityId, flagName)).thenReturn(Optional.of(testFeatureFlag1));

        mockMvc.perform(get("/featureflags/{entityId}/{name}", entityId, flagName))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("testFlag1")))
                .andExpect(jsonPath("$.value", is("true")));

        verify(featureFlagService).getFeatureFlag(entityId, flagName);
    }

    @Test
    void getFeatureFlag_NotFound() throws Exception {
        String entityId = "entity1";
        String flagName = "nonExistentFlag";
        
        when(featureFlagService.getFeatureFlag(entityId, flagName)).thenReturn(Optional.empty());

        mockMvc.perform(get("/featureflags/{entityId}/{name}", entityId, flagName))
                .andExpect(status().isNotFound());

        verify(featureFlagService).getFeatureFlag(entityId, flagName);
    }

    // Test POST (create/update) a feature flag
    @Test
    void createOrUpdateFeatureFlag_Create_Success() throws Exception {
        String entityId = "entity1";
        String flagName = "newFlag";
        FeatureFlagDto input = new FeatureFlagDto();
        input.setName(flagName);
        input.setValue("some-value");
        
        when(featureFlagService.createOrUpdateFeatureFlag(eq(entityId), eq(flagName), any(FeatureFlagDto.class)))
                .thenReturn(input);

        mockMvc.perform(post("/featureflags/{entityId}/{name}", entityId, flagName)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is(flagName)))
                .andExpect(jsonPath("$.value", is("some-value")));

        verify(featureFlagService).createOrUpdateFeatureFlag(eq(entityId), eq(flagName), any(FeatureFlagDto.class));
    }

    @Test
    void createOrUpdateFeatureFlag_InvalidInput() throws Exception {
        String entityId = "entity1";
        String flagName = "testFlag";
        FeatureFlagDto invalidInput = new FeatureFlagDto(); // Missing value
        
        mockMvc.perform(post("/featureflags/{entityId}/{name}", entityId, flagName)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidInput)))
                .andExpect(status().isBadRequest());

        verify(featureFlagService, never()).createOrUpdateFeatureFlag(anyString(), anyString(), any(FeatureFlagDto.class));
    }
    
    @Test
    void createOrUpdateFeatureFlag_MismatchedName() throws Exception {
        String entityId = "entity1";
        String flagName = "testFlag";
        FeatureFlagDto mismatchedInput = new FeatureFlagDto();
        mismatchedInput.setName("differentName"); // Name doesn't match path variable
        mismatchedInput.setValue("some-value");
        
        mockMvc.perform(post("/featureflags/{entityId}/{name}", entityId, flagName)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mismatchedInput)))
                .andExpect(status().isBadRequest());

        verify(featureFlagService, never()).createOrUpdateFeatureFlag(anyString(), anyString(), any(FeatureFlagDto.class));
    }

    // Test DELETE a feature flag
    @Test
    void deleteFeatureFlag_Success() throws Exception {
        String entityId = "entity1";
        String flagName = "testFlag1";
        
        when(featureFlagService.deleteFeatureFlag(entityId, flagName)).thenReturn(true);

        mockMvc.perform(delete("/featureflags/{entityId}/{name}", entityId, flagName))
                .andExpect(status().isOk());

        verify(featureFlagService).deleteFeatureFlag(entityId, flagName);
    }

    @Test
    void deleteFeatureFlag_NotFound() throws Exception {
        String entityId = "entity1";
        String flagName = "nonExistentFlag";
        
        when(featureFlagService.deleteFeatureFlag(entityId, flagName)).thenReturn(false);

        mockMvc.perform(delete("/featureflags/{entityId}/{name}", entityId, flagName))
                .andExpect(status().isNotFound());

        verify(featureFlagService).deleteFeatureFlag(entityId, flagName);
    }

    // Test publish feature flags for an entity to Memcached
    @Test
    void publishFeatureFlags_Success() throws Exception {
        String entityId = "entity1";
        
        when(featureFlagService.publishFeatureFlags(entityId)).thenReturn(true);

        mockMvc.perform(post("/featureflags/publish/{entityId}", entityId))
                .andExpect(status().isOk());

        verify(featureFlagService).publishFeatureFlags(entityId);
    }

    @Test
    void publishFeatureFlags_NotFound() throws Exception {
        String entityId = "unknownEntity";
        
        when(featureFlagService.publishFeatureFlags(entityId)).thenReturn(false);

        mockMvc.perform(post("/featureflags/publish/{entityId}", entityId))
                .andExpect(status().isNotFound());

        verify(featureFlagService).publishFeatureFlags(entityId);
    }

    // Test publish all feature flags to Memcached
    @Test
    void publishAllFeatureFlags_Success() throws Exception {
        when(featureFlagService.publishAllFeatureFlags()).thenReturn(true);

        mockMvc.perform(post("/featureflags/publish/all"))
                .andExpect(status().isOk());

        verify(featureFlagService).publishAllFeatureFlags();
    }

    @Test
    void publishAllFeatureFlags_Failure() throws Exception {
        when(featureFlagService.publishAllFeatureFlags()).thenReturn(false);

        mockMvc.perform(post("/featureflags/publish/all"))
                .andExpect(status().isInternalServerError());

        verify(featureFlagService).publishAllFeatureFlags();
    }

    // Test error handling for service exceptions
    @Test
    void handleServiceException() throws Exception {
        String entityId = "entity1";
        
        when(featureFlagService.getFeatureFlags(entityId))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/featureflags/{entityId}", entityId))
                .andExpect(status().isInternalServerError());

        verify(featureFlagService).getFeatureFlags(entityId);
    }

 */
}

