package com.raketo.league.audit;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class AuditListenerInitializer {
    private final AuditService auditService;

    public AuditListenerInitializer(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostConstruct
    public void init() {
        AuditEntityListener.setAuditService(auditService);
    }
}

