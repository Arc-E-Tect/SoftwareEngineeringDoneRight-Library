package com.arc_e_tect.gradle.architecture.domainvalidation;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Tells which validation slice a class belongs to.
 *
 * <p>Each direct sub-package of a validations package is one validation slice, except the
 * shared packages, such as {@code verdict}, which every slice may use.
 *
 * @since 0.1.0
 */
final class ValidationSlices {

    private final List<String> validationsPackages;
    private final Set<String> sharedPackages;
    private final DescribedPredicate<String> inboundPorts;

    private ValidationSlices(List<String> validationsPackages, Set<String> sharedPackages, DescribedPredicate<String> inboundPorts) {
        this.validationsPackages = validationsPackages;
        this.sharedPackages = sharedPackages;
        this.inboundPorts = inboundPorts;
    }

    static ValidationSlices fromConfiguration() {
        return new ValidationSlices(
                DomainValidationRulePackConfiguration.validationsPackages(),
                DomainValidationRulePackConfiguration.sharedPackages(),
                DomainValidationRulePackConfiguration.inboundPorts());
    }

    Optional<Slice> sliceOf(JavaClass javaClass) {
        return firstSegmentBelowAValidationsPackage(javaClass)
                .filter(segment -> !sharedPackages.contains(segment.name()))
                .map(segment -> new Slice(segment.validationsPackage(), segment.name()));
    }

    boolean isShared(JavaClass javaClass) {
        return firstSegmentBelowAValidationsPackage(javaClass)
                .map(segment -> sharedPackages.contains(segment.name()))
                .orElse(false);
    }

    boolean isInboundPort(JavaClass javaClass) {
        return inboundPorts.test(javaClass.getPackageName());
    }

    /** The classes a class depends on directly, without primitives and with arrays as their component type. */
    static Stream<JavaClass> dependencyTargets(JavaClass javaClass) {
        return javaClass.getDirectDependenciesFromSelf().stream()
                .map(Dependency::getTargetClass)
                .map(JavaClass::getBaseComponentType)
                .filter(target -> !target.isPrimitive());
    }

    static boolean hasSegment(String packageName, String segment) {
        return ("." + packageName + ".").contains("." + segment + ".");
    }

    private Optional<Segment> firstSegmentBelowAValidationsPackage(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        return validationsPackages.stream()
                .filter(validationsPackage -> packageName.startsWith(validationsPackage + "."))
                .map(validationsPackage -> {
                    String rest = packageName.substring(validationsPackage.length() + 1);
                    int dot = rest.indexOf('.');
                    return new Segment(validationsPackage, dot < 0 ? rest : rest.substring(0, dot));
                })
                .findFirst();
    }

    private record Segment(String validationsPackage, String name) {
    }

    /** One validation: a direct sub-package of a validations package. */
    record Slice(String validationsPackage, String name) {

        String packageName() {
            return validationsPackage + "." + name;
        }

        boolean contains(JavaClass javaClass) {
            return isInPackage(javaClass, packageName());
        }

        /** The slice's inbound web adapter, which holds its controller and mapper. */
        boolean containsInInboundWebAdapter(JavaClass javaClass) {
            return isInPackage(javaClass, packageName() + ".adapter.inbound.web");
        }

        /** The package the slice's code is generated into from the contract. */
        boolean containsInGeneratedApi(JavaClass javaClass) {
            return isInPackage(javaClass, packageName() + ".adapter.inbound.web.api");
        }

        private static boolean isInPackage(JavaClass javaClass, String packageName) {
            String actual = javaClass.getPackageName();
            return actual.equals(packageName) || actual.startsWith(packageName + ".");
        }
    }
}
