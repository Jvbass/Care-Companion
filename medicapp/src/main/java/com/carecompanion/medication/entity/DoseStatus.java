package com.carecompanion.medication.entity;

/**
 * Outcome of a scheduled dose. Values must match the {@code status} ENUM
 * literals on dose_logs defined in V1__init_schema.sql exactly.
 */
public enum DoseStatus {
    TAKEN,
    SKIPPED,
    POSTPONED
}
