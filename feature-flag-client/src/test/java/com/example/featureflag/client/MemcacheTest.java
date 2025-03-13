package com.example.featureflag.client;

import com.example.featureflag.client.config.FeatureFlagClientConfig;
import com.example.featureflag.client.config.FeatureFlagClientProperties;
import net.spy.memcached.MemcachedClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MemcacheTest {

    @Test
    public void test() throws IOException {
        FeatureFlagClientConfig config = new FeatureFlagClientConfig();
        FeatureFlagClientProperties properties = new FeatureFlagClientProperties();
        FeatureFlagClient featureFlagClient = new FeatureFlagClient(config.memcachedClient(properties), properties.getClientTimeout());
        Map<String,String> vals = featureFlagClient.getFeatureFlags("1234");
        System.out.println(vals);
    }

    public MemcachedClient memcachedClient(List<String> servers) throws IOException {
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
