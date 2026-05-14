package com.example.camping.resource;

import com.example.camping.config.AppConfig;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class RootResource {
    @Inject
    AppConfig config;

    @GET
    public Map<String, Object> root() {
        return Map.of(
                "message", "Camping Kafka API",
                "status", "running",
                "topic", config.kafkaTopicName(),
                "schema_registry", config.schemaRegistryEndpoint(),
                "ksqldb", config.ksqlDbEndpoint()
        );
    }
}
