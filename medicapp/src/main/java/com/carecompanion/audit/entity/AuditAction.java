package com.carecompanion.audit.entity;

/**
 * Kind of change recorded by an audit log entry. Values must match the
 * {@code action} ENUM literals on audit_logs defined in V1__init_schema.sql
 * exactly.
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE
}
