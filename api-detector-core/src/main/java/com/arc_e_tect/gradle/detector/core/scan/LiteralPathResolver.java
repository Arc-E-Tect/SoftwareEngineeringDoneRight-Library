package com.arc_e_tect.gradle.detector.core.scan;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort resolution of a JavaParser {@link Expression} to the literal string value it
 * denotes, shared by every scanner in this project that extracts an endpoint path from source
 * (mapping-annotation arguments, request-builder call arguments) - all of which parse one file at
 * a time with no symbol solver and no classpath, by design (see {@link ControllerScanner}'s own
 * javadoc for why: recognising Spring MVC/RestDocs/REST Assured conventions by simple name means
 * none of those libraries needs to be on the scanner's own classpath).
 *
 * <p>Handles, without any out-of-band help, three shapes: a literal itself, and a simple name - a
 * local variable or a field, declared with a literal initializer anywhere in the same compilation
 * unit. Extracting a path into a named constant (e.g.
 * {@code private static final String PATH = "/v1/users/{id}";}, used later as {@code PATH}) is at
 * least as common a practice as writing the literal inline every time, and previously caused every
 * one of its usages to be silently treated the same as a genuinely dynamic path.</p>
 *
 * <p>Two further shapes - a constant computed at runtime by a method call (e.g. a shared
 * {@code ApiEndpoints.get("users.by-username")} helper backed by a properties file), and a field
 * annotated {@code @Value("${users.by-username}")} injected by Spring from an
 * {@code application.properties}/{@code .yml} file - genuinely can't be resolved from source alone
 * without either a classpath or being told, out of band, which method/property key maps to which
 * value. The {@link #resolve(Expression, PropertyResolutionContext)} overload accepts that
 * out-of-band knowledge as a {@link PropertyResolutionContext}; the no-context
 * {@link #resolve(Expression)} overload behaves exactly as before (equivalent to passing
 * {@link PropertyResolutionContext#empty()}) and remains fully backward compatible.</p>
 */
public final class LiteralPathResolver {

    private static final Pattern VALUE_PLACEHOLDER = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}");

    private LiteralPathResolver() {}

    /**
     * Resolves {@code expr} to the literal string it denotes, without any out-of-band property
     * knowledge. Equivalent to {@code resolve(expr, PropertyResolutionContext.empty())}.
     *
     * @param expr the expression to resolve - typically a mapping annotation's {@code value}/
     *             {@code path} member, or a request-builder call's first argument
     * @return the literal value, or empty if {@code expr} is neither a string literal nor a name
     *         that resolves to one within the same compilation unit
     */
    public static Optional<String> resolve(Expression expr) {
        return resolve(expr, PropertyResolutionContext.empty());
    }

    /**
     * Resolves {@code expr} to the literal string it denotes, additionally consulting
     * {@code context} to resolve a configured helper-method call or an {@code @Value}-annotated
     * field against a caller-supplied merged property map.
     *
     * @param expr    the expression to resolve
     * @param context out-of-band property knowledge; pass {@link PropertyResolutionContext#empty()}
     *                if none is available
     * @return the resolved literal value, or empty if it could not be resolved
     */
    public static Optional<String> resolve(Expression expr, PropertyResolutionContext context) {
        if (expr.isStringLiteralExpr()) {
            return Optional.of(expr.asStringLiteralExpr().asString());
        }
        if (expr.isNameExpr()) {
            return resolveName(expr.asNameExpr(), context);
        }
        if (expr.isMethodCallExpr()) {
            return resolveHelperMethodCall(expr.asMethodCallExpr(), context);
        }
        return Optional.empty();
    }

    private static Optional<String> resolveHelperMethodCall(MethodCallExpr call, PropertyResolutionContext context) {
        if (call.getArguments().size() != 1) {
            return Optional.empty();
        }
        String className = call.getScope()
                .filter(Expression::isNameExpr)
                .map(scope -> scope.asNameExpr().getNameAsString())
                .orElse(null);
        if (className == null || !context.isHelperMethod(className, call.getNameAsString())) {
            return Optional.empty();
        }
        return resolve(call.getArgument(0), context).flatMap(context::lookup);
    }

    private static Optional<String> resolveName(NameExpr nameExpr, PropertyResolutionContext context) {
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
        Optional<VariableDeclarator> field = nameExpr.findCompilationUnit()
                .flatMap(cu -> cu.findAll(FieldDeclaration.class).stream()
                        .flatMap(f -> f.getVariables().stream())
                        .filter(v -> v.getNameAsString().equals(name))
                        .findFirst());
        if (field.isEmpty()) {
            return Optional.empty();
        }

        Optional<Expression> initializer = field.flatMap(VariableDeclarator::getInitializer);

        Optional<String> literal = initializer
                .filter(Expression::isStringLiteralExpr)
                .map(init -> init.asStringLiteralExpr().asString());
        if (literal.isPresent()) {
            return literal;
        }

        Optional<String> viaHelperMethod = initializer
                .filter(Expression::isMethodCallExpr)
                .flatMap(init -> resolveHelperMethodCall(init.asMethodCallExpr(), context));
        if (viaHelperMethod.isPresent()) {
            return viaHelperMethod;
        }

        return field.flatMap(v -> v.findAncestor(FieldDeclaration.class))
                .flatMap(f -> resolveValueAnnotation(f, context));
    }

    private static Optional<String> resolveValueAnnotation(FieldDeclaration field, PropertyResolutionContext context) {
        for (AnnotationExpr annotation : field.getAnnotations()) {
            if (!annotation.getNameAsString().equals("Value") || !annotation.isSingleMemberAnnotationExpr()) {
                continue;
            }
            SingleMemberAnnotationExpr valueAnnotation = annotation.asSingleMemberAnnotationExpr();
            if (!valueAnnotation.getMemberValue().isStringLiteralExpr()) {
                continue;
            }
            String raw = valueAnnotation.getMemberValue().asStringLiteralExpr().asString();
            Matcher matcher = VALUE_PLACEHOLDER.matcher(raw);
            if (!matcher.matches()) {
                continue;
            }
            String key = matcher.group(1);
            String fallback = matcher.group(2);
            Optional<String> resolved = context.lookup(key);
            if (resolved.isPresent()) {
                return resolved;
            }
            if (fallback != null) {
                return Optional.of(fallback);
            }
        }
        return Optional.empty();
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
