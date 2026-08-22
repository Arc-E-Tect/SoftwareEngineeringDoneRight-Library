package com.arc_e_tect.gradle.detector.core.exclude;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExclusionRule")
class ExclusionRuleTest {

    @Test
    @DisplayName("parses a bare path as an ANY-verb rule")
    void parsesBarePathAsAnyVerb() {
        ExclusionRule rule = ExclusionRule.parse("/actuator/health");

        assertThat(rule.verb()).isEqualTo(HttpVerb.ANY);
        assertThat(rule.pathPattern()).isEqualTo("/actuator/health");
    }

    @Test
    @DisplayName("parses a 'VERB path' rule, case-insensitively for the verb")
    void parsesVerbPrefixedRule() {
        ExclusionRule rule = ExclusionRule.parse("get /actuator/health");

        assertThat(rule.verb()).isEqualTo(HttpVerb.GET);
        assertThat(rule.pathPattern()).isEqualTo("/actuator/health");
    }

    @Test
    @DisplayName("trims surrounding whitespace")
    void trimsWhitespace() {
        ExclusionRule rule = ExclusionRule.parse("  GET   /actuator/health  ");

        assertThat(rule.verb()).isEqualTo(HttpVerb.GET);
        assertThat(rule.pathPattern()).isEqualTo("/actuator/health");
    }

    @Test
    @DisplayName("rejects a blank rule")
    void rejectsBlankRule() {
        assertThatThrownBy(() -> ExclusionRule.parse("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects a path pattern that does not start with '/'")
    void rejectsPathWithoutLeadingSlash() {
        assertThatThrownBy(() -> ExclusionRule.parse("actuator/health"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must start with '/'");
    }

    @Test
    @DisplayName("rejects an unrecognised HTTP verb")
    void rejectsUnrecognisedVerb() {
        assertThatThrownBy(() -> ExclusionRule.parse("FETCH /actuator/health"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FETCH");
    }
}
