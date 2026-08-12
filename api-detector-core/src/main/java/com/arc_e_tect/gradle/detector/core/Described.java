package com.arc_e_tect.gradle.detector.core;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;

/**
 * A single HTTP verb + path template pair, implemented by both a controller-scanned
 * {@link com.arc_e_tect.gradle.detector.core.model.Endpoint} and an OpenAPI-derived
 * {@link com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint} so that
 * {@link com.arc_e_tect.gradle.detector.core.detect.ContractSetOperations} can compare either
 * shape against the other, in either direction.
 */
public interface Described {

    /**
     * The HTTP verb.
     *
     * @return the verb
     */
    HttpVerb verb();

    /**
     * The path template.
     *
     * @return the path
     */
    String path();
}
