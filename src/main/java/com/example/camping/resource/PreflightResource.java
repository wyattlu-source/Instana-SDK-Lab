package com.example.camping.resource;

import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("{path:.*}")
public class PreflightResource {
    @OPTIONS
    public Response options() {
        return Response.noContent().build();
    }
}
