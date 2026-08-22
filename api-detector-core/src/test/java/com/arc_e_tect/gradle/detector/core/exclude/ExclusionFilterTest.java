package com.arc_e_tect.gradle.detector.core.exclude;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.openapi.DescribedEndpoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExclusionFilter")
class ExclusionFilterTest {

    private static final DescribedEndpoint HEALTH = endpoint(HttpVerb.GET, "/actuator/health");
    private static final DescribedEndpoint USERS = endpoint(HttpVerb.GET, "/users");

    @Test
    @DisplayName("excludeMatching(): keeps only items with no matching rule")
    void excludeMatchingKeepsNonMatchingItems() {
        List<ExclusionRule> rules = List.of(ExclusionRule.parse("/actuator/**"));

        assertThat(ExclusionFilter.excludeMatching(List.of(HEALTH, USERS), rules)).containsExactly(USERS);
    }

    @Test
    @DisplayName("excludeMatching(): returns the input unchanged when there are no rules")
    void excludeMatchingReturnsInputWhenNoRules() {
        List<DescribedEndpoint> items = List.of(HEALTH, USERS);

        assertThat(ExclusionFilter.excludeMatching(items, List.of())).isEqualTo(items);
    }

    @Test
    @DisplayName("onlyMatching(): keeps only items with a matching rule")
    void onlyMatchingKeepsMatchingItems() {
        List<ExclusionRule> rules = List.of(ExclusionRule.parse("/actuator/**"));

        assertThat(ExclusionFilter.onlyMatching(List.of(HEALTH, USERS), rules)).containsExactly(HEALTH);
    }

    @Test
    @DisplayName("onlyMatching(): is empty when there are no rules")
    void onlyMatchingEmptyWhenNoRules() {
        assertThat(ExclusionFilter.onlyMatching(List.of(HEALTH, USERS), List.of())).isEmpty();
    }

    private static DescribedEndpoint endpoint(HttpVerb verb, String path) {
        return new DescribedEndpoint(verb, path, "op", List.of());
    }
}
