package com.arc_e_tect.gradle.dslupdater;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds a plugin's DSL properties, missing from a project's build file, set to their default
 * values - the engine behind a plugin's own {@code updateDSL} task.
 *
 * <p>Operates only inside the named extension block's own span; everything outside it, and any
 * content inside it this class doesn't recognize, is preserved byte-for-byte. An existing
 * assignment is never touched, even when its value differs from the schema default - "missing
 * property" is the only thing this class ever adds, which is what makes doing so a no-op.</p>
 *
 * <p>Understands enough Groovy syntax (strings, GString interpolation, comments, nested braces)
 * to find the block and to tell an existing top-level assignment inside it from one that's merely
 * mentioned in a nested block, a string, or a comment - see {@link GroovySyntax}. Kotlin DSL
 * ({@code build.gradle.kts}) is not supported.</p>
 */
public final class DslUpdater {

    private static final String DEFAULT_INDENT = "    ";

    private DslUpdater() {
    }

    /** The outcome of one {@link #update(String, DslExtensionSchema, UpdateDslOptions)} call. */
    public record Outcome(String source, UpdateDslResult result) {
    }

    /**
     * @param originalSource the build file's current text
     * @param schema         the plugin's DSL property schema
     * @param options        which of {@code --generateDSL}/{@code --cleanupDSL} are set
     * @return the (possibly unchanged) new source text, and a summary of what was done
     */
    public static Outcome update(String originalSource, DslExtensionSchema schema, UpdateDslOptions options) {
        GroovySyntax syntax = GroovySyntax.analyze(originalSource);
        Optional<BraceBlockLocator.BlockSpan> located =
                BraceBlockLocator.locate(originalSource, syntax, schema.blockName());

        if (located.isEmpty()) {
            if (!options.generateDsl()) {
                return new Outcome(originalSource, new UpdateDslResult(false, false, List.of(), false));
            }
            List<String> addedFromSchema = scalarNames(schema);
            String generated = renderNewBlock(schema, options.cleanupDsl());
            String updated = appendBlock(originalSource, generated);
            return new Outcome(updated, new UpdateDslResult(false, true, addedFromSchema, false));
        }

        BraceBlockLocator.BlockSpan span = located.get();
        List<Integer> topLevelLineStarts = topLevelLineStarts(originalSource, syntax, span);
        String indent = detectIndent(originalSource, span);

        List<String> added = new ArrayList<>();
        StringBuilder insertion = new StringBuilder();
        for (DslPropertySpec property : schema.properties()) {
            if (property.kind() != DslPropertyKind.SCALAR) {
                continue;
            }
            if (isConfigured(originalSource, topLevelLineStarts, property.name())) {
                continue;
            }
            added.add(property.name());
            if (!options.cleanupDsl() && hasText(property.doc())) {
                insertion.append(indent).append("// ").append(property.doc()).append('\n');
            }
            insertion.append(indent).append(property.name()).append(" = ").append(property.defaultLiteral()).append('\n');
        }

        String withInsertions = originalSource;
        if (!insertion.isEmpty()) {
            String toInsert = insertion.toString();
            if (span.contentEnd() == span.contentStart()
                    || originalSource.charAt(span.contentEnd() - 1) != '\n') {
                toInsert = "\n" + toInsert;
            }
            withInsertions = insertAt(originalSource, span.contentEnd(), toInsert);
        }

        boolean cleaned = false;
        String finalSource = withInsertions;
        if (options.cleanupDsl()) {
            GroovySyntax syntaxAfterInsert = GroovySyntax.analyze(withInsertions);
            BraceBlockLocator.BlockSpan spanAfterInsert =
                    BraceBlockLocator.locate(withInsertions, syntaxAfterInsert, schema.blockName())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Extension block '" + schema.blockName() + "' vanished after inserting properties"));
            String withoutComments = stripCommentLines(withInsertions, syntaxAfterInsert, spanAfterInsert);
            cleaned = !withoutComments.equals(withInsertions);
            finalSource = withoutComments;
        }

        return new Outcome(finalSource, new UpdateDslResult(true, false, added, cleaned));
    }

    private static List<String> scalarNames(DslExtensionSchema schema) {
        List<String> names = new ArrayList<>();
        for (DslPropertySpec property : schema.properties()) {
            if (property.kind() == DslPropertyKind.SCALAR) {
                names.add(property.name());
            }
        }
        return names;
    }

