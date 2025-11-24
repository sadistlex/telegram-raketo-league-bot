package com.raketo.league.audit;

import jakarta.persistence.*;

public class AuditEntityListener {

    private static AuditService auditService;

    public static void setAuditService(AuditService service) {
        auditService = service;
    }

    @PostPersist
    public void postPersist(Object entity) {
        if (auditService != null) {
            auditService.record(entity.getClass().getSimpleName(), "create", entity);
        }
    }

    @PostUpdate
    public void postUpdate(Object entity) {
        if (auditService != null) {
            auditService.record(entity.getClass().getSimpleName(), "update", entity);
        }
    }

    @PostRemove
    public void postRemove(Object entity) {
        if (auditService != null) {
            auditService.record(entity.getClass().getSimpleName(), "delete", entity);
        }
    }
}

