package com.example.camping.resource;

import com.example.camping.dto.EmailPayload;
import com.example.camping.observability.InstanaTracing;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.annotation.TagParam;
import jakarta.enterprise.context.RequestScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;
import java.util.logging.Logger;

@Path("/send_src_email")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class NewsletterResource {
    private static final Logger LOGGER = Logger.getLogger(NewsletterResource.class.getName());

    @POST
    @Span(type = Span.Type.ENTRY, value = InstanaTracing.NEWSLETTER_HTTP_SPAN, captureArguments = true, capturedStackFrames = 5)
    public Map<String, Object> subscribe(@Valid @TagParam("email_payload") EmailPayload emailPayload) {
        InstanaTracing.httpEntry(InstanaTracing.NEWSLETTER_HTTP_SPAN, "POST", "/api/send_src_email", 200);
        InstanaTracing.method(Span.Type.ENTRY, InstanaTracing.NEWSLETTER_HTTP_SPAN, NewsletterResource.class.getName(), "subscribe");
        InstanaTracing.entry(InstanaTracing.NEWSLETTER_HTTP_SPAN, "tags.newsletter.email_domain", emailDomain(emailPayload.getUserEmail()));
        LOGGER.info(() -> "Newsletter signup received: " + emailPayload.getUserEmail());
        return Map.of("success", true, "message", "Newsletter signup received");
    }

    @Span(type = Span.Type.INTERMEDIATE, value = InstanaTracing.EMAIL_DOMAIN_SPAN, captureArguments = true, captureReturn = true)
    private String emailDomain(@TagParam("email") String email) {
        InstanaTracing.method(InstanaTracing.EMAIL_DOMAIN_SPAN, NewsletterResource.class.getName(), "emailDomain");
        if (email == null || !email.contains("@")) {
            return "unknown";
        }
        return email.substring(email.indexOf('@') + 1);
    }
}
