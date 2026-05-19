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
                "ksqldb", config.ksqlDbEndpoint(),
                "mongodb", mongoConfig.isAvailable() ? "connected"
                        : "unavailable: " + mongoConfig.getLastError()
        );
    }

    /** 測試 InstanaTracing.logInfo/logWarn/logError 是否正確出現在 Instana span tags */
    @GET
    @Path("/log-test")
    @Span(type = Span.Type.ENTRY, value = "camping-api-log-test", capturedStackFrames = 5)
    public Map<String, Object> logTest() {
        InstanaTracing.httpEntry("camping-api-log-test", "GET", "/api/health/log-test", 200);

        InstanaTracing.logInfo(LOGGER,  "[LOG-TEST] INFO  level - 這是 INFO  log，直接寫進 Instana span tag");
        InstanaTracing.logWarn(LOGGER,  "[LOG-TEST] WARN  level - 這是 WARN  log，直接寫進 Instana span tag");
        InstanaTracing.logError(LOGGER, "[LOG-TEST] ERROR level - 這是 ERROR log，直接寫進 Instana span tag");

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
