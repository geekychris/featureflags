package com.example.featureflag.service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class FeatureFlagId implements Serializable {
        @Column(name = "entity_id", nullable = false)
        private String entityId;

        @Column(name = "name", nullable = false)
        private String name;
    }