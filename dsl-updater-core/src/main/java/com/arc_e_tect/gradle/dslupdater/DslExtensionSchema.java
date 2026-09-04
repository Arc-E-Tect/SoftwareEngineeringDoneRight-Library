package com.arc_e_tect.gradle.dslupdater;

import java.util.List;
import java.util.Objects;

/**
 * The full, hand-declared property list for one plugin's DSL extension block, in the order
 * properties should appear when a fresh block is generated.
 *
 * <p>This is deliberately not derived by reflection over the extension class: a
 * {@code Property<T>} getter tells you a property's name and type, but its default only exists
 * as a {@code convention(...)} call in the plugin's {@code apply()} method, and reflection can't
 * recover doc comments or the grouping/ordering a human would choose. One {@code DslExtensionSchema}
 * is the single place a plugin author lists all of that by hand.</p>
 */
public final class DslExtensionSchema {

    private final String blockName;
    private final List<DslPropertySpec> properties;

    /**
     * Creates a schema for one plugin's DSL extension block.
     *
     * @param blockName  the extension's DSL block name (e.g. {@code "trackerLens"})
     * @param properties every DSL property {@code updateDSL} should know about, in declaration order
     */
    public DslExtensionSchema(String blockName, List<DslPropertySpec> properties) {
        this.blockName = Objects.requireNonNull(blockName, "blockName");
        this.properties = List.copyOf(Objects.requireNonNull(properties, "properties"));
    }

    /**
     * The extension's DSL block name.
     *
     * @return the DSL block name
     */
    public String blockName() {
        return blockName;
    }

    /**
     * Every DSL property {@code updateDSL} should know about, in declaration order.
     *
     * @return the property list
     */
    public List<DslPropertySpec> properties() {
        return properties;
    }
}
