package com.carecompanion.profile.entity;

/**
 * Membership role of a user within a treatment profile.
 * Values must match the {@code role} / {@code default_role} ENUM literals
 * defined in V1__init_schema.sql exactly.
 */
public enum Role {
    OWNER,
    CAREGIVER,
    VIEWER
}
