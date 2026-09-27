package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A response body the TranscriberJ cannot build. The files leave out every stub that would answer
 * with it, and the report names each one and why; the stubs without a body are written. In the
 * {@code java} format, making such a stub throws the TranscriberJ's own exception, with the reason.
 */
@DisplayName("A body the TranscriberJ cannot build")
class UnbuildableBodyTest {

    @Test
    void theFilesLeaveOutItsStubsAndSayWhy() {
        GeneratedSuite suite = GeneratedSuite.of(Fixtures.UNBUILDABLE, Map.of(), Suites.directory("unbuildable"));
        assertThat(suite.mappingFiles().keySet()).containsExactlyInAnyOrder(
                "mappings/getCount/path-name-maxLength.json", "mappings/getCount/fallback-invalid.json");
        assertThat(Fixtures.Generated.files(suite.files()).keySet()).noneMatch(path -> path.startsWith("__files"));
        assertThat(suite.generated.wiremock().degraded()).extracting(d -> d.className()).containsExactlyInAnyOrder(
                "mappings/getCount/success-200-required.json", "mappings/getCount/fallback-valid.json");
        assertThat(suite.generated.wiremock().degraded()).allSatisfy(d -> {
            assertThat(d.method()).isEqualTo("response body");
            assertThat(d.finding().detail()).contains("CountV1.fullBody() has no valid value");
        });
    }

    @Test
    void theJavaFormatThrowsTheTranscriberJsException() {
        GeneratedSuite suite = GeneratedSuite.of(Fixtures.UNBUILDABLE, Map.of(), Suites.directory("unbuildable-java"));
        assertThatThrownBy(() -> suite.mapping(suite.find("success-200-required")))
                .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("CountV1.fullBody()");
        assertThat(suite.mapping(suite.find("path-name-maxLength")).build().getName())
                .isEqualTo("getCount/path-name-maxLength");
    }
}
