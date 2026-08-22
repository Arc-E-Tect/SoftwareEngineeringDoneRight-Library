package com.arc_e_tect.gradle.detector.core.exclude;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WellKnownExclusionSets")
class WellKnownExclusionSetsTest {

    @Test
    @DisplayName("resolves the bundled Spring Boot Actuator set")
    void resolvesSpringBootActuator() {
        List<ExclusionRule> rules = WellKnownExclusionSets.resolve(WellKnownExclusionSets.SPRING_BOOT_ACTUATOR);

        assertThat(rules).containsExactly(new ExclusionRule(HttpVerb.ANY, "/actuator/**"));
    }

    @Test
    @DisplayName("rejects an unknown well-known set name")
    void rejectsUnknownName() {
        assertThatThrownBy(() -> WellKnownExclusionSets.resolve("not-a-real-set"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not-a-real-set")
                .hasMessageContaining(WellKnownExclusionSets.SPRING_BOOT_ACTUATOR);
    }
}
