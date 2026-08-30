package com.arc_e_tect.gradle.detector.core.scan;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;

import java.util.Optional;

/**
 * Best-effort resolution of a JavaParser {@link Expression} to the literal string value it
 * denotes, shared by every scanner in this project that extracts an endpoint path from source
 * (mapping-annotation arguments, request-builder call arguments) - all of which parse one file at
 * a time with no symbol solver and no classpath, by design (see {@link ControllerScanner}'s own
 * javadoc for why: recognising Spring MVC/RestDocs/REST Assured conventions by simple name means
 * none of those libraries needs to be on the scanner's own classpath).
 *
 * <p>Handles two shapes: a literal itself, and a simple name - a local variable or a field,
 * declared with a literal initializer anywhere in the same compilation unit. Extracting a path
 * into a named constant (e.g. {@code private static final String PATH = "/v1/users/{id}";}, used
 * later as {@code PATH}) is at least as common a practice as writing the literal inline every
 * time, and previously caused every one of its usages to be silently treated the same as a
 * genuinely dynamic path.</p>
 *
 * <p>This does <strong>not</strong> resolve a name to a value computed at runtime - a constant
 * initialized by a method call (e.g. a shared {@code ApiEndpoints.get("users.by-username")}
 * helper backed by a properties file) still can't be resolved without either a classpath or being
 * told, out of band, which method reads which file. That is a structurally different problem this
 * class deliberately does not attempt to solve.</p>
 */
public final class LiteralPathResolver {

    private LiteralPathResolver() {}

    /**
     * Resolves {@code expr} to the literal string it denotes.
     *
     * @param expr the expression to resolve - typically a mapping annotation's {@code value}/
     *             {@code path} member, or a request-builder call's first argument
     * @return the literal value, or empty if {@code expr} is neither a string literal nor a name
     *         that resolves to one within the same compilation unit
     */
    public static Optional<String> resolve(Expression expr) {
        if (expr.isStringLiteralExpr()) {
            return Optional.of(expr.asStringLiteralExpr().asString());
        }
        if (expr.isNameExpr()) {
            return resolveName(expr.asNameExpr());
        }
        return Optional.empty();
    }

    private static Optional<String> resolveName(NameExpr nameExpr) {
        String name = nameExpr.getNameAsString();

        Optional<String> local = nameExpr.findAncestor(MethodDeclaration.class)
                .flatMap(method -> literalInitializer(method.findAll(VariableDeclarator.class), name));
        if (local.isPresent()) {
            return local;
        }

        // Falls back to every field in the whole file rather than just the enclosing type's own -
        // deliberately lenient rather than reimplementing Java's inheritance/visibility rules for a
        // best-effort heuristic. This is what resolves the shared-contract-test-interface
        // convention this class was written for: a constant field declared on an interface,
        // referenced from that same interface's own default method, both in the same file.
        return nameExpr.findCompilationUnit()
                .flatMap(cu -> literalInitializer(
                        cu.findAll(FieldDeclaration.class).stream()
                                .flatMap(f -> f.getVariables().stream())
                                .toList(),
                        name));
    }

    private static Optional<String> literalInitializer(Iterable<VariableDeclarator> candidates, String name) {
        for (VariableDeclarator candidate : candidates) {
            if (!candidate.getNameAsString().equals(name)) {
                continue;
            }
            Optional<String> literal = candidate.getInitializer()
                    .filter(Expression::isStringLiteralExpr)
                    .map(init -> init.asStringLiteralExpr().asString());
            if (literal.isPresent()) {
                return literal;
            }
        }
        return Optional.empty();
    }
}
