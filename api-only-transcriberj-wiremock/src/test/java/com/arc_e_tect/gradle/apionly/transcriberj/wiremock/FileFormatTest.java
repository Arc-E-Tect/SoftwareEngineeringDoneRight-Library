package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T21.9 and T21.10: the files are WireMock's format, and the Java builds the same stubs.
 *
 * <p>Every mapping file deserialises with WireMock's own mapper into a {@code StubMapping} and
 * serialises back to an equivalent document; validates against the mapping schema WireMock
 * publishes in its jar; and holds the documented metadata. For every case and every fallback, the
 * {@code java} format's mapping, built and serialised with WireMock's mapper, equals the file,
 * ignoring only the identifiers WireMock generates and where the body is kept -- in a file of
 * {@code __files/} rather than inline.
 */
@DisplayName("T21.9 The files are WireMock's format; T21.10 Java and files are the same stubs")
class FileFormatTest {

    /** The members WireMock generates for a mapping it builds or reads, which the files do not hold. */
    private static final List<String> GENERATED = List.of("id", "uuid");

    private static final Schema MAPPING_SCHEMA = mappingSchema();

    static List<Fixtures.Contract> contracts() {
        return Fixtures.ALL;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyFileIsAMappingWireMockReadsAndWritesBack(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        assertThat(suite.mappingFiles()).isNotEmpty();
        suite.mappingFiles().forEach((path, text) -> {
            JsonNode file = Oracle.JSON.readTree(text);
            JsonNode written = withoutGenerated(GeneratedSuite.tree(StubMapping.buildFrom(text)));
            assertThat(written).as(path).isEqualTo(file);
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyFileValidatesAgainstWireMocksMappingSchema(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        suite.mappingFiles().forEach((path, text) ->
                assertThat(MAPPING_SCHEMA.validate(Oracle.JSON.readTree(text))).as(path).isEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void everyFileHoldsTheDocumentedMetadata(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        suite.mappingFiles().forEach((path, text) -> {
            JsonNode mapping = Oracle.JSON.readTree(text);
            JsonNode metadata = mapping.get("metadata");
            String caseId = metadata.get("case").stringValue();
            boolean fallback = caseId.startsWith("fallback-");
            List<String> members = new ArrayList<>(List.of("contract", "contractVersion", "operation", "case", "kind",
                    "fallback"));
            if (caseId.equals("fallback-valid")) members.addAll(List.of("enforces", "doesNotEnforce"));
            assertThat(metadata.propertyNames()).as(path).containsExactlyElementsOf(members);
            assertThat(metadata.get("contract").stringValue()).isEqualTo(contract.name());
            assertThat(metadata.get("contractVersion").stringValue()).isEqualTo("1.0.0");
            assertThat(metadata.get("fallback").booleanValue()).as(path).isEqualTo(fallback);
            assertThat(mapping.get("name").stringValue()).as(path)
                    .isEqualTo(metadata.get("operation").stringValue() + "/" + caseId);
            assertThat(path).isEqualTo(WireMockEmitter.MAPPINGS + "/" + metadata.get("operation").stringValue() + "/"
                    + caseId + ".json");
            if (!fallback) {
                String operation = metadata.get("operation").stringValue();
                GeneratedSuite.Case c = suite.cases.stream().filter(k -> k.id().equals(caseId)
                        && suite.operationName(k.operation()).equals(operation)).findFirst().orElseThrow();
                assertThat(metadata.get("kind").stringValue()).as(path).isEqualTo(c.kind());
            }
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("contracts")
    void theJavaFormatBuildsTheSameStubsAsTheFiles(Fixtures.Contract contract) {
        GeneratedSuite suite = Suites.of(contract);
        Map<String, JsonNode> files = new TreeMap<>();
        suite.mappingFiles().values().forEach(text -> {
            JsonNode file = Oracle.JSON.readTree(text);
            files.put(file.get("name").stringValue(), inline(suite, file));
        });
        Map<String, JsonNode> java = new TreeMap<>();
        for (MappingBuilder mapping : suite.all()) {
            JsonNode built = withoutGenerated(GeneratedSuite.tree(mapping.build()));
            java.putIfAbsent(built.get("name").stringValue(), built);
        }
        List<GeneratedSuite.Case> shadowed = PublishedFilesPassTest.shadowed(suite);
        java.keySet().removeAll(shadowed.stream().map(suite::mappingName).toList());
        assertThat(java.keySet()).isEqualTo(files.keySet());
        java.forEach((name, built) -> assertThat(built).as(name).isEqualTo(files.get(name)));
    }

    /** A file's mapping with its body file's content inline, as the {@code java} format writes it. */
    private static JsonNode inline(GeneratedSuite suite, JsonNode file) {
        ObjectNode out = (ObjectNode) file.deepCopy();
        ObjectNode response = (ObjectNode) out.get("response");
        if (response.has("bodyFileName")) {
            String name = response.remove("bodyFileName").stringValue();
            try {
                response.put("body", Files.readString(suite.files().resolve(WireMockEmitter.FILES).resolve(name)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return out;
    }

    private static JsonNode withoutGenerated(JsonNode mapping) {
        ObjectNode out = (ObjectNode) mapping.deepCopy();
        GENERATED.forEach(out::remove);
        return out;
    }

    /** The mapping schema WireMock publishes in its jar. */
    private static Schema mappingSchema() {
        try (InputStream in = StubMapping.class.getResourceAsStream("/schemas/wiremock-stub-mapping.json")) {
            String schema = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_4).getSchema(schema);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
