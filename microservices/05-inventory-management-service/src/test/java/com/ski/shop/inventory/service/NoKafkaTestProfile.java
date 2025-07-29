package com.ski.shop.inventory.service;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

/**
 * Test profile that disables Kafka messaging
 */
public class NoKafkaTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                // Disable reactive messaging for tests
                "mp.messaging.incoming.product-events.connector", "smallrye-in-memory",
                "mp.messaging.outgoing.inventory-events.connector", "smallrye-in-memory"
        );
    }
}