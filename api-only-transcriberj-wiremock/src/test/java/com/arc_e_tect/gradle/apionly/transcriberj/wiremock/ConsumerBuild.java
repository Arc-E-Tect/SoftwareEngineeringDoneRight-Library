package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * A consumer build of the API-Only TranscriberJ with this emitter: the user-account fixture
 * contract published to a file channel, as the API-Only Publisher ships one, the emitter loaded
 * from its jar, and the subscription's {@code emitter('wiremock')} block as a test gives it. Run
 * with Gradle TestKit, against the TranscriberJ version the emitter is built against.
 */
final class ConsumerBuild {

    /** The contract the build subscribes to. */
    static final String CONTRACT = "user-account";

    /** The files directory, as the plugin places it by default. */
    static final String FILES = "build/generated/files/transcriberj/user-account/wiremock";

    /** The archive, as the plugin names it. */
    static final String ARCHIVE = "build/distributions/user-account-wiremock-1.0.0.zip";

    final Path dir;

    ConsumerBuild(Path dir) {
        this.dir = dir;
    }

    /**
     * Writes the build.
     *
     * @param emitterBlock the body of the {@code emitter('wiremock')} block
     * @param extra        anything more for the build script
     * @return this build
     */
    ConsumerBuild write(String emitterBlock, String extra) {
        try {
            String version = System.getProperty("transcriberj.version");
            Files.writeString(dir.resolve("settings.gradle"), """
                    pluginManagement {
                        repositories {
                            %s
                            gradlePluginPortal()
                        }
                    }
                    rootProject.name = 'consumer'
                    """.formatted(version.endsWith("-SNAPSHOT") ? "mavenLocal()" : ""));
            if (!Files.exists(dir.resolve("channel"))) publish();
            Files.writeString(dir.resolve("build.gradle"), """
                    plugins {
                        id 'java'
                        id 'com.arc-e-tect.api-only-transcriberj' version '%s'
                    }

                    repositories {
                        mavenCentral()
                    }

                    sourceSets {
                        contractTest
                    }

                    dependencies {
                        transcriberjEmitters files('%s')
                    }

                    apiOnlySubscriber {
                        channel {
                            type = 'file'
                            directory = file('channel').path
                        }
                        subscribe('user-account') {
                            apiContractVersion = '1.0.0'
                        }
                    }

                    apiOnlyTranscriberJ {
                        subscription('user-account') {
                            basePackage = 'com.example.contract'
                            sourceSets = ['test', 'contractTest']
                            emitter('wiremock') {
                    %s
                            }
                        }
                    }
                    %s
                    """.formatted(version, System.getProperty("emitter.jar").replace('\\', '/'),
                    emitterBlock.indent(16).stripTrailing(), extra));
            return this;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Runs the build, which must succeed. */
    BuildResult run(String... arguments) {
        List<String> args = new ArrayList<>(List.of(arguments));
        args.add("--stacktrace");
        return GradleRunner.create().withProjectDir(dir.toFile()).withArguments(args).forwardOutput().build();
    }

    /** A file of the build. */
    Path file(String path) {
        return dir.resolve(path);
    }

    /** Publishes the fixture contract to the file channel, as the API-Only Publisher ships one. */
    private void publish() throws IOException {
        Path stage = Files.createDirectories(dir.resolve("stage"));
        Files.copy(Fixtures.USER_ACCOUNT.document(), stage.resolve("openapi.yaml"));
        Files.writeString(stage.resolve("manifest.json"), """
                {"schemaVersion":1,"target":"user-account","version":"1.0.0","files":[{"path":"openapi.yaml","sha256":"%s"}]}"""
                .formatted(sha256(stage.resolve("openapi.yaml"))));
        Path archives = Files.createDirectories(dir.resolve("channel/user-account/1.0.0"));
        try {
            Process tar = new ProcessBuilder("tar", "-czf", archives.resolve("user-account-1.0.0.tgz").toString(),
                    "manifest.json", "openapi.yaml").directory(stage.toFile()).inheritIO().start();
            if (tar.waitFor() != 0) throw new IllegalStateException("tar failed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static String sha256(Path file) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The UTF-8 text of a file of the build. */
    String read(String path) {
        try {
            return Files.readString(file(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
