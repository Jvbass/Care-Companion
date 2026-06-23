package com.carecompanion.note.entity;

import com.carecompanion.common.persistence.SyncEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A free-text note (symptom or question) within a treatment profile.
 * Mirrors the {@code notes} table defined in V1__init_schema.sql.
 *
 * <p>Foreign keys are stored as plain UUID strings, not JPA associations
 * (see TreatmentProfile for rationale).
 */
@Entity
@Table(name = "notes")
public class Note extends SyncEntity {

    @Column(name = "profile_id", nullable = false, columnDefinition = "CHAR(36)")
    private String profileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NoteType type;

    @Column(name = "text", nullable = false, length = 2000)
    private String text;

    @Column(name = "linked_appointment_id", columnDefinition = "CHAR(36)")
    private String linkedAppointmentId;

    @Column(name = "resolved", nullable = false)
    private boolean resolved;

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public NoteType getType() {
        return type;
    }

    public void setType(NoteType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLinkedAppointmentId() {
        return linkedAppointmentId;
    }

    public void setLinkedAppointmentId(String linkedAppointmentId) {
        this.linkedAppointmentId = linkedAppointmentId;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
}
