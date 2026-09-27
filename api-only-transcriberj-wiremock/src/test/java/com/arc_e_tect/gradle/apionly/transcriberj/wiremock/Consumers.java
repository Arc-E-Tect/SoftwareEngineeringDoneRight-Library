package com.arc_e_tect.gradle.apionly.transcriberj.wiremock;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The consumer builds, each run once for every test class that reads it: a Gradle build takes the
 * time. Each prints where its source sets and classpaths read from, and every dependency it
 * declares, through a {@code placement} task.
 */
final class Consumers {

    /** Prints each source set's directories, each classpath, and each declared dependency. */
    static final String PLACEMENT = """
            tasks.register('placement') {
                def sets = sourceSets
                def configs = configurations
                doLast {
                    sets.each { s ->
                        println "SOURCES ${s.name} ${s.java.srcDirs}"
                        println "RESOURCES ${s.name} ${s.resources.srcDirs}"
                        println "CLASSPATH ${s.name} ${s.runtimeClasspath.files}"
                    }
                    configs.each { c ->
                        c.allDependencies.each { d -> println "DEPENDENCY ${c.name} ${d.group}:${d.name}" }
                    }
                }
            }
            """;

    private static ConsumerBuild files;
    private static ConsumerBuild java;
    private static byte[] firstArchive;

    private Consumers() {
    }

    /** The {@code files} format's build, its archive packaged. */
    static synchronized ConsumerBuild files() {
        if (files == null) {
            files = new ConsumerBuild(directory("files")).write("", PLACEMENT);
            files.run("packageUserAccountWiremock", "placement");
            firstArchive = bytes(files.file(ConsumerBuild.ARCHIVE));
        }
        return files;
    }

    /** The archive the {@code files} format's build packaged the first time. */
    static synchronized byte[] firstArchive() {
        files();
        return firstArchive;
    }

    /** The {@code java} format's build, its test source sets compiled. */
    static synchronized ConsumerBuild java() {
        if (java == null) {
            java = new ConsumerBuild(directory("java")).write("""
                    sourceSets = ['contractTest']
                    options = [format: 'java']""", PLACEMENT);
            java.run("compileTestJava", "compileContractTestJava", "placement");
        }
        return java;
    }

    static byte[] bytes(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path directory(String name) {
        try {
            return Files.createTempDirectory("wiremock-consumer-" + name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
