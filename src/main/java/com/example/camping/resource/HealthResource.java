package com.example.camping.resource;

import com.example.camping.config.AppConfig;
import com.example.camping.observability.InstanaTracing;
import com.instana.sdk.annotation.Span;
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
    @Span(type = Span.Type.ENTRY, value = InstanaTracing.HEALTH_HTTP_SPAN, capturedStackFrames = 5)
    public Map<String, Object> health() {
        InstanaTracing.httpEntry(InstanaTracing.HEALTH_HTTP_SPAN, "GET", "/api/health", 200);
        InstanaTracing.method(Span.Type.ENTRY, InstanaTracing.HEALTH_HTTP_SPAN, HealthResource.class.getName(), "health");
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
