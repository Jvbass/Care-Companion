package com.carecompanion.sharing.entity;

import com.carecompanion.common.persistence.SyncEntity;
import com.carecompanion.profile.entity.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A share code granting access to a treatment profile. Mirrors the
 * {@code share_codes} table defined in V1__init_schema.sql.
 *
 * <p>Foreign keys are stored as plain UUID strings, not JPA associations
 * (see TreatmentProfile for rationale).
 */
@Entity
@Table(name = "share_codes")
public class ShareCode extends SyncEntity {

    @Column(name = "profile_id", nullable = false, columnDefinition = "CHAR(36)")
    private String profileId;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_role", nullable = false)
    private Role defaultRole;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Role getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(Role defaultRole) {
        this.defaultRole = defaultRole;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }
}
