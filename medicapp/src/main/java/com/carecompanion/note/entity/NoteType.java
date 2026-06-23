package com.carecompanion.note.entity;

/**
 * Category of a free-text note. Values must match the {@code type} ENUM
 * literals on notes defined in V1__init_schema.sql exactly.
 */
public enum NoteType {
    SYMPTOM,
    QUESTION
}
