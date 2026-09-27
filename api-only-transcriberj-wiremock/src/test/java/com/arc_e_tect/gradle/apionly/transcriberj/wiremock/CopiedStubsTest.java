package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T21.11: the Java is a starting point. The generated {@code ContractStubs}, copied into a package
 * of its own in a separate source tree, with one response body changed to a literal, compiles
 * against the generated tree -- so the copy depends only on its public API -- and a server using the
 * copy serves the changed response.
 */
@DisplayName("T21.11 The Java is a starting point")
class CopiedStubsTest {

    @Test
    void aCopyWithAChangedBodyCompilesAndServesIt() throws Exception {
        GeneratedSuite suite = Suites.of(Fixtures.PRECEDENCE);
        Path generated = suite.generated.java().resolve("com/example/contract/wiremock/ContractStubs.java");
        String source = Files.readString(generated);
        String changed = source
                .replace("package com.example.contract.wiremock;", "package com.example.mystubs;")
                .replace("case \"UserV1\" -> com.example.contract.UserV1.fullBody();",
                        "case \"UserV1\" -> \"{\\\"id\\\":\\\"changed\\\"}\";");
        assertThat(changed).isNotEqualTo(source).contains("\"{\\\"id\\\":\\\"changed\\\"}\"");

        Path sources = Suites.directory("copied");
        Path copy = Files.createDirectories(sources.resolve("src/com/example/mystubs")).resolve("ContractStubs.java");
        Files.writeString(copy, changed);
        Path classes = Files.createDirectories(sources.resolve("classes"));
        GeneratedSuite.compile(List.of(sources.resolve("src")), classes,
                System.getProperty("java.class.path") + File.pathSeparator + suite.classes(), null);

        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes.toUri().toURL()}, suite.loader());
             DoubleServer server = new DoubleServer()) {
            Class<?> stubs = loader.loadClass("com.example.mystubs.ContractStubs");
            GeneratedSuite.Case c = suite.find("GetUser", "success-200-required");
            MappingBuilder mapping = (MappingBuilder) stubs.getMethod("mappingFor",
                    suite.type("com.example.contract.ContractCase")).invoke(null, suite.contractCase(c));
            server.stub(mapping);
            Replay.Response response = Replay.send(server, suite.request(c));
            assertThat(response.status()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("{\"id\":\"changed\"}");
        }
    }
}
