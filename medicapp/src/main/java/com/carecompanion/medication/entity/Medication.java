package com.carecompanion.medication.entity;

import com.carecompanion.common.persistence.SyncEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * A medication tracked within a treatment profile. Mirrors the
 * {@code medications} table defined in V1__init_schema.sql.
 *
 * <p>Foreign keys are stored as plain UUID strings, not JPA associations
 * (see TreatmentProfile for rationale).
 */
@Entity
@Table(name = "medications")
public class Medication extends SyncEntity {

    @Column(name = "profile_id", nullable = false, columnDefinition = "CHAR(36)")
    private String profileId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "dose_amount", nullable = false)
    private String doseAmount;

    @Column(name = "dose_unit", nullable = false)
    private String doseUnit;

    @Column(name = "treatment_start_date", nullable = false)
    private LocalDate treatmentStartDate;

    @Column(name = "treatment_end_date")
    private LocalDate treatmentEndDate;

    @Column(name = "notes", length = 1000)
    private String notes;

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDoseAmount() {
        return doseAmount;
    }

    public void setDoseAmount(String doseAmount) {
        this.doseAmount = doseAmount;
    }

    public String getDoseUnit() {
        return doseUnit;
    }

    public void setDoseUnit(String doseUnit) {
        this.doseUnit = doseUnit;
    }

    public LocalDate getTreatmentStartDate() {
        return treatmentStartDate;
    }

    public void setTreatmentStartDate(LocalDate treatmentStartDate) {
        this.treatmentStartDate = treatmentStartDate;
    }

    public LocalDate getTreatmentEndDate() {
        return treatmentEndDate;
    }

    public void setTreatmentEndDate(LocalDate treatmentEndDate) {
        this.treatmentEndDate = treatmentEndDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
