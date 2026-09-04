package com.arc_e_tect.gradle.dslupdater;

import java.util.List;

/**
 * What one {@link DslUpdater#update(String, DslExtensionSchema, UpdateDslOptions)} run did, for a
 * caller to log (e.g. under {@code --info}) or to decide whether the file needs rewriting.
 *
 * @param blockFoundBefore whether the extension block already existed before this run
 * @param blockGenerated   whether a whole new block was synthesized ({@code --generateDSL} on a
 *                         project with no block at all)
 * @param addedProperties  names of scalar properties written with their default value in this
 *                         run, in schema order - populated whether they were added to an
 *                         existing block or are part of a freshly generated one; container
 *                         properties are never listed here (see {@link #blockGenerated()} for
 *                         whether a container stub was included in a freshly generated block)
 * @param cleaned          whether {@code --cleanupDSL} actually removed any comment lines
 */
public record UpdateDslResult(boolean blockFoundBefore, boolean blockGenerated, List<String> addedProperties,
                               boolean cleaned) {

    /** Defensively copies {@code addedProperties} into an immutable list. */
    public UpdateDslResult {
        addedProperties = List.copyOf(addedProperties);
    }

    /**
     * Whether this run produced a source file different from the one it was given.
     *
     * @return {@code true} if the block was generated, a property was added, or comments were cleaned
     */
    public boolean changed() {
        return blockGenerated || !addedProperties.isEmpty() || cleaned;
    }
}
