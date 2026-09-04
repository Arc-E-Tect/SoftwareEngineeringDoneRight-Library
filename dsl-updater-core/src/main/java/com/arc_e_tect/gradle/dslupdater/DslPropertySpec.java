package com.arc_e_tect.gradle.dslupdater;

import java.util.Objects;

/**
 * Describes one property of a plugin's DSL extension for {@link DslUpdater}: enough to recognize
 * an existing assignment for it in a build file, and to render one when it's missing.
 *
 * <p>The default literal here is Groovy source text, not a Java value - e.g. {@code "\"light-lens\""}
 * for a {@code String} default of {@code light-lens}, or {@code "file('dashboard-template.html')"}
 * for a {@code RegularFileProperty}. Callers should build it from the same constant their
 * extension's own {@code convention(...)} call uses, so the two can never drift apart - the whole
 * point of {@code updateDSL} is that setting an added property explicitly is a no-op.</p>
 */
public final class DslPropertySpec {

    private final String name;
    private final DslPropertyKind kind;
    private final String defaultLiteral;
    private final String doc;
    private final String containerStub;

    private DslPropertySpec(String name, DslPropertyKind kind, String defaultLiteral, String doc, String containerStub) {
        this.name = Objects.requireNonNull(name, "name");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.defaultLiteral = defaultLiteral;
        this.doc = doc;
        this.containerStub = containerStub;
    }

    /**
     * A plain {@code name = value} property.
     *
     * @param name           the property name, exactly as it appears in the DSL
     * @param defaultLiteral its default value, as literal Groovy source (e.g. {@code "true"},
     *                       {@code "\"light-lens\""}, {@code "file('x.css')"})
     * @param doc            one-line doc comment to place above a generated assignment, or
     *                       {@code null} for none
     * @return a scalar property spec
     */
    public static DslPropertySpec scalar(String name, String defaultLiteral, String doc) {
        Objects.requireNonNull(defaultLiteral, "defaultLiteral");
        return new DslPropertySpec(name, DslPropertyKind.SCALAR, defaultLiteral, doc, null);
    }

    /**
     * A named nested container block (see {@link DslPropertyKind#CONTAINER}).
     *
     * @param name          the block name, exactly as it appears in the DSL
     * @param doc           one-line doc comment to place above the block when a whole new
     *                      extension block is generated, or {@code null} for none
     * @param containerStub example content placed inside the block when a whole new extension
     *                      block is generated (typically a commented-out {@code register(...)}
     *                      call), or {@code null} for an empty block
     * @return a container property spec
     */
    public static DslPropertySpec container(String name, String doc, String containerStub) {
        return new DslPropertySpec(name, DslPropertyKind.CONTAINER, null, doc, containerStub);
    }

    /**
     * The property name, exactly as it appears in the DSL.
     *
     * @return the property name
     */
    public String name() {
        return name;
    }

    /**
     * Whether this is a plain scalar assignment or a nested container block.
     *
     * @return the property kind
     */
    public DslPropertyKind kind() {
        return kind;
    }

    /**
     * The default value, as literal Groovy source. {@code null} for a {@link DslPropertyKind#CONTAINER}.
     *
     * @return the default literal, or {@code null}
     */
    public String defaultLiteral() {
        return defaultLiteral;
    }

    /**
     * One-line doc comment to place above a generated assignment or block.
     *
     * @return the doc comment, or {@code null} for none
     */
    public String doc() {
        return doc;
    }

    /**
     * Example content placed inside a freshly generated container block. {@code null} for a
     * {@link DslPropertyKind#SCALAR}.
     *
     * @return the container stub text, or {@code null}
     */
    public String containerStub() {
        return containerStub;
    }
}
