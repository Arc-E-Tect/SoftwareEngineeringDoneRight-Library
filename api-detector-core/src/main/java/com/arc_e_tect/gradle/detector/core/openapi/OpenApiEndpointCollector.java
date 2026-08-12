package com.arc_e_tect.gradle.detector.core.openapi;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.model.PathTemplates;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads a root OpenAPI document - resolving every {@code $ref} to other documents relative to
 * it - and collects every HTTP verb + path template pair it describes.
 */
public class OpenApiEndpointCollector {

    private static final Pattern OPENAPI_32_YAML_PATTERN =
        Pattern.compile("(?m)^\\s*openapi\\s*:\\s*['\"]?(3\\.2(?:\\.\\d+)?)['\"]?\\s*$");

    private static final Pattern OPENAPI_32_JSON_PATTERN =
        Pattern.compile("(\"openapi\"\\s*:\\s*\")3\\.2(?:\\.\\d+)?(\")");

    /** Creates a new {@code OpenApiEndpointCollector}. */
    public OpenApiEndpointCollector() {}

    /**
     * Parses {@code rootDocument} and every document it links to (relative {@code $ref}s are
     * resolved automatically), and returns the verb + path template pair described by every
     * operation found.
     *
     * @param rootDocument the root OpenAPI document (JSON or YAML)
     * @return possibly-empty list of described endpoints, never {@code null}
     * @throws IllegalStateException if the document cannot be parsed
     */
    public List<DescribedEndpoint> collect(File rootDocument) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        SwaggerParseResult result = parse(rootDocument, options);
        if (result.getOpenAPI() == null && declaresOpenApi32(rootDocument.toPath())) {
            result = parseWithOpenApi31Compatibility(rootDocument, options);
        }

        OpenAPI openApi = result.getOpenAPI();
        if (openApi == null) {
            String messages = result.getMessages() == null ? "" : String.join("; ", result.getMessages());
            throw new IllegalStateException(
                    "apiDetectorCore: failed to parse OpenAPI document " + rootDocument + ": " + messages);
        }

        List<DescribedEndpoint> endpoints = new ArrayList<>();
        Paths paths = openApi.getPaths();
        if (paths == null) {
            return endpoints;
        }
        paths.forEach((path, item) -> item.readOperationsMap().forEach((method, operation) ->
                endpoints.add(new DescribedEndpoint(
                        HttpVerb.valueOf(method.name()),
                        PathTemplates.normalize(path),
                        operation.getOperationId(),
                        operationTags(operation)))));
        return endpoints;
    }

    private static List<String> operationTags(Operation operation) {
        return operation.getTags() == null ? List.of() : List.copyOf(operation.getTags());
    }

    private static SwaggerParseResult parse(File rootDocument, ParseOptions options) {
        return new OpenAPIV3Parser().readLocation(rootDocument.getAbsolutePath(), null, options);
    }

    private static SwaggerParseResult parseWithOpenApi31Compatibility(File rootDocument, ParseOptions options) {
        Path rootPath = rootDocument.toPath();
        try {
            String original = Files.readString(rootPath);
            String normalized = normalizeOpenApi32To31(original);
            if (normalized.equals(original)) {
                return parse(rootDocument, options);
            }

            Path parent = rootPath.getParent();
            String prefix = rootDocument.getName() + ".api-detector-core-";
            Path tempFile = Files.createTempFile(parent, prefix, ".yaml");
            try {
                Files.writeString(tempFile, normalized);
                return parse(tempFile.toFile(), options);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "apiDetectorCore: failed to prepare OpenAPI 3.2 compatibility parse for "
                            + rootDocument + ": " + ex.getMessage(),
                    ex);
        }
    }

    private static boolean declaresOpenApi32(Path rootDocument) {
        try {
            String content = Files.readString(rootDocument);
            return OPENAPI_32_YAML_PATTERN.matcher(content).find()
                    || OPENAPI_32_JSON_PATTERN.matcher(content).find();
        } catch (IOException ex) {
            return false;
        }
    }

    private static String normalizeOpenApi32To31(String content) {
        Matcher yamlMatcher = OPENAPI_32_YAML_PATTERN.matcher(content);
        String rewritten = yamlMatcher.replaceFirst("openapi: 3.1.0");
        Matcher jsonMatcher = OPENAPI_32_JSON_PATTERN.matcher(rewritten);
        return jsonMatcher.replaceFirst("$13.1.0$2");
    }
}
