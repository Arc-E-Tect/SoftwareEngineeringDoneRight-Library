package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T21.12 and T21.16: placement, packaging and the dependency, in consumer builds of the
 * TranscriberJ. With {@code files}, no generated file is in any source set's sources, resources or
 * runtime classpath, no Java is generated, and no configuration declares WireMock; the archive holds
 * {@code mappings/} and {@code __files/} at its root, and the provenance file, and is byte-identical
 * across two runs. With {@code java}, the class compiles in the configured source set only, which
 * alone gets the managed WireMock dependency.
 */
@DisplayName("T21.12 Placement and packaging; T21.16 Dependency")
class PackagingTest {

    @Test
    void theFilesAreOnNoSourceSetAndNoJavaIsGenerated() {
        ConsumerBuild build = Consumers.files();
        String placement = build.run("placement", "-q").getOutput();
        assertThat(placement).contains("SOURCES test", "SOURCES contractTest");
        assertThat(placement.lines().filter(l -> l.startsWith("SOURCES") || l.startsWith("RESOURCES")
                || l.startsWith("CLASSPATH"))).noneMatch(l -> l.contains("generated/files"))
                .noneMatch(l -> l.contains("transcriberj/user-account/wiremock"));
        assertThat(build.file(ConsumerBuild.FILES).resolve("mappings")).isDirectory();
        assertThat(build.file("build/generated/sources/transcriberj/user-account/wiremock")).satisfiesAnyOf(
                dir -> assertThat(dir).doesNotExist(), dir -> assertThat(dir).isEmptyDirectory());
        assertThat(placement.lines().filter(l -> l.startsWith("DEPENDENCY"))).noneMatch(l -> l.contains("wiremock"));
    }

    @Test
    void theArchiveHoldsTheMappingsAtItsRootAndItsProvenance() {
        List<String> entries = entries(Consumers.firstArchive());
        assertThat(entries).contains("apionly-provenance.json", "mappings/", "__files/")
                .anyMatch(e -> e.startsWith("mappings/GetUser/") && e.endsWith(".json"))
                .anyMatch(e -> e.startsWith("__files/user-account/") && e.endsWith(".json"))
                .allMatch(e -> e.equals("apionly-provenance.json") || e.startsWith("mappings/") || e.startsWith("__files/"));
        List<String> files = new ArrayList<>();
        try (var walk = Files.walk(Consumers.files().file(ConsumerBuild.FILES))) {
            Consumers.files().file(ConsumerBuild.FILES);
            walk.filter(Files::isRegularFile).forEach(p -> files.add(
                    Consumers.files().file(ConsumerBuild.FILES).relativize(p).toString().replace('\\', '/')));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertThat(entries).containsAll(files);
    }

    @Test
    void theArchiveIsTheSameBytesOnASecondRun() {
        ConsumerBuild build = Consumers.files();
        byte[] first = Consumers.firstArchive();
        build.run("clean", "packageUserAccountWiremock");
        assertThat(Consumers.bytes(build.file(ConsumerBuild.ARCHIVE))).isEqualTo(first);
    }

    @Test
    void theJavaFormatCompilesInItsSourceSetOnlyWithItsDependency() {
        ConsumerBuild build = Consumers.java();
        assertThat(build.file("build/classes/java/contractTest/com/example/contract/wiremock/ContractStubs.class"))
                .exists();
        assertThat(build.file("build/classes/java/test/com/example/contract/wiremock")).doesNotExist();
        BuildResult result = build.run("placement", "-q");
        String placement = result.getOutput();
        assertThat(placement.lines().filter(l -> l.startsWith("SOURCES contractTest")))
                .anyMatch(l -> l.contains("transcriberj/user-account/wiremock"));
        assertThat(placement.lines().filter(l -> l.startsWith("SOURCES test ")))
                .noneMatch(l -> l.contains("transcriberj/user-account/wiremock"));
        List<String> wiremock = placement.lines().filter(l -> l.startsWith("DEPENDENCY") && l.contains("wiremock-jetty12"))
                .toList();
        assertThat(wiremock).isNotEmpty().allMatch(l -> l.startsWith("DEPENDENCY contractTest"));
        assertThat(build.file("build/generated/files/transcriberj/user-account/wiremock")).satisfiesAnyOf(
                dir -> assertThat(dir).doesNotExist(), dir -> assertThat(dir).isEmptyDirectory());
    }

    /** An archive's entries, in order. */
    static List<String> entries(byte[] archive) {
        List<String> out = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(archive))) {
            for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) out.add(e.getName());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }

    /** One entry of an archive, as text. */
    static String entry(byte[] archive, String name) {
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(archive))) {
            for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
                if (e.getName().equals(name)) return new String(((InputStream) zip).readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }
}