    private static String renderNewBlock(DslExtensionSchema schema, boolean cleanup) {
        StringBuilder sb = new StringBuilder();
        sb.append(schema.blockName()).append(" {\n");
        for (DslPropertySpec property : schema.properties()) {
            if (!cleanup && hasText(property.doc())) {
                sb.append(DEFAULT_INDENT).append("// ").append(property.doc()).append('\n');
            }
            if (property.kind() == DslPropertyKind.SCALAR) {
                sb.append(DEFAULT_INDENT).append(property.name()).append(" = ").append(property.defaultLiteral()).append('\n');
            } else {
                sb.append(DEFAULT_INDENT).append(property.name()).append(" {\n");
                if (!cleanup && hasText(property.containerStub())) {
                    for (String line : property.containerStub().split("\n", -1)) {
                        sb.append(DEFAULT_INDENT).append(DEFAULT_INDENT).append(line).append('\n');
                    }
                }
                sb.append(DEFAULT_INDENT).append("}\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static String appendBlock(String originalSource, String block) {
        if (originalSource.isEmpty()) {
            return block;
        }
        String base = originalSource.endsWith("\n") ? originalSource : originalSource + "\n";
        return base + "\n" + block;
    }

    private static String insertAt(String text, int index, String insertion) {
        return text.substring(0, index) + insertion + text.substring(index);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Start offsets of every line inside the block whose first non-whitespace character sits
     * directly at the block's own nesting depth - i.e. lines that are the block's own properties
     * or container headers, not content belonging to a nested block.
     */
    private static List<Integer> topLevelLineStarts(String text, GroovySyntax syntax, BraceBlockLocator.BlockSpan span) {
        List<Integer> starts = new ArrayList<>();
        int i = span.contentStart();
        while (i < span.contentEnd()) {
            int lineStart = i;
            int lineEnd = lineStart;
            while (lineEnd < span.contentEnd() && text.charAt(lineEnd) != '\n') {
                lineEnd++;
            }
            int firstNonBlank = lineStart;
            while (firstNonBlank < lineEnd && Character.isWhitespace(text.charAt(firstNonBlank))) {
                firstNonBlank++;
            }
            if (firstNonBlank < lineEnd
                    && syntax.isCode(firstNonBlank)
                    && syntax.depthBefore(firstNonBlank) == span.contentDepth()) {
                starts.add(firstNonBlank);
            }
            i = lineEnd + 1;
        }
        return starts;
    }

    private static boolean isConfigured(String text, List<Integer> topLevelLineStarts, String propertyName) {
        Pattern pattern = Pattern.compile(Pattern.quote(propertyName) + "\\s*[=(]");
        for (int start : topLevelLineStarts) {
            Matcher matcher = pattern.matcher(text);
            matcher.region(start, text.length());
            if (matcher.lookingAt()) {
                return true;
            }
        }
        return false;
    }

    private static String detectIndent(String text, BraceBlockLocator.BlockSpan span) {
        int i = span.contentStart();
        while (i < span.contentEnd()) {
            int lineStart = i;
            int lineEnd = lineStart;
            while (lineEnd < span.contentEnd() && text.charAt(lineEnd) != '\n') {
                lineEnd++;
            }
            String line = text.substring(lineStart, lineEnd);
            if (!line.isBlank()) {
                int indentEnd = 0;
                while (indentEnd < line.length() && (line.charAt(indentEnd) == ' ' || line.charAt(indentEnd) == '\t')) {
                    indentEnd++;
                }
                if (indentEnd > 0) {
                    return line.substring(0, indentEnd);
                }
                break;
            }
            i = lineEnd + 1;
        }
        return DEFAULT_INDENT;
    }

    /**
     * Removes every line inside the block whose trimmed content starts with a real (not
     * string-embedded) {@code //}, dropping the line and its terminator entirely. Only whole-line
     * comments are handled - a trailing {@code // comment} on a property line is left alone,
     * since stripping it back to a bare value is a much smaller safety margin for a first pass.
     */
    private static String stripCommentLines(String text, GroovySyntax syntax, BraceBlockLocator.BlockSpan span) {
        StringBuilder result = new StringBuilder(text.length());
        result.append(text, 0, span.contentStart());
        int i = span.contentStart();
        while (i < span.contentEnd()) {
            int lineStart = i;
            int lineEnd = lineStart;
            while (lineEnd < span.contentEnd() && text.charAt(lineEnd) != '\n') {
                lineEnd++;
            }
            String line = text.substring(lineStart, lineEnd);
            String trimmed = line.stripLeading();
            int commentStart = lineStart + (line.length() - trimmed.length());
            boolean isWholeLineComment = trimmed.startsWith("//")
                    && commentStart < text.length()
                    && !syntax.isInString(commentStart);
            if (!isWholeLineComment) {
                result.append(line);
                if (lineEnd < span.contentEnd()) {
                    result.append('\n');
                }
            }
            i = lineEnd + 1;
        }
        result.append(text, span.contentEnd(), text.length());
        return result.toString();
    }
}
