package com.arc_e_tect.gradle.dslupdater;

/**
 * How a {@link DslPropertySpec} is represented in the DSL block, which determines how
 * {@link DslUpdater} handles it.
 */
public enum DslPropertyKind {

    /** A single {@code name = value} assignment. Added with its default when missing. */
    SCALAR,

    /**
     * A named nested block, typically backed by a {@code NamedDomainObjectContainer} (e.g.
     * {@code trackers { register(...) { ... } } }). There is no single "default" entry to add -
     * the container is only ever included when a whole new block is generated from scratch
     * ({@code --generateDSL} on a project with no block at all), never added to or otherwise
     * touched inside an existing block.
     */
    CONTAINER
}
