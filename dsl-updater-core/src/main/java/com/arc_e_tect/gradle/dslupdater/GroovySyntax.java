package com.arc_e_tect.gradle.dslupdater;

/**
 * Minimal structural scan of a Groovy source file: for every character index it records the
 * brace-nesting depth immediately before that character, whether the character is "real code"
 * (not inside a string literal or a comment), and whether it is inside a string literal
 * specifically. This is not a Groovy parser - it only tracks enough to find top-level
 * {@code name { ... }} blocks and to tell a genuine {@code //} comment from one that merely
 * appears inside a string.
 *
 * <p>Handles single/double-quoted strings (with backslash escaping), triple-quoted strings,
 * {@code ${...}} interpolation inside double-quoted GStrings (braces inside an interpolation are
 * tracked as real code, so they participate in depth counting), line comments, and block
 * comments. Slashy regex strings ({@code /.../}) are not recognized - build files essentially
 * never use them, and misreading one as a comment-start would be a worse failure mode than
 * simply not supporting them.</p>
 */
final class GroovySyntax {

    private final String text;
    private final int[] depthBefore;
    private final boolean[] isCode;
    private final boolean[] inString;

    private GroovySyntax(String text, int[] depthBefore, boolean[] isCode, boolean[] inString) {
        this.text = text;
        this.depthBefore = depthBefore;
        this.isCode = isCode;
        this.inString = inString;
    }

    static GroovySyntax analyze(String text) {
        int n = text.length();
        int[] depthBefore = new int[n + 1];
        boolean[] isCode = new boolean[n];
        boolean[] inString = new boolean[n];

        int depth = 0;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        char stringQuote = 0;
        boolean tripleQuoted = false;
        boolean inInterpolation = false;
        int interpolationDepth = 0;

        int i = 0;
        while (i < n) {
            depthBefore[i] = depth;
            char c = text.charAt(i);

            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                }
                i++;
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && i + 1 < n && text.charAt(i + 1) == '/') {
                    depthBefore[i + 1] = depth;
                    i += 2;
                    inBlockComment = false;
                    continue;
                }
                i++;
                continue;
            }
            if (stringQuote != 0 && !inInterpolation) {
                inString[i] = true;
                if (c == '\\' && !tripleQuoted && i + 1 < n) {
                    depthBefore[i + 1] = depth;
                    inString[Math.min(i + 1, n - 1)] = true;
                    i += 2;
                    continue;
                }
                if (stringQuote == '"' && c == '$' && i + 1 < n && text.charAt(i + 1) == '{') {
                    isCode[i] = true;
                    isCode[i + 1] = true;
                    depthBefore[i + 1] = depth;
                    depth++;
                    inInterpolation = true;
                    interpolationDepth = depth - 1;
                    i += 2;
                    continue;
                }
                if (tripleQuoted) {
                    if (i + 2 < n && text.charAt(i + 1) == stringQuote && text.charAt(i + 2) == stringQuote) {
                        inString[i + 1] = true;
                        inString[i + 2] = true;
                        i += 3;
                        stringQuote = 0;
                        tripleQuoted = false;
                        continue;
                    }
                    i++;
                    continue;
                }
                if (c == stringQuote) {
                    stringQuote = 0;
                    i++;
                    continue;
                }
                i++;
                continue;
            }

            // Real code: not in a comment, not in a (non-interpolation) string.
            isCode[i] = true;

            if (inInterpolation) {
                if (c == '{') {
                    depth++;
                    i++;
                    continue;
                }
                if (c == '}') {
                    depth--;
                    i++;
                    if (depth == interpolationDepth) {
                        inInterpolation = false;
                        stringQuote = '"';
                    }
                    continue;
                }
            }

            if (c == '/' && i + 1 < n && text.charAt(i + 1) == '/') {
                inLineComment = true;
                isCode[i] = false;
                i++;
                continue;
            }
            if (c == '/' && i + 1 < n && text.charAt(i + 1) == '*') {
                inBlockComment = true;
                isCode[i] = false;
                i += 2;
                continue;
            }
            if (c == '\'' || c == '"') {
                boolean triple = i + 2 < n && text.charAt(i + 1) == c && text.charAt(i + 2) == c;
                stringQuote = c;
                tripleQuoted = triple;
                inString[i] = true;
                i += triple ? 3 : 1;
                continue;
            }
            if (c == '{') {
                depth++;
                i++;
                continue;
            }
            if (c == '}') {
                depth--;
                i++;
                continue;
            }
            i++;
        }
        depthBefore[n] = depth;

        return new GroovySyntax(text, depthBefore, isCode, inString);
    }

    int depthBefore(int index) {
        return depthBefore[index];
    }

    boolean isCode(int index) {
        return isCode[index];
    }

    boolean isInString(int index) {
        return inString[index];
    }

    int length() {
        return text.length();
    }
}
