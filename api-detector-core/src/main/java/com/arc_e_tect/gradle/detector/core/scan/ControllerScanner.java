package com.arc_e_tect.gradle.detector.core.scan;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.model.PathTemplates;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parses Java source files and collects every HTTP endpoint exposed by a Spring
 * {@code @RestController} class.
 *
 * <p>Recognises the class-level {@code @RequestMapping} base path plus the standard method-level
 * mapping annotations: {@code @RequestMapping}, {@code @GetMapping}, {@code @PostMapping},
 * {@code @PutMapping}, {@code @DeleteMapping}, and {@code @PatchMapping}. When a
 * {@code @RequestMapping} method annotation does not restrict its {@code method} attribute, the
 * resulting {@link Endpoint} carries {@link HttpVerb#ANY}.</p>
 *
 * <p>Annotations are matched by simple name, so both the standard
 * {@code org.springframework.web.bind.annotation} imports and any fully-qualified usage are
 * recognised without requiring Spring on the scanner's own classpath.</p>
 */
public class ControllerScanner {

    private static final Map<String, HttpVerb> SHORTCUT_MAPPING_VERBS = Map.of(
            "GetMapping", HttpVerb.GET,
            "PostMapping", HttpVerb.POST,
            "PutMapping", HttpVerb.PUT,
            "DeleteMapping", HttpVerb.DELETE,
            "PatchMapping", HttpVerb.PATCH);

    /** Creates a new {@code ControllerScanner}. */
    public ControllerScanner() {}

    /**
     * Scans one {@code .java} source file and returns every endpoint exposed by every
     * {@code @RestController} class it declares.
     *
     * @param sourceFile the file to parse
     * @return possibly-empty list of endpoints, never {@code null}
     * @throws IOException if the file cannot be read
     */
    public List<Endpoint> scan(File sourceFile) throws IOException {
        List<Endpoint> endpoints = new ArrayList<>();

        ParseResult<CompilationUnit> parseResult = new JavaParser(
                new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21))
                .parse(sourceFile);
        if (!parseResult.isSuccessful() || parseResult.getResult().isEmpty()) {
            return endpoints;
        }

        CompilationUnit cu = parseResult.getResult().get();
        String fileName = sourceFile.getName();

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
            if (!hasAnnotation(cls, "RestController")) {
                return;
            }
            String declaringClass = buildFqcn(cu, cls);
            List<String> basePaths = classBasePaths(cls);

            cls.getMethods().forEach(method ->
                    endpoints.addAll(endpointsForMethod(method, declaringClass, fileName, basePaths)));
        });

        return endpoints;
    }

    private List<Endpoint> endpointsForMethod(
            MethodDeclaration method, String declaringClass, String fileName, List<String> basePaths) {
        MappingInfo mapping = findMapping(method);
        if (mapping == null) {
            return List.of();
        }

        List<String> methodPaths = mapping.paths().isEmpty() ? List.of("") : mapping.paths();
        List<HttpVerb> verbs = mapping.verbs().isEmpty() ? List.of(HttpVerb.ANY) : mapping.verbs();
        String signature = method.getNameAsString() + "(" + buildParams(method.getParameters()) + ")";
        int line = method.getBegin().map(p -> p.line).orElse(0);

        List<Endpoint> results = new ArrayList<>();
        for (String basePath : basePaths) {
            for (String methodPath : methodPaths) {
                String path = PathTemplates.join(basePath, methodPath);
                for (HttpVerb verb : verbs) {
                    results.add(new Endpoint(verb, path, declaringClass, signature, fileName, line));
                }
            }
        }
        return results;
    }

    private List<String> classBasePaths(ClassOrInterfaceDeclaration cls) {
        for (AnnotationExpr ann : cls.getAnnotations()) {
            if (simpleAnnotationName(ann).equals("RequestMapping")) {
                List<String> paths = pathValues(ann);
                return paths.isEmpty() ? List.of("") : paths;
            }
        }
        return List.of("");
    }

    /**
     * Finds the mapping annotation (if any) on {@code node}, resolving its paths and, for
     * {@code @RequestMapping}, its restricted verbs.
     */
    private MappingInfo findMapping(NodeWithAnnotations<?> node) {
        for (AnnotationExpr ann : node.getAnnotations()) {
            String simpleName = simpleAnnotationName(ann);
            if (simpleName.equals("RequestMapping")) {
                return new MappingInfo(pathValues(ann), verbValues(ann));
            }
            HttpVerb fixedVerb = SHORTCUT_MAPPING_VERBS.get(simpleName);
            if (fixedVerb != null) {
                return new MappingInfo(pathValues(ann), List.of(fixedVerb));
            }
        }
        return null;
    }

    private List<String> pathValues(AnnotationExpr ann) {
        if (ann.isSingleMemberAnnotationExpr()) {
            return extractStringValues(ann.asSingleMemberAnnotationExpr().getMemberValue());
        }
        if (ann.isNormalAnnotationExpr()) {
            for (MemberValuePair pair : ann.asNormalAnnotationExpr().getPairs()) {
                String name = pair.getNameAsString();
                if (name.equals("value") || name.equals("path")) {
                    return extractStringValues(pair.getValue());
                }
            }
        }
        return List.of();
    }

    private List<HttpVerb> verbValues(AnnotationExpr ann) {
        if (ann.isNormalAnnotationExpr()) {
            for (MemberValuePair pair : ann.asNormalAnnotationExpr().getPairs()) {
                if (pair.getNameAsString().equals("method")) {
                    return extractVerbs(pair.getValue());
                }
            }
        }
        return List.of();
    }

    private List<String> extractStringValues(Expression expr) {
        List<String> values = new ArrayList<>();
        if (expr.isArrayInitializerExpr()) {
            expr.asArrayInitializerExpr().getValues().forEach(e -> values.addAll(extractStringValues(e)));
        } else if (expr.isStringLiteralExpr()) {
            values.add(expr.asStringLiteralExpr().asString());
        }
        // Anything else (e.g. a reference to a constant) cannot be resolved without a
        // classpath and is silently ignored - the annotation contributes no path in that case.
        return values;
    }

    private List<HttpVerb> extractVerbs(Expression expr) {
        List<HttpVerb> verbs = new ArrayList<>();
        if (expr.isArrayInitializerExpr()) {
            expr.asArrayInitializerExpr().getValues().forEach(e -> verbs.addAll(extractVerbs(e)));
        } else if (expr.isFieldAccessExpr()) {
            verbs.add(HttpVerb.fromSpringRequestMethod(expr.asFieldAccessExpr().getNameAsString()));
        } else if (expr.isNameExpr()) {
            verbs.add(HttpVerb.fromSpringRequestMethod(expr.asNameExpr().getNameAsString()));
        }
        return verbs;
    }

    private boolean hasAnnotation(NodeWithAnnotations<?> node, String simpleName) {
        return node.getAnnotations().stream().anyMatch(a -> simpleAnnotationName(a).equals(simpleName));
    }

    private String simpleAnnotationName(AnnotationExpr ann) {
        String name = ann.getNameAsString();
        int lastDot = name.lastIndexOf('.');
        return lastDot >= 0 ? name.substring(lastDot + 1) : name;
    }

    private String buildFqcn(CompilationUnit cu, ClassOrInterfaceDeclaration cls) {
        String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
        String nestedName = buildNestedName(cls);
        return pkg.isEmpty() ? nestedName : pkg + "." + nestedName;
    }

    private String buildNestedName(ClassOrInterfaceDeclaration cls) {
        if (cls.getParentNode().isPresent()
                && cls.getParentNode().get() instanceof ClassOrInterfaceDeclaration parent) {
            return buildNestedName(parent) + "." + cls.getNameAsString();
        }
        return cls.getNameAsString();
    }

    private String buildParams(NodeList<Parameter> params) {
        return params.stream().map(p -> p.getType().asString()).collect(Collectors.joining(", "));
    }

    /** Paths and restricted verbs resolved from a single mapping annotation. */
    private record MappingInfo(List<String> paths, List<HttpVerb> verbs) {}
}
