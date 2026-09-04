package com.arc_e_tect.gradle.dslupdater;

/**
 * Flags controlling one {@link DslUpdater#update(String, DslExtensionSchema, UpdateDslOptions)} run.
 *
 * @param generateDsl when the extension block is entirely absent, synthesize a full new one from
 *                    the schema and append it to the end of the file; when {@code false}, a
 *                    missing block is left alone
 * @param cleanupDsl  strip every comment line from inside the managed block - both pre-existing
 *                     ones and any doc comment this run would otherwise add - leaving only
 *                     properties
 */
public record UpdateDslOptions(boolean generateDsl, boolean cleanupDsl) {

    private static final UpdateDslOptions DEFAULTS = new UpdateDslOptions(false, false);

    /**
     * Both flags off - a missing block is left alone, and no comments are stripped.
     *
     * @return the default options
     */
    public static UpdateDslOptions defaults() {
        return DEFAULTS;
    }
}
