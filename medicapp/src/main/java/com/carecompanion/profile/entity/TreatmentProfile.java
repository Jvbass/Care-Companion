package com.carecompanion.profile.entity;

import com.carecompanion.common.persistence.SyncEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A treatment profile (the person being cared for). Mirrors the
 * {@code treatment_profiles} table defined in V1__init_schema.sql.
 *
 * <p>Foreign keys are stored as plain UUID strings, not JPA associations,
 * to stay compatible with the delta-sync model (client-generated UUIDs,
 * record-level last-write-wins) and the DB-level ON DELETE policy.
 */
@Entity
@Table(name = "treatment_profiles")
public class TreatmentProfile extends SyncEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "owner_user_id", nullable = false, columnDefinition = "CHAR(36)")
    private String ownerUserId;

    @Column(name = "condition")
    private String condition;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
