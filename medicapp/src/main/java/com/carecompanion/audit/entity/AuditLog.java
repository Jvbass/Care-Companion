package com.carecompanion.audit.entity;

import com.carecompanion.common.persistence.SyncEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * An append-only audit trail entry for a treatment profile. Mirrors the
 * {@code audit_logs} table defined in V1__init_schema.sql.
 *
 * <p>Extends {@link SyncEntity} for column uniformity, but {@code deleted}
 * is inert here: rows are never updated or soft-deleted by application
 * logic (see TECHNICAL_DESIGN section 8).
 *
 * <p>Foreign keys are stored as plain UUID strings, not JPA associations
 * (see {@code TreatmentProfile} for rationale).
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog extends SyncEntity {

    @Column(name = "profile_id", nullable = false, columnDefinition = "CHAR(36)")
    private String profileId;

    @Column(name = "actor_user_id", nullable = false, columnDefinition = "CHAR(36)")
    private String actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AuditAction action;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false, columnDefinition = "CHAR(36)")
    private String entityId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "summary", length = 1000)
    private String summary;

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(String actorUserId) {
        this.actorUserId = actorUserId;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
