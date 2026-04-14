package com.arc_e_tect.sedr.utils.jacoco.marker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * Marker annotation that signals JaCoCo (and the
 * {@code com.arc-e-tect.jacoco-exclusion-report} Gradle plugin) to exclude the
 * annotated constructor, method, or type from code-coverage measurement.
 *
 * <p>Apply this annotation only to elements that are genuinely untestable or
 * where coverage is not meaningful — for example, framework entry points,
 * Lombok-generated boilerplate, or infrastructure glue code.
 *
 * <p>Example usage:
 * <pre>
 *   {@literal @}ExcludeFromJacocoGeneratedCodeCoverage("Spring Boot entry point")
 *   public static void main(String[] args) { ... }
 *
 *   {@literal @}ExcludeFromJacocoGeneratedCodeCoverage(justification = "Lombok-generated boilerplate")
 *   public class MyDto { ... }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, TYPE, METHOD})
public @interface ExcludeFromJacocoGeneratedCodeCoverage {
    /**
     * Optional human-readable justification for why this element is excluded
     * from JaCoCo code-coverage measurement.
     *
     * <p>Examples:
     * <pre>
     *   {@literal @}ExcludeFromJacocoGeneratedCodeCoverage("Spring Boot entry point – not unit-testable")
     *   {@literal @}ExcludeFromJacocoGeneratedCodeCoverage(justification = "Lombok-generated boilerplate")
     * </pre>
     *
     * @return the justification string, or an empty string if none was provided
     */
    String justification() default "";
}
