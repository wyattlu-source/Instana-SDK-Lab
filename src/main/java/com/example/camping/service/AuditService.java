package com.example.camping.service;

import jakarta.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditService.class);

    public void record(String action, String user) {
        LOGGER.warn("[AUDIT] action=" + action + " user=" + user);
    }
}
