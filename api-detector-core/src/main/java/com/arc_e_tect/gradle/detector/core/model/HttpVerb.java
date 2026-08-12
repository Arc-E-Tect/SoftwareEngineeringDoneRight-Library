package com.arc_e_tect.gradle.detector.core.model;

/**
 * HTTP verb exposed by a Spring MVC endpoint or described by an OpenAPI operation.
 *
 * <p>{@link #ANY} is a controller-only concept: it represents a Spring
 * {@code @RequestMapping} that does not restrict the {@code method} attribute, and is
 * therefore reachable through every HTTP verb. It never appears on the OpenAPI side,
 * since every OpenAPI operation is described under one concrete verb.</p>
 */
public enum HttpVerb {

    GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE,

    /** Matches any concrete verb; used for Spring mappings that do not restrict the verb. */
    ANY;

    /**
     * Resolves the {@link HttpVerb} matching a Spring {@code RequestMethod} enum constant name
     * (e.g. {@code "GET"}), as found on a {@code @RequestMapping(method = ...)} attribute.
     *
     * @param springRequestMethodName the simple name of the {@code RequestMethod} constant
     * @return the matching verb
     * @throws IllegalArgumentException if the name is not a recognised HTTP verb
     */
    public static HttpVerb fromSpringRequestMethod(String springRequestMethodName) {
        return HttpVerb.valueOf(springRequestMethodName.trim().toUpperCase());
    }
}
