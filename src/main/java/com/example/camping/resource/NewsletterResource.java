package com.example.camping.resource;

import com.example.camping.dto.EmailPayload;
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
    public Map<String, Object> subscribe(@Valid EmailPayload emailPayload) {
        LOGGER.info(() -> "Newsletter signup received: " + emailPayload.getUserEmail());
        return Map.of("success", true, "message", "Newsletter signup received");
    }

    private String emailDomain(String email) {
        if (email == null || !email.contains("@")) {
            return "unknown";
        }
        return email.substring(email.indexOf('@') + 1);
    }
}
