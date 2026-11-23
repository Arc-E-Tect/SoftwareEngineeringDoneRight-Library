package com.arc_e_tect.gradle.detector.core.scan;

import com.arc_e_tect.gradle.detector.core.model.Endpoint;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.model.PathTemplates;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 *
 * <p>Like Spring MVC, a controller also serves the mappings it inherits from the interfaces it
 * implements and the classes it extends, which is how a controller implementing an interface
 * generated from an OpenAPI contract is written. {@link #scan(Collection)} parses every given file
 * first, so a supertype declared in another scanned file is found; the controller's own
 * annotations win over inherited ones, and otherwise the first supertype that carries one does,
 * interfaces before the superclass. An inherited endpoint is reported on the controller class, at
 * the controller's overriding method or, when there is none, at the inherited method itself.</p>
 *
 * <p>A path given as another scanned type's string constant ({@code BooksApi.PATH_GET_BOOK})
 * resolves to that constant's literal. A {@code ${key:default}} placeholder resolves to the
 * property {@code key} from the scanner's {@link PropertyResolutionContext}, else to its default;
 * with neither, the value contributes no path, like any other value that cannot be resolved.</p>
 */
public class ControllerScanner {

    private static final Map<String, HttpVerb> SHORTCUT_MAPPING_VERBS = Map.of(
            "GetMapping", HttpVerb.GET,
            "PostMapping", HttpVerb.POST,
            "PutMapping", HttpVerb.PUT,
            "DeleteMapping", HttpVerb.DELETE,
            "PatchMapping", HttpVerb.PATCH);

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}");

    private final PropertyResolutionContext context;

    /** Creates a new {@code ControllerScanner} without any out-of-band property knowledge. */
    public ControllerScanner() {
        this(PropertyResolutionContext.empty());
    }

    /**
     * Creates a new {@code ControllerScanner} that resolves {@code ${key}} placeholders and
     * qualified constants in mapping paths through {@code context}.
     *
     * @param context out-of-band property knowledge; its properties win over the string constants
     *                found in the scanned files
     */
    public ControllerScanner(PropertyResolutionContext context) {
        this.context = context;
    }

    /**
     * Scans one {@code .java} source file and returns every endpoint exposed by every
     * {@code @RestController} class it declares. Equivalent to {@code scan(List.of(sourceFile))}:
     * mappings inherited from a supertype declared in another file are not found.
     *
     * @param sourceFile the file to parse
     * @return possibly-empty list of endpoints, never {@code null}
     * @throws IOException if the file cannot be read
     */
    public List<Endpoint> scan(File sourceFile) throws IOException {
        return scan(List.of(sourceFile));
    }

    /**
     * Scans {@code .java} source files together and returns every endpoint exposed by every
     * {@code @RestController} class they declare, including the mappings each controller inherits
     * from supertypes declared in any of the files. A file that is not valid Java is skipped.
     *
     * @param sourceFiles the files to parse, typically every file of the main source set, generated
     *                    sources included
     * @return possibly-empty list of endpoints, in file order, never {@code null}
     * @throws IOException if a file cannot be read
     */
    public List<Endpoint> scan(Collection<File> sourceFiles) throws IOException {
        return scan(sourceFiles, file -> { });
    }

    /**
     * Scans {@code .java} source files together, as {@link #scan(Collection)} does, and tells
     * {@code onFileParsed} about each file once it is parsed.
     *
     * <p>Parsing every file is most of the work, and it all happens inside this one call, before
     * any endpoint can be resolved; the callback lets a caller drive a determinate progress
     * indicator through it. It is invoked once per file, in the order given, for a file that is not
     * valid Java too, and before any endpoint is resolved.</p>
     *
     * @param sourceFiles  the files to parse, typically every file of the main source set,
     *                     generated sources included
     * @param onFileParsed invoked once per file, after it is parsed; never {@code null}
     * @return possibly-empty list of endpoints, in file order, never {@code null}
     * @throws IOException if a file cannot be read
     */
    public List<Endpoint> scan(Collection<File> sourceFiles, Consumer<File> onFileParsed) throws IOException {
        JavaParser parser = new JavaParser(
                new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
        List<TypeIndex.ParsedSource> sources = new ArrayList<>();
        for (File sourceFile : sourceFiles) {
            ParseResult<CompilationUnit> parseResult = parser.parse(sourceFile);
            if (parseResult.isSuccessful()) {
                sources.add(new TypeIndex.ParsedSource(sourceFile.getName(), parseResult.getResult().orElseThrow()));
            }
            onFileParsed.accept(sourceFile);
        }

        TypeIndex index = new TypeIndex(sources);
        PropertyResolutionContext resolution = context.withFallbacks(index.stringConstants());
        List<Endpoint> endpoints = new ArrayList<>();
        for (TypeIndex.IndexedType type : index.types()) {
            if (hasAnnotation(type.declaration(), "RestController")) {
                endpoints.addAll(endpointsForController(type, index.hierarchy(type), resolution));
            }
        }
        return endpoints;
    }

    private List<Endpoint> endpointsForController(
            TypeIndex.IndexedType controller, List<TypeIndex.IndexedType> hierarchy,
            PropertyResolutionContext resolution) {
        List<String> basePaths = classBasePaths(hierarchy, resolution);

        // One entry per method (by name and parameter count, which tolerates a generic supertype's
        // type parameters): the most specific declaration, and the first mapping in the hierarchy.
        Map<String, InheritedMethod> methods = new LinkedHashMap<>();
        for (TypeIndex.IndexedType type : hierarchy) {
            for (MethodDeclaration method : type.declaration().getMethods()) {
                String key = method.getNameAsString() + "/" + method.getParameters().size();
                MappingInfo mapping = findMapping(method, resolution);
                InheritedMethod known = methods.get(key);
                if (known == null) {
                    methods.put(key, new InheritedMethod(method, type.fileName(), mapping));
                } else if (known.mapping() == null && mapping != null) {
                    methods.put(key, new InheritedMethod(known.method(), known.fileName(), mapping));
                }
            }
        }

        List<Endpoint> endpoints = new ArrayList<>();
        for (InheritedMethod method : methods.values()) {
            if (method.mapping() != null) {
                endpoints.addAll(endpointsForMethod(
                        method.method(), method.mapping(), controller.fqcn(), method.fileName(), basePaths));
            }
        }
        return endpoints;
    }

    private List<Endpoint> endpointsForMethod(
            MethodDeclaration method, MappingInfo mapping, String declaringClass, String fileName,
            List<String> basePaths) {
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

    private List<String> classBasePaths(List<TypeIndex.IndexedType> hierarchy, PropertyResolutionContext resolution) {
        for (TypeIndex.IndexedType type : hierarchy) {
            for (AnnotationExpr ann : type.declaration().getAnnotations()) {
                if (simpleAnnotationName(ann).equals("RequestMapping")) {
                    List<String> paths = pathValues(ann, resolution);
                    return paths.isEmpty() ? List.of("") : paths;
                }
            }
        }
        return List.of("");
    }

    /**
     * Finds the mapping annotation (if any) on {@code node}, resolving its paths and, for
     * {@code @RequestMapping}, its restricted verbs.
     */
    private MappingInfo findMapping(NodeWithAnnotations<?> node, PropertyResolutionContext resolution) {
        for (AnnotationExpr ann : node.getAnnotations()) {
            String simpleName = simpleAnnotationName(ann);
            if (simpleName.equals("RequestMapping")) {
                return new MappingInfo(pathValues(ann, resolution), verbValues(ann));
            }
            HttpVerb fixedVerb = SHORTCUT_MAPPING_VERBS.get(simpleName);
            if (fixedVerb != null) {
                return new MappingInfo(pathValues(ann, resolution), List.of(fixedVerb));
            }
        }
        return null;
    }

    private List<String> pathValues(AnnotationExpr ann, PropertyResolutionContext resolution) {
        if (ann.isSingleMemberAnnotationExpr()) {
            return extractStringValues(ann.asSingleMemberAnnotationExpr().getMemberValue(), resolution);
        }
        if (ann.isNormalAnnotationExpr()) {
            for (MemberValuePair pair : ann.asNormalAnnotationExpr().getPairs()) {
                String name = pair.getNameAsString();
                if (name.equals("value") || name.equals("path")) {
                    return extractStringValues(pair.getValue(), resolution);
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

    private List<String> extractStringValues(Expression expr, PropertyResolutionContext resolution) {
        List<String> values = new ArrayList<>();
        if (expr.isArrayInitializerExpr()) {
            expr.asArrayInitializerExpr().getValues().forEach(e -> values.addAll(extractStringValues(e, resolution)));
        } else {
            LiteralPathResolver.resolve(expr, resolution)
                    .flatMap(value -> resolvePlaceholders(value, resolution))
                    .ifPresent(values::add);
        }
        // Anything that can't be resolved (e.g. a value computed at runtime, a constant declared
        // in a type that was not scanned, or a placeholder without a property and without a
        // default) is silently ignored - the annotation contributes no path in that case.
        return values;
    }

    private Optional<String> resolvePlaceholders(String value, PropertyResolutionContext resolution) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder resolved = new StringBuilder();
        while (matcher.find()) {
            Optional<String> replacement = resolution.lookup(matcher.group(1))
                    .or(() -> Optional.ofNullable(matcher.group(2)));
            if (replacement.isEmpty()) {
                return Optional.empty();
            }
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement.get()));
        }
        matcher.appendTail(resolved);
        return Optional.of(resolved.toString());
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

    private String buildParams(NodeList<Parameter> params) {
        return params.stream().map(p -> p.getType().asString()).collect(Collectors.joining(", "));
    }

    /** Paths and restricted verbs resolved from a single mapping annotation. */
    private record MappingInfo(List<String> paths, List<HttpVerb> verbs) {}

    /** A method a controller serves: its most specific declaration and the mapping it inherits. */
    private record InheritedMethod(MethodDeclaration method, String fileName, MappingInfo mapping) {}
}
