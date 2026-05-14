package com.example.camping.resource;

import com.example.camping.config.AppConfig;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class HealthResource {
    @Inject
    AppConfig config;

    @GET
    public Map<String, Object> health() {
        return Map.of(
                "status", "healthy",
                "spots", "fallback",
                "checkout", "available",
                "topic", config.kafkaTopicName(),
                "kafka_bootstrap_server", config.kafkaBootstrapServer(),
                "schema_registry", config.schemaRegistryEndpoint(),
                "ksqldb", config.ksqlDbEndpoint()
        );
    }
}
