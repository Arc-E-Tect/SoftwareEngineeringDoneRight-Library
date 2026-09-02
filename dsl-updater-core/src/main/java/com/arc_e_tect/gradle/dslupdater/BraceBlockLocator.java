package com.arc_e_tect.gradle.dslupdater;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds a named top-level {@code name { ... }} block in a Groovy source file. */
final class BraceBlockLocator {

    private BraceBlockLocator() {
    }

    /**
     * A located block's key offsets, all relative to the source text the locator was called with.
     *
     * @param headerStart  index of the block name's first character
     * @param contentStart index of the first character after the opening {@code {}
     * @param contentEnd   index of the matching closing {@code }}
     * @param blockEnd     index just past the closing {@code }}
     * @param contentDepth brace-nesting depth of the block's own content (its properties sit at
     *                     this depth; anything nested deeper, e.g. inside a container block,
     *                     does not)
     */
    record BlockSpan(int headerStart, int contentStart, int contentEnd, int blockEnd, int contentDepth) {
    }

    static Optional<BlockSpan> locate(String text, GroovySyntax syntax, String blockName) {
        Pattern header = Pattern.compile("(?<![\\w.])" + Pattern.quote(blockName) + "\\s*\\{");
        Matcher matcher = header.matcher(text);
        while (matcher.find()) {
            int nameStart = matcher.start();
            int braceIndex = matcher.end() - 1;
            if (!syntax.isCode(nameStart) || !syntax.isCode(braceIndex)) {
                continue;
            }
            if (syntax.depthBefore(nameStart) != 0) {
                continue;
            }
            int contentEnd = findMatchingClose(text, syntax, braceIndex);
            if (contentEnd < 0) {
                continue;
            }
            int contentStart = braceIndex + 1;
            return Optional.of(new BlockSpan(nameStart, contentStart, contentEnd, contentEnd + 1,
                    syntax.depthBefore(contentStart)));
        }
        return Optional.empty();
    }

    private static int findMatchingClose(String text, GroovySyntax syntax, int openBraceIndex) {
        int depth = 1;
        for (int i = openBraceIndex + 1; i < text.length(); i++) {
            if (!syntax.isCode(i)) {
                continue;
            }
            char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
