package com.example.featureflag.client.config;

import lombok.Data;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Feature Flag Client
 */
@Data
@ConfigurationProperties(prefix = "feature-flag.client")
public class FeatureFlagClientProperties {

    /**
     * Whether the feature flag client is enabled
     */
    private boolean enabled = true;

    /**
     * Comma-separated list of Memcached servers in the format "host:port"
     */
    private String servers = "localhost:11211";

    /**
     * Protocol to use for Memcached connection
     */
    private Protocol protocol = Protocol.BINARY;

    /**
     * Timeout for client operations in milliseconds
     */
    private int clientTimeout = 1000;

    /**
     * Operation timeout for Memcached in milliseconds
     */
    private long operationTimeout = 2500;

    /**
     * Timeout exception threshold for Memcached connections
     */
    private int timeoutExceptionThreshold = 10;

    /**
     * Maximum reconnect delay in milliseconds (0 for default behavior)
     */
    private long maxReconnectDelay = 0;

    /**
     * Feature flag cache key prefix
     */
    private String cacheKeyPrefix = "feature_flags:";
}

