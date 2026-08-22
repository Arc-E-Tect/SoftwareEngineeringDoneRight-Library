package com.arc_e_tect.gradle.detector.core.exclude;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExclusionMatcher")
class ExclusionMatcherTest {

    @Test
    @DisplayName("matches an exact path with an ANY-verb rule")
    void matchesExactPath() {
        assertThat(ExclusionMatcher.matches(endpoint(HttpVerb.GET, "/actuator/health"),
                List.of(ExclusionRule.parse("/actuator/health")))).isTrue();
    }

    @Test
    @DisplayName("does not match a different exact path")
    void doesNotMatchDifferentPath() {
        assertThat(ExclusionMatcher.matches(endpoint(HttpVerb.GET, "/actuator/info"),
                List.of(ExclusionRule.parse("/actuator/health")))).isFalse();
    }

    @Test
    @DisplayName("a verb-restricted rule only matches that verb")
    void verbRestrictedRuleMatchesOnlyThatVerb() {
        List<ExclusionRule> rules = List.of(ExclusionRule.parse("GET /actuator/health"));

        assertThat(ExclusionMatcher.matches(endpoint(HttpVerb.GET, "/actuator/health"), rules)).isTrue();
        assertThat(ExclusionMatcher.matches(endpoint(HttpVerb.POST, "/actuator/health"), rules)).isFalse();
    }

    @Test
    @DisplayName("'*' matches exactly one path segment")
    void starMatchesOneSegment() {
        List<ExclusionRule> rules = List.of(ExclusionRule.parse("/actuator/*"));

        assertThat(ExclusionMatcher.matches(endpoint(HttpVerb.GET, "/actuator/health"), rules)).isTrue();
        assertThat(ExclusionMatcher.matches(endpoint(HttpVerb.GET, "/actuator/health/db"), rules)).isFalse();
    }

    @Test
    @DisplayName("trailing '**' matches any remaining segments, including zero")
    void doubleStarMatchesAnyRemainingSegments() {
        List<ExclusionRule> rules = List.of(ExclusionRule.parse("/actuator/**"));

        assertThat(ExclusionMatcher.matches(endpoint(HttpVerb.GET, "/actuator"), rules)).isTrue();
        assertThat(ExclusionMatcher.matches(endpoint(HttpVerb.GET, "/actuator/health"), rules)).isTrue();
        assertThat(ExclusionMatcher.matches(endpoint(HttpVerb.GET, "/actuator/health/db"), rules)).isTrue();
        assertThat(ExclusionMatcher.matches(endpoint(HttpVerb.GET, "/other"), rules)).isFalse();
    }

    @Test
    @DisplayName("'*' matches a placeholder segment just like a literal one")
    void starMatchesPlaceholderSegment() {
        List<ExclusionRule> rules = List.of(ExclusionRule.parse("/users/*"));

        assertThat(ExclusionMatcher.matches(endpoint(HttpVerb.GET, "/users/{id}"), rules)).isTrue();
    }

    @Test
    @DisplayName("matches when at least one of several rules matches")
    void matchesWhenAnyRuleMatches() {
        List<ExclusionRule> rules = List.of(
                ExclusionRule.parse("/other/**"),
                ExclusionRule.parse("/actuator/health"));

        assertThat(ExclusionMatcher.matches(endpoint(HttpVerb.GET, "/actuator/health"), rules)).isTrue();
    }

    @Test
    @DisplayName("does not match an empty rule list")
    void doesNotMatchEmptyRuleList() {
        assertThat(ExclusionMatcher.matches(endpoint(HttpVerb.GET, "/actuator/health"), List.of())).isFalse();
    }

    private static DescribedEndpoint endpoint(HttpVerb verb, String path) {
        return new DescribedEndpoint(verb, path, "op", List.of());
    }
}
