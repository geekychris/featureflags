package com.example.featureflag.client.config;

import com.example.featureflag.client.FeatureFlagClient;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Configuration for the Feature Flag Client
 * Auto-configures Memcached client and feature flag client
 */
@Configuration
@EnableConfigurationProperties(FeatureFlagClientProperties.class)
@ConditionalOnProperty(prefix = "feature-flag.client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FeatureFlagClientConfig {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagClientConfig.class);

    /**
     * Creates a Memcached client bean if one is not already defined
     */
    @Bean
    @ConditionalOnMissingBean
    public MemcachedClient memcachedClient(FeatureFlagClientProperties properties) throws IOException {
        log.info("Initializing Memcached client with servers: {}", properties.getServers());

        ConnectionFactoryBuilder connectionFactoryBuilder = new ConnectionFactoryBuilder()
                .setProtocol(properties.getProtocol())
                .setOpTimeout(properties.getOperationTimeout())
                .setTimeoutExceptionThreshold(properties.getTimeoutExceptionThreshold());

        if (properties.getMaxReconnectDelay() > 0) {
            connectionFactoryBuilder.setMaxReconnectDelay(properties.getMaxReconnectDelay());
        }

        return new MemcachedClient(
                connectionFactoryBuilder.build(),
                AddrUtil.getAddresses(properties.getServers())
        );
    }

    /**
     * Creates a FeatureFlagClient bean if one is not already defined
     */
    @Bean
    @ConditionalOnMissingBean
    public FeatureFlagClient featureFlagClient(MemcachedClient memcachedClient, FeatureFlagClientProperties properties) {
        log.info("Initializing Feature Flag Client with timeout: {} ms", properties.getClientTimeout());
        return new FeatureFlagClient(memcachedClient, properties.getClientTimeout());
    }
}


