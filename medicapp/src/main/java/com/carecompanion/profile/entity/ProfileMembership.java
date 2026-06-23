package com.carecompanion.profile.entity;

import com.carecompanion.common.persistence.SyncEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Links a user to a treatment profile with a role. Mirrors the
 * {@code profile_memberships} table defined in V1__init_schema.sql.
 *
 * <p>Foreign keys are stored as plain UUID strings, not JPA associations
 * (see {@link TreatmentProfile} for rationale).
 */
@Entity
@Table(name = "profile_memberships")
public class ProfileMembership extends SyncEntity {

    @Column(name = "profile_id", nullable = false, columnDefinition = "CHAR(36)")
    private String profileId;

    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
