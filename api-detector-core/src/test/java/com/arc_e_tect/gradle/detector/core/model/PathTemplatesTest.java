package com.arc_e_tect.gradle.detector.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PathTemplates")
class PathTemplatesTest {

    @Test
    @DisplayName("normalize() adds a leading slash")
    void addsLeadingSlash() {
        assertThat(PathTemplates.normalize("users")).isEqualTo("/users");
    }

    @Test
    @DisplayName("normalize() collapses repeated slashes")
    void collapsesRepeatedSlashes() {
        assertThat(PathTemplates.normalize("//api//users//")).isEqualTo("/api/users");
    }

    @Test
    @DisplayName("normalize() strips a trailing slash")
    void stripsTrailingSlash() {
        assertThat(PathTemplates.normalize("/api/users/")).isEqualTo("/api/users");
    }

    @Test
    @DisplayName("normalize() keeps the root path as a single slash")
    void keepsRootPath() {
        assertThat(PathTemplates.normalize("/")).isEqualTo("/");
        assertThat(PathTemplates.normalize("")).isEqualTo("/");
    }

    @Test
    @DisplayName("normalize() strips regex constraints from path variables")
    void stripsRegexConstraints() {
        assertThat(PathTemplates.normalize("/users/{id:[0-9]+}")).isEqualTo("/users/{id}");
    }

    @Test
    @DisplayName("normalize() treats null as an empty path")
    void treatsNullAsEmpty() {
        assertThat(PathTemplates.normalize(null)).isEqualTo("/");
    }

    @Test
    @DisplayName("join() combines a base and sub path")
    void joinsBaseAndSubPath() {
        assertThat(PathTemplates.join("/api/users", "/{id}")).isEqualTo("/api/users/{id}");
    }

    @Test
    @DisplayName("join() handles a blank base path")
    void joinsBlankBasePath() {
        assertThat(PathTemplates.join("", "/health")).isEqualTo("/health");
    }

    @Test
    @DisplayName("join() handles a blank sub path")
    void joinsBlankSubPath() {
        assertThat(PathTemplates.join("/api/users", "")).isEqualTo("/api/users");
    }

    @Test
    @DisplayName("isPlaceholder() recognises a curly-brace segment")
    void recognisesPlaceholder() {
        assertThat(PathTemplates.isPlaceholder("{id}")).isTrue();
        assertThat(PathTemplates.isPlaceholder("users")).isFalse();
    }

    @Test
    @DisplayName("stripBasePath() removes a matching leading segment")
    void stripBasePathRemovesMatchingLeadingSegment() {
        assertThat(PathTemplates.stripBasePath("/crm-service/v1/users", "/crm-service"))
                .isEqualTo("/v1/users");
    }

    @Test
    @DisplayName("stripBasePath() reduces an exact match to the root path")
    void stripBasePathReducesExactMatchToRoot() {
        assertThat(PathTemplates.stripBasePath("/crm-service", "/crm-service")).isEqualTo("/");
    }

    @Test
    @DisplayName("stripBasePath() leaves a non-matching path unchanged, other than normalising it")
    void stripBasePathLeavesNonMatchingPathUnchanged() {
        assertThat(PathTemplates.stripBasePath("/other/v1/users", "/crm-service"))
                .isEqualTo("/other/v1/users");
    }

    @Test
    @DisplayName("stripBasePath() never strips a segment that merely shares a prefix")
    void stripBasePathNeverStripsAMerePrefixMatch() {
        // "/crm-service-admin/..." must not be treated as starting with "/crm-service" - only a
        // full path segment boundary counts as a match.
        assertThat(PathTemplates.stripBasePath("/crm-service-admin/v1/users", "/crm-service"))
                .isEqualTo("/crm-service-admin/v1/users");
    }

    @Test
    @DisplayName("stripBasePath() is a no-op for a blank base path")
    void stripBasePathNoOpForBlankBasePath() {
        assertThat(PathTemplates.stripBasePath("/v1/users", "")).isEqualTo("/v1/users");
        assertThat(PathTemplates.stripBasePath("/v1/users", null)).isEqualTo("/v1/users");
    }

    @Test
    @DisplayName("stripBasePath() is a no-op for a root base path")
    void stripBasePathNoOpForRootBasePath() {
        assertThat(PathTemplates.stripBasePath("/v1/users", "/")).isEqualTo("/v1/users");
    }

    @Test
    @DisplayName("stripBasePath() normalises both the path and the base path first")
    void stripBasePathNormalisesBothArguments() {
        assertThat(PathTemplates.stripBasePath("crm-service//v1/users/", "crm-service"))
                .isEqualTo("/v1/users");
    }
}
