package com.example.camping.config;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Provider
public class Utf8JsonReaderInterceptor implements ReaderInterceptor {

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        MediaType mediaType = context.getMediaType();
        if (mediaType != null
                && MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType)
                && !mediaType.getParameters().containsKey("charset")) {
            Map<String, String> params = new HashMap<>(mediaType.getParameters());
            params.put("charset", "UTF-8");
            context.setMediaType(new MediaType(mediaType.getType(), mediaType.getSubtype(), params));
        }
        return context.proceed();
    }
}
