package com.arc_e_tect.gradle.detector.core.openapi;

import com.arc_e_tect.gradle.detector.core.Described;
import com.arc_e_tect.gradle.detector.core.model.HttpVerb;

import java.util.List;

/**
 * A single HTTP verb + path template pair described by an OpenAPI operation.
 *
 * @param verb        the HTTP verb the OpenAPI operation is documented under; never
 *                     {@link HttpVerb#ANY}
 * @param path         the OpenAPI path template, e.g. {@code "/users/{id}"}
 * @param operationId  the operation's {@code operationId}, or {@code null} when not set
 * @param tags         the operation's tags, in document order; empty when none are set, never
 *                     {@code null}
 */
public record DescribedEndpoint(HttpVerb verb, String path, String operationId, List<String> tags)
        implements Described {
}
