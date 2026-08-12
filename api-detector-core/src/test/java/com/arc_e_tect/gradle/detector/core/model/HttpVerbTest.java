package com.arc_e_tect.gradle.detector.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HttpVerb")
class HttpVerbTest {

    @Test
    @DisplayName("resolves a Spring RequestMethod constant name")
    void resolvesRequestMethodName() {
        assertThat(HttpVerb.fromSpringRequestMethod("GET")).isEqualTo(HttpVerb.GET);
        assertThat(HttpVerb.fromSpringRequestMethod(" post ")).isEqualTo(HttpVerb.POST);
    }

    @Test
    @DisplayName("throws for an unrecognised verb name")
    void throwsForUnrecognisedVerb() {
        assertThatThrownBy(() -> HttpVerb.fromSpringRequestMethod("NOT_A_VERB"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
