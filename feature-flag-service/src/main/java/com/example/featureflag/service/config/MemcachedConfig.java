package com.example.featureflag.service.config;

import lombok.Getter;
import lombok.Setter;
import net.spy.memcached.MemcachedClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "memcached")
@Getter
@Setter
public class MemcachedConfig {
    
    /**
     * List of Memcached servers in the format host:port
     */
    private List<String> servers = new ArrayList<>();
    
    /**
     * Expiration time for cached items in seconds (never expires)
     */
    private int expirationTime = 0;
    
    /**
     * Creates a MemcachedClient bean configured with the specified servers
     * 
     * @return MemcachedClient instance
     * @throws IOException if connection to Memcached servers fails
     */
    @Bean
    public MemcachedClient memcachedClient() throws IOException {
        List<InetSocketAddress> addresses = new ArrayList<>();
        
        for (String server : servers) {
            String[] parts = server.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            addresses.add(new InetSocketAddress(host, port));
        }
        
        return new MemcachedClient(addresses);
    }
}

