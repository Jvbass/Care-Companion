package com.carecompanion.appointment.entity;

import com.carecompanion.common.persistence.SyncEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A medical appointment within a treatment profile. Mirrors the
 * {@code appointments} table defined in V1__init_schema.sql.
 *
 * <p>Foreign keys are stored as plain UUID strings, not JPA associations
 * (see TreatmentProfile for rationale).
 */
@Entity
@Table(name = "appointments")
public class Appointment extends SyncEntity {

    @Column(name = "profile_id", nullable = false, columnDefinition = "CHAR(36)")
    private String profileId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "datetime", nullable = false)
    private Instant datetime;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "purpose")
    private String purpose;

    @Column(name = "reminder_lead_minutes", nullable = false)
    private int reminderLeadMinutes;

    @Column(name = "notes", length = 1000)
    private String notes;

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Instant getDatetime() {
        return datetime;
    }

    public void setDatetime(Instant datetime) {
        this.datetime = datetime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public int getReminderLeadMinutes() {
        return reminderLeadMinutes;
    }

    public void setReminderLeadMinutes(int reminderLeadMinutes) {
        this.reminderLeadMinutes = reminderLeadMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
