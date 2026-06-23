package com.carecompanion.medication.entity;

/**
 * Dosing frequency strategy for a medication schedule.
 * Values must match the {@code frequency_type} ENUM literals defined in
 * V1__init_schema.sql exactly.
 */
public enum FrequencyType {
    FIXED_TIMES,
    EVERY_X_HOURS,
    DAYS_OF_WEEK,
    AS_NEEDED
}
