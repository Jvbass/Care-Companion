package com.carecompanion.medication.entity;

import com.carecompanion.common.persistence.SyncEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A logged outcome for a scheduled dose. Mirrors the {@code dose_logs}
 * table defined in V1__init_schema.sql.
 *
 * <p>Foreign keys are stored as plain UUID strings, not JPA associations
 * (see TreatmentProfile for rationale). Note: dose_logs has no direct
 * profile_id column; it hangs off schedule_id.
 */
@Entity
@Table(name = "dose_logs")
public class DoseLog extends SyncEntity {

    @Column(name = "schedule_id", nullable = false, columnDefinition = "CHAR(36)")
    private String scheduleId;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DoseStatus status;

    @Column(name = "logged_by_user_id", nullable = false, columnDefinition = "CHAR(36)")
    private String loggedByUserId;

    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt;

    @Column(name = "note", length = 1000)
    private String note;

    public String getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public DoseStatus getStatus() {
        return status;
    }

    public void setStatus(DoseStatus status) {
        this.status = status;
    }

    public String getLoggedByUserId() {
        return loggedByUserId;
    }

    public void setLoggedByUserId(String loggedByUserId) {
        this.loggedByUserId = loggedByUserId;
    }

    public Instant getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(Instant loggedAt) {
        this.loggedAt = loggedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
