package com.example.camping.service;

import com.example.camping.observability.InstanaTracing;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
import com.instana.sdk.support.SpanSupport;
import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditService.class);

    @Span(type = Span.Type.INTERMEDIATE, value = InstanaTracing.AUDIT_SPAN, capturedStackFrames = 5)
    public void record(@TagParam("action") String action, @TagParam("user") String user) {
        InstanaTracing.method(InstanaTracing.AUDIT_SPAN, AuditService.class.getName(), "record");
        SpanSupport.annotate("tags.audit.action", action);
        SpanSupport.annotate("tags.audit.user", user == null ? "anonymous" : user);
        SpanSupport.annotate("tags.audit.timestamp", String.valueOf(System.currentTimeMillis()));
        SpanSupport.annotate("tags.service", "camping-api");
        LOGGER.warn("[AUDIT] action=" + action + " user=" + user);
    }
}
