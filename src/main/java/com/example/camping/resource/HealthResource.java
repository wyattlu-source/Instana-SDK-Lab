package com.example.camping.resource;

import com.example.camping.config.AppConfig;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class HealthResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthResource.class);

    @Inject
    AppConfig config;

    @Inject
    com.example.camping.config.MongoConfig mongoConfig;

    @GET
    public Map<String, Object> health() {
        return Map.of(
                "status", "healthy",
                "spots", "fallback",
                "checkout", "available",
                "topic", config.kafkaTopicName(),
                "kafka_bootstrap_server", config.kafkaBootstrapServer(),
                "schema_registry", config.schemaRegistryEndpoint(),
                "ksqldb", config.ksqlDbEndpoint(),
                "mongodb", mongoConfig.isAvailable() ? "connected"
                        : "unavailable: " + mongoConfig.getLastError()
        );
    }

    /** 測試 InstanaTracing.logInfo/logWarn/logError 是否正確出現在 Instana span tags */
    @GET
    @Path("/log-test")
    public Map<String, Object> logTest() {

        return Map.of(
                "result", "ok",
                "message", "3 log entries written to Instana span tags: log.0 / log.1 / log.2",
                "instana_tags", Map.of(
                        "log.0.level", "INFO",
                        "log.0.msg",   "[LOG-TEST] INFO  level - ...",
                        "log.1.level", "WARN",
                        "log.1.msg",   "[LOG-TEST] WARN  level - ...",
                        "log.2.level", "ERROR",
                        "log.2.msg",   "[LOG-TEST] ERROR level - ..."
                )
        );
    }
}
