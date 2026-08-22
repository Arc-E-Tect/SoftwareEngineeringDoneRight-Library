package com.arc_e_tect.gradle.detector.core.exclude;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExclusionRuleFile")
class ExclusionRuleFileTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("loads rules from a well-formed exclusion file")
    void loadsRulesFromFile() throws IOException {
        File file = writeFile("""
                exclusions:
                  - "/actuator/health"
                  - "GET /actuator/**"
                """);

        List<ExclusionRule> rules = ExclusionRuleFile.load(file);

        assertThat(rules).containsExactly(
                new ExclusionRule(HttpVerb.ANY, "/actuator/health"),
                new ExclusionRule(HttpVerb.GET, "/actuator/**"));
    }

    @Test
    @DisplayName("returns an empty list for an empty file")
    void returnsEmptyListForEmptyFile() throws IOException {
        File file = writeFile("");

        assertThat(ExclusionRuleFile.load(file)).isEmpty();
    }

    @Test
    @DisplayName("rejects a file with no top-level 'exclusions' list")
    void rejectsFileWithoutExclusionsKey() throws IOException {
        File file = writeFile("other: []\n");

        assertThatThrownBy(() -> ExclusionRuleFile.load(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exclusions:");
    }

    @Test
    @DisplayName("rejects a non-string entry under 'exclusions'")
    void rejectsNonStringEntry() throws IOException {
        File file = writeFile("""
                exclusions:
                  - 42
                """);

        assertThatThrownBy(() -> ExclusionRuleFile.load(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a string");
    }

    @Test
    @DisplayName("propagates a malformed rule string's own parse error")
    void propagatesMalformedRuleError() throws IOException {
        File file = writeFile("""
                exclusions:
                  - "not-a-path"
                """);

        assertThatThrownBy(() -> ExclusionRuleFile.load(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must start with '/'");
    }

    @Test
    @DisplayName("loads a bundled classpath resource")
    void loadsBundledResource() throws IOException {
        List<ExclusionRule> rules =
                ExclusionRuleFile.loadResource(WellKnownExclusionSets.class, "wellknown/spring-boot-actuator.yaml");

        assertThat(rules).contains(new ExclusionRule(HttpVerb.ANY, "/actuator/**"));
    }

    @Test
    @DisplayName("fails when the classpath resource is missing")
    void failsWhenResourceMissing() {
        assertThatThrownBy(() -> ExclusionRuleFile.loadResource(WellKnownExclusionSets.class, "wellknown/does-not-exist.yaml"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Missing bundled resource");
    }

    private File writeFile(String content) throws IOException {
        Path path = tempDir.resolve("exclusions.yaml");
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path.toFile();
    }
}
