package com.arc_e_tect.gradle.detector.core.model;

import com.arc_e_tect.gradle.detector.core.Described;

/**
 * A single HTTP endpoint exposed by a {@code @RestController} method, as found by scanning
 * controller source code.
 *
 * @param verb            the HTTP verb the endpoint responds to; {@link HttpVerb#ANY} when the
 *                         controller method does not restrict the verb
 * @param path             the endpoint's path template, e.g. {@code "/api/users/{id}"}; always
 *                         starts with {@code "/"}
 * @param declaringClass   fully-qualified name of the {@code @RestController} class
 * @param methodSignature  simple signature of the handler method, e.g. {@code "getUser(Long)"}
 * @param sourceFile       simple file name of the source file, e.g. {@code "UserController.java"}
 * @param lineNumber       1-based source line number of the handler method, or {@code 0} when
 *                         unknown
 */
public record Endpoint(
        HttpVerb verb, String path, String declaringClass, String methodSignature,
        String sourceFile, int lineNumber) implements Described {
}
