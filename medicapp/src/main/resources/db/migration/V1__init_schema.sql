-- V1: initial schema for Care Companion core domain.
-- Single atomic migration for greenfield schema (10 tables, FK-safe order).
-- Engine: InnoDB, charset: utf8mb4 (full Unicode support, emoji-safe).
--
-- Shared sync columns on every profile-data table:
--   id          CHAR(36)    NOT NULL PRIMARY KEY  -- UUID generated client-side
--   created_at  DATETIME(3) NOT NULL
--   updated_at  DATETIME(3) NOT NULL               -- last edit time (client clock)
--   server_seq  BIGINT      NOT NULL               -- monotonic server sequence (pull cursor)
--   deleted     BOOLEAN     NOT NULL DEFAULT FALSE -- soft-delete tombstone flag
--   deleted_at  DATETIME(3) NULL
--
-- audit_logs keeps an inert `deleted` column (always FALSE) only for JPA
-- @MappedSuperclass uniformity with SyncEntity. It is append-only: rows are
-- never updated or soft-deleted by application logic (see TECHNICAL_DESIGN §8).

-- ============================================================================
-- users
-- ============================================================================
CREATE TABLE users (
    id            CHAR(36)     NOT NULL,
    created_at    DATETIME(3)  NOT NULL,
    updated_at    DATETIME(3)  NOT NULL,
    server_seq    BIGINT       NOT NULL,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    DATETIME(3)  NULL,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    birth_date    DATE         NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    INDEX idx_users_server_seq (server_seq)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ============================================================================
-- treatment_profiles
-- ============================================================================
CREATE TABLE treatment_profiles (
    id            CHAR(36)     NOT NULL,
    created_at    DATETIME(3)  NOT NULL,
    updated_at    DATETIME(3)  NOT NULL,
    server_seq    BIGINT       NOT NULL,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at    DATETIME(3)  NULL,
    title         VARCHAR(255) NOT NULL,
    owner_user_id CHAR(36)     NOT NULL,
    `condition`   VARCHAR(255) NULL,
    PRIMARY KEY (id),
    INDEX idx_treatment_profiles_server_seq (server_seq),
    INDEX idx_treatment_profiles_owner (owner_user_id),
    CONSTRAINT fk_treatment_profiles_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ============================================================================
-- profile_memberships
-- ============================================================================
CREATE TABLE profile_memberships (
    id          CHAR(36)                              NOT NULL,
    created_at  DATETIME(3)                            NOT NULL,
    updated_at  DATETIME(3)                            NOT NULL,
    server_seq  BIGINT                                 NOT NULL,
    deleted     BOOLEAN                                NOT NULL DEFAULT FALSE,
    deleted_at  DATETIME(3)                            NULL,
    profile_id  CHAR(36)                               NOT NULL,
    user_id     CHAR(36)                               NOT NULL,
    role        ENUM ('OWNER', 'CAREGIVER', 'VIEWER')  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_membership (profile_id, user_id),
    INDEX idx_profile_memberships_server_seq (server_seq),
    INDEX idx_profile_memberships_profile_seq (profile_id, server_seq),
    CONSTRAINT fk_profile_memberships_profile
        FOREIGN KEY (profile_id) REFERENCES treatment_profiles (id),
    CONSTRAINT fk_profile_memberships_user
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ============================================================================
-- medications
-- ============================================================================
CREATE TABLE medications (
    id                    CHAR(36)     NOT NULL,
    created_at            DATETIME(3)  NOT NULL,
    updated_at            DATETIME(3)  NOT NULL,
    server_seq            BIGINT       NOT NULL,
    deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at            DATETIME(3)  NULL,
    profile_id            CHAR(36)     NOT NULL,
    name                  VARCHAR(255) NOT NULL,
    dose_amount           VARCHAR(255) NOT NULL,
    dose_unit             VARCHAR(255) NOT NULL,
    treatment_start_date  DATE         NOT NULL,
    treatment_end_date    DATE         NULL,
    notes                 VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    INDEX idx_medications_server_seq (server_seq),
    INDEX idx_medications_profile_seq (profile_id, server_seq),
    CONSTRAINT fk_medications_profile
        FOREIGN KEY (profile_id) REFERENCES treatment_profiles (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ============================================================================
-- medication_schedules
-- ============================================================================
CREATE TABLE medication_schedules (
    id                     CHAR(36)                                                          NOT NULL,
    created_at             DATETIME(3)                                                        NOT NULL,
    updated_at             DATETIME(3)                                                        NOT NULL,
    server_seq             BIGINT                                                             NOT NULL,
    deleted                BOOLEAN                                                            NOT NULL DEFAULT FALSE,
    deleted_at             DATETIME(3)                                                        NULL,
    medication_id          CHAR(36)                                                           NOT NULL,
    frequency_type         ENUM ('FIXED_TIMES', 'EVERY_X_HOURS', 'DAYS_OF_WEEK', 'AS_NEEDED')  NOT NULL,
    times                  JSON                                                               NULL,
    interval_hours         INT                                                                NULL,
    days_of_week           JSON                                                               NULL,
    reminder_lead_minutes  INT                                                                NOT NULL DEFAULT 30,
    PRIMARY KEY (id),
    INDEX idx_medication_schedules_server_seq (server_seq),
    INDEX idx_medication_schedules_profile_seq (medication_id, server_seq),
    CONSTRAINT fk_medication_schedules_medication
        FOREIGN KEY (medication_id) REFERENCES medications (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ============================================================================
-- dose_logs
-- Note: no direct profile_id (hangs off schedule_id). Indexed by server_seq
-- and schedule_id; the (profile_id, server_seq) composite does not apply here.
-- ============================================================================
CREATE TABLE dose_logs (
    id                  CHAR(36)                                  NOT NULL,
    created_at          DATETIME(3)                                NOT NULL,
    updated_at          DATETIME(3)                                NOT NULL,
    server_seq          BIGINT                                      NOT NULL,
    deleted             BOOLEAN                                     NOT NULL DEFAULT FALSE,
    deleted_at          DATETIME(3)                                NULL,
    schedule_id         CHAR(36)                                    NOT NULL,
    scheduled_at        DATETIME(3)                                 NOT NULL,
    status              ENUM ('TAKEN', 'SKIPPED', 'POSTPONED')      NOT NULL,
    logged_by_user_id   CHAR(36)                                    NOT NULL,
    logged_at           DATETIME(3)                                 NOT NULL,
    note                VARCHAR(1000)                               NULL,
    PRIMARY KEY (id),
    INDEX idx_dose_logs_server_seq (server_seq),
    INDEX idx_dose_logs_schedule_id (schedule_id),
    CONSTRAINT fk_dose_logs_schedule
        FOREIGN KEY (schedule_id) REFERENCES medication_schedules (id),
    CONSTRAINT fk_dose_logs_logged_by_user
        FOREIGN KEY (logged_by_user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ============================================================================
-- appointments
-- ============================================================================
CREATE TABLE appointments (
    id                     CHAR(36)      NOT NULL,
    created_at             DATETIME(3)   NOT NULL,
    updated_at             DATETIME(3)   NOT NULL,
    server_seq             BIGINT        NOT NULL,
    deleted                BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at             DATETIME(3)   NULL,
    profile_id             CHAR(36)      NOT NULL,
    title                  VARCHAR(255)  NOT NULL,
    datetime               DATETIME(3)   NOT NULL,
    location               VARCHAR(255)  NOT NULL,
    purpose                VARCHAR(255)  NULL,
    reminder_lead_minutes  INT           NOT NULL DEFAULT 30,
    notes                  VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    INDEX idx_appointments_server_seq (server_seq),
    INDEX idx_appointments_profile_seq (profile_id, server_seq),
    CONSTRAINT fk_appointments_profile
        FOREIGN KEY (profile_id) REFERENCES treatment_profiles (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ============================================================================
-- notes
-- ============================================================================
CREATE TABLE notes (
    id                      CHAR(36)                         NOT NULL,
    created_at              DATETIME(3)                       NOT NULL,
    updated_at              DATETIME(3)                       NOT NULL,
    server_seq              BIGINT                             NOT NULL,
    deleted                 BOOLEAN                            NOT NULL DEFAULT FALSE,
    deleted_at              DATETIME(3)                        NULL,
    profile_id              CHAR(36)                           NOT NULL,
    type                    ENUM ('SYMPTOM', 'QUESTION')       NOT NULL,
    text                    VARCHAR(2000)                      NOT NULL,
    linked_appointment_id   CHAR(36)                           NULL,
    resolved                BOOLEAN                            NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    INDEX idx_notes_server_seq (server_seq),
    INDEX idx_notes_profile_seq (profile_id, server_seq),
    CONSTRAINT fk_notes_profile
        FOREIGN KEY (profile_id) REFERENCES treatment_profiles (id),
    CONSTRAINT fk_notes_linked_appointment
        FOREIGN KEY (linked_appointment_id) REFERENCES appointments (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ============================================================================
-- share_codes
-- ============================================================================
CREATE TABLE share_codes (
    id            CHAR(36)                                NOT NULL,
    created_at    DATETIME(3)                              NOT NULL,
    updated_at    DATETIME(3)                              NOT NULL,
    server_seq    BIGINT                                   NOT NULL,
    deleted       BOOLEAN                                  NOT NULL DEFAULT FALSE,
    deleted_at    DATETIME(3)                              NULL,
    profile_id    CHAR(36)                                 NOT NULL,
    code          VARCHAR(64)                              NOT NULL,
    default_role  ENUM ('OWNER', 'CAREGIVER', 'VIEWER')    NOT NULL,
    expires_at    DATETIME(3)                              NOT NULL,
    revoked       BOOLEAN                                  NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    INDEX idx_share_codes_server_seq (server_seq),
    INDEX idx_share_codes_profile_seq (profile_id, server_seq),
    INDEX idx_share_codes_code (code),
    CONSTRAINT fk_share_codes_profile
        FOREIGN KEY (profile_id) REFERENCES treatment_profiles (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ============================================================================
-- audit_logs
-- Append-only: rows are never updated or soft-deleted by application logic
-- (see TECHNICAL_DESIGN §8). The `deleted` column is kept only for
-- @MappedSuperclass uniformity with SyncEntity and is always FALSE.
-- ============================================================================
CREATE TABLE audit_logs (
    id              CHAR(36)                            NOT NULL,
    created_at      DATETIME(3)                          NOT NULL,
    updated_at      DATETIME(3)                          NOT NULL,
    server_seq      BIGINT                               NOT NULL,
    deleted         BOOLEAN                              NOT NULL DEFAULT FALSE,
    deleted_at      DATETIME(3)                          NULL,
    profile_id      CHAR(36)                             NOT NULL,
    actor_user_id   CHAR(36)                             NOT NULL,
    action          ENUM ('CREATE', 'UPDATE', 'DELETE')  NOT NULL,
    entity_type     VARCHAR(255)                         NOT NULL,
    entity_id       CHAR(36)                             NOT NULL,
    timestamp       DATETIME(3)                          NOT NULL,
    summary         VARCHAR(1000)                        NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_logs_server_seq (server_seq),
    INDEX idx_audit_logs_profile_seq (profile_id, server_seq),
    CONSTRAINT fk_audit_logs_profile
        FOREIGN KEY (profile_id) REFERENCES treatment_profiles (id),
    CONSTRAINT fk_audit_logs_actor_user
        FOREIGN KEY (actor_user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
