package com.example.camping.resource;

import com.example.camping.dto.EmailPayload;
import com.example.camping.util.InstanaTracingUtil;
import com.instana.sdk.annotation.Span;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Newsletter Resource
 *
 * 追蹤層級：
 * 1. Instana Agent 自動追蹤：HTTP 請求、方法調用
 * 2. SDK 手動追蹤：訂閱者資訊、業務事件
 */
@Path("/send_src_email")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class NewsletterResource {
    private static final Logger LOGGER = Logger.getLogger(NewsletterResource.class.getName());

    @POST
    @Span(value = "newsletter.subscribe", type = Span.Type.ENTRY)
    public Map<String, Object> subscribe(@Valid EmailPayload emailPayload) {
        return InstanaTracingUtil.trace("NewsletterResource.subscribe", () -> {
            InstanaTracingUtil.markStep("1.validate_email", "驗證電子郵件格式");
            
            String userEmail = emailPayload.getUserEmail();
            InstanaTracingUtil.addBusinessTag("subscriber.email", userEmail);
            
            InstanaTracingUtil.markStep("2.process_subscription", "處理訂閱請求");
            
            LOGGER.info(() -> "Newsletter signup received: " + userEmail);
            
            InstanaTracingUtil.logBusinessEvent("NEWSLETTER_SIGNUP",
                "New newsletter subscription: " + userEmail);
            
            InstanaTracingUtil.addBusinessTag("subscription.status", "success");
            
            return Map.of("success", true, "message", "Newsletter signup received");
        });
    }
}
