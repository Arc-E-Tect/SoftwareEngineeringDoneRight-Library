package com.arc_e_tect.gradle.detector.core.progress;

import java.time.Instant;

/**
 * One entry in a contract history file's persisted migration audit trail - see
 * {@link ContractHistoryStore}'s own javadoc for when this is recorded automatically versus when
 * a file instead needs the explicit, manually-invoked migration a structural (field-shape) change
 * requires.
 *
 * @param fromVersion the {@code schemaVersion} the file was at before this migration
 * @param toVersion   the {@code schemaVersion} the file was upgraded to by this migration
 * @param migratedAt  when this migration was performed
 */
public record SchemaMigration(String fromVersion, String toVersion, Instant migratedAt) {
}
