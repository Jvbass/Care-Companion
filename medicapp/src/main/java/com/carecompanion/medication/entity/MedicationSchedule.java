package com.carecompanion.medication.entity;

import com.carecompanion.common.persistence.SyncEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Dosing schedule for a medication. Mirrors the
 * {@code medication_schedules} table defined in V1__init_schema.sql.
 *
 * <p>Foreign keys are stored as plain UUID strings, not JPA associations
 * (see TreatmentProfile for rationale).
 */
@Entity
@Table(name = "medication_schedules")
public class MedicationSchedule extends SyncEntity {

    @Column(name = "medication_id", nullable = false, columnDefinition = "CHAR(36)")
    private String medicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency_type", nullable = false)
    private FrequencyType frequencyType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "times")
    private List<String> times;

    @Column(name = "interval_hours")
    private Integer intervalHours;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "days_of_week")
    private List<String> daysOfWeek;

    @Column(name = "reminder_lead_minutes", nullable = false)
    private int reminderLeadMinutes;

    public String getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(String medicationId) {
        this.medicationId = medicationId;
    }

    public FrequencyType getFrequencyType() {
        return frequencyType;
    }

    public void setFrequencyType(FrequencyType frequencyType) {
        this.frequencyType = frequencyType;
    }

    public List<String> getTimes() {
        return times;
    }

    public void setTimes(List<String> times) {
        this.times = times;
    }

    public Integer getIntervalHours() {
        return intervalHours;
    }

    public void setIntervalHours(Integer intervalHours) {
        this.intervalHours = intervalHours;
    }

    public List<String> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(List<String> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public int getReminderLeadMinutes() {
        return reminderLeadMinutes;
    }

    public void setReminderLeadMinutes(int reminderLeadMinutes) {
        this.reminderLeadMinutes = reminderLeadMinutes;
    }
}
