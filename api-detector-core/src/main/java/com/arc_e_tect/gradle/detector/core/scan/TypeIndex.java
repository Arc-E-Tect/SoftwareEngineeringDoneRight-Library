package com.arc_e_tect.gradle.detector.core.scan;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Every class and interface declared in the source files of one scan, by fully qualified name, so
 * a type's supertypes can be followed across files without a classpath or a symbol solver.
 *
 * <p>A supertype reference is resolved the way the Java compiler looks up a simple name, as far
 * as source alone allows: a type declared in the same file, a single-type import, the file's own
 * package, then on-demand ({@code .*}) imports. A fully qualified or enclosing-type-qualified
 * reference ({@code Outer.Inner}) is resolved as well. A supertype that is not among the scanned
 * files, such as {@code java.io.Serializable}, simply resolves to nothing.</p>
 */
final class TypeIndex {

    /** A parsed source file. */
    record ParsedSource(String fileName, CompilationUnit compilationUnit) {}

    /** A class or interface declared in one of the scanned files. */
    record IndexedType(String fqcn, ClassOrInterfaceDeclaration declaration, CompilationUnit compilationUnit,
                       String fileName) {}

    private final Map<String, IndexedType> typesByFqcn = new LinkedHashMap<>();

    TypeIndex(List<ParsedSource> sources) {
        for (ParsedSource source : sources) {
            CompilationUnit cu = source.compilationUnit();
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(type ->
                    typesByFqcn.putIfAbsent(fqcn(cu, type), new IndexedType(fqcn(cu, type), type, cu, source.fileName())));
        }
    }

    /** @return every indexed type, in file order and, within a file, in declaration order. */
    Collection<IndexedType> types() {
        return typesByFqcn.values();
    }

    /**
     * @return {@code type} itself followed by every supertype found among the scanned files, in the
     *         order Spring MVC searches them for a mapping annotation: a type's interfaces (each
     *         followed by its own super-interfaces) before its superclass; each type once
     */
    List<IndexedType> hierarchy(IndexedType type) {
        Set<IndexedType> visited = new LinkedHashSet<>();
        visit(type, visited);
        return new ArrayList<>(visited);
    }

    /**
     * @return every field with a string-literal initializer, keyed {@code "SimpleTypeName.FIELD"}
     *         as {@link PropertyResolutionContext} expects a qualified constant; when two scanned
     *         types share a simple name, the first one's constant is kept
     */
    Map<String, String> stringConstants() {
        Map<String, String> constants = new LinkedHashMap<>();
        for (IndexedType type : typesByFqcn.values()) {
            for (FieldDeclaration field : type.declaration().getFields()) {
                for (VariableDeclarator variable : field.getVariables()) {
                    variable.getInitializer()
                            .filter(Expression::isStringLiteralExpr)
                            .ifPresent(init -> constants.putIfAbsent(
                                    type.declaration().getNameAsString() + "." + variable.getNameAsString(),
                                    init.asStringLiteralExpr().asString()));
                }
            }
        }
        return constants;
    }

    private void visit(IndexedType type, Set<IndexedType> visited) {
        if (!visited.add(type)) {
            return;
        }
        ClassOrInterfaceDeclaration declaration = type.declaration();
        List<ClassOrInterfaceType> supertypes = new ArrayList<>(declaration.getImplementedTypes());
        supertypes.addAll(declaration.getExtendedTypes());
        for (ClassOrInterfaceType supertype : supertypes) {
            resolve(supertype.getNameWithScope(), type).ifPresent(resolved -> visit(resolved, visited));
        }
    }

    private Optional<IndexedType> resolve(String name, IndexedType from) {
        IndexedType qualified = typesByFqcn.get(name);
        if (qualified != null) {
            return Optional.of(qualified);
        }
        int firstDot = name.indexOf('.');
        String simpleName = firstDot < 0 ? name : name.substring(0, firstDot);
        String nestedRest = firstDot < 0 ? "" : name.substring(firstDot);
        return resolveSimpleName(simpleName, from)
                .flatMap(outer -> Optional.ofNullable(typesByFqcn.get(outer.fqcn() + nestedRest)));
    }

    private Optional<IndexedType> resolveSimpleName(String simpleName, IndexedType from) {
        CompilationUnit cu = from.compilationUnit();
        for (IndexedType candidate : typesByFqcn.values()) {
            if (candidate.compilationUnit() == cu && candidate.declaration().getNameAsString().equals(simpleName)) {
                return Optional.of(candidate);
            }
        }
        List<String> candidates = new ArrayList<>();
        for (ImportDeclaration imp : cu.getImports()) {
            String imported = imp.getNameAsString();
            if (!imp.isStatic() && !imp.isAsterisk() && imported.endsWith("." + simpleName)) {
                candidates.add(imported);
            }
        }
        candidates.add(qualify(packageName(cu), simpleName));
        for (ImportDeclaration imp : cu.getImports()) {
            if (!imp.isStatic() && imp.isAsterisk()) {
                candidates.add(imp.getNameAsString() + "." + simpleName);
            }
        }
        return candidates.stream().map(typesByFqcn::get).filter(t -> t != null).findFirst();
    }

    private static String fqcn(CompilationUnit cu, ClassOrInterfaceDeclaration type) {
        return qualify(packageName(cu), nestedName(type));
    }

    private static String nestedName(ClassOrInterfaceDeclaration type) {
        if (type.getParentNode().orElse(null) instanceof ClassOrInterfaceDeclaration parent) {
            return nestedName(parent) + "." + type.getNameAsString();
        }
        return type.getNameAsString();
    }

    private static String packageName(CompilationUnit cu) {
        return cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
    }

    private static String qualify(String packageName, String name) {
        return packageName.isEmpty() ? name : packageName + "." + name;
    }
}
