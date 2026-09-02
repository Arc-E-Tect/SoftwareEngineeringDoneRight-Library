package com.arc_e_tect.gradle.detector.core.openapi;

import com.arc_e_tect.gradle.detector.core.model.HttpVerb;
import com.arc_e_tect.gradle.detector.core.model.PathTemplates;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
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

    /**
     * Matches a {@code $ref} entry's target in either YAML ({@code $ref: 'foo.yaml#/...'}) or
     * JSON ({@code "$ref": "foo.json#/..."}) syntax, capturing everything up to the closing quote
     * or a {@code #} fragment marker, whichever comes first. A same-document ref (starting
     * directly with {@code #}) captures an empty group.
     */
    private static final Pattern REF_TARGET_PATTERN = Pattern.compile("\\$ref\\s*:\\s*[\"']([^\"'#]*)");

    /** Creates a new {@code OpenApiEndpointCollector}. */
    public OpenApiEndpointCollector() {}

    /**
     * Parses {@code rootDocument} and every document it links to (relative {@code $ref}s are
     * resolved automatically), and returns the verb + path template pair described by every
     * operation found. Equivalent to {@link #collect(File, Consumer)} with a callback that does
     * nothing.
     *
     * @param rootDocument the root OpenAPI document (JSON or YAML)
     * @return possibly-empty list of described endpoints, never {@code null}
     * @throws IllegalStateException if the document cannot be parsed
     */
    public List<DescribedEndpoint> collect(File rootDocument) {
        return collect(rootDocument, file -> { });
    }

    /**
     * Parses {@code rootDocument} and every document it links to (relative {@code $ref}s are
     * resolved automatically), and returns the verb + path template pair described by every
     * operation found.
     *
     * <p>{@code onDocumentResolved} is invoked once for {@code rootDocument} itself and once for
     * every distinct document reachable from it via a relative {@code $ref}, so a caller can drive
     * a progress indicator during what would otherwise be a single opaque, potentially long-running
     * call - the underlying parser resolves {@code $ref}s internally and offers no such callback of
     * its own. The set of documents is discovered by a lightweight, best-effort textual scan for
     * {@code $ref} entries (the same "read the file as data" approach the plugins' own WireMock and
     * Spring Cloud Contract scanners use), run <em>before</em> the real parse; it does not replace
     * or influence actual {@code $ref} resolution, which is still performed by the parser exactly
     * as it is for {@link #collect(File)}. A document that can't be read for this discovery pass is
     * silently skipped - the real parse below still surfaces the failure normally.</p>
     *
     * @param rootDocument       the root OpenAPI document (JSON or YAML)
     * @param onDocumentResolved invoked once per distinct document discovered, including the root;
     *                           never {@code null}
     * @return possibly-empty list of described endpoints, never {@code null}
     * @throws IllegalStateException if the document cannot be parsed
     */
    public List<DescribedEndpoint> collect(File rootDocument, Consumer<File> onDocumentResolved) {
        discoverReferencedDocuments(rootDocument, new HashSet<>(), onDocumentResolved);
        OpenAPI openApi = parseOpenApi(rootDocument);

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
                        operationTags(operation),
                        operationResponseCodes(operation)))));
        return endpoints;
    }

    /**
     * Parses {@code rootDocument} and returns the path component of its first declared
     * {@code servers} entry's {@code url} - e.g. {@code http://localhost:9011/user-account-service} yields
     * {@code /user-account-service} - normalised via {@link PathTemplates#normalize(String)}.
     *
     * <p>An OpenAPI document's {@code paths} are always relative to that base path: a client
     * actually requests {@code <server url>/<path>}, but every operation's declared path (and every
     * path a {@code @RequestMapping}-derived scan produces) omits it. A WireMock stub mapping, in
     * contrast, records the full request path a client actually sends, base path included. This is
     * the default source for stripping it back off before comparing the two - see
     * {@code mirageApiDetector.basePath} in the Mirage API Detector plugin, which falls back to
     * this method's result when left unconfigured.</p>
     *
     * @param rootDocument the root OpenAPI document (JSON or YAML)
     * @return the first server's base path, or {@link Optional#empty()} when the document declares
     *         no {@code servers} entry, its {@code url} is blank, or that URL has no path component
     *         (e.g. {@code http://localhost:9011} alone, with nothing to strip)
     * @throws IllegalStateException if the document cannot be parsed
     */
    public Optional<String> firstServerBasePath(File rootDocument) {
        OpenAPI openApi = parseOpenApi(rootDocument);
        List<Server> servers = openApi.getServers();
        if (servers == null || servers.isEmpty()) {
            return Optional.empty();
        }
        String url = servers.get(0).getUrl();
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        String path;
        try {
            path = new URI(url).getPath();
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
        if (path == null || path.isBlank() || path.equals("/")) {
            return Optional.empty();
        }
        return Optional.of(PathTemplates.normalize(path));
    }

    private OpenAPI parseOpenApi(File rootDocument) {
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
        return openApi;
    }

    private static List<String> operationTags(Operation operation) {
        return operation.getTags() == null ? List.of() : List.copyOf(operation.getTags());
    }

    /**
     * Returns the operation's declared response codes exactly as keyed in its {@code responses}
     * map (e.g. {@code "200"}, {@code "404"}, {@code "5XX"}, {@code "default"}), sorted.
     */
    private static List<String> operationResponseCodes(Operation operation) {
        if (operation.getResponses() == null) {
            return List.of();
        }
        return operation.getResponses().keySet().stream().sorted().toList();
    }

    /**
     * Recursively discovers every document reachable from {@code document} via a relative
     * {@code $ref}, invoking {@code onDocumentResolved} once for each distinct document the first
     * time it's encountered. {@code visited} guards against revisiting a document already seen -
     * both to avoid infinite recursion on a {@code $ref} cycle and to guarantee each document is
     * reported at most once.
     */
    private void discoverReferencedDocuments(File document, Set<File> visited, Consumer<File> onDocumentResolved) {
        if (!visited.add(canonicalOrAbsolute(document))) {
            return;
        }
        onDocumentResolved.accept(document);
        if (!document.isFile()) {
            return;
        }

        String content;
        try {
            content = Files.readString(document.toPath());
        } catch (IOException e) {
            return;
        }

        Matcher matcher = REF_TARGET_PATTERN.matcher(content);
        while (matcher.find()) {
            String refPath = matcher.group(1);
            if (refPath.isBlank() || refPath.contains("://")) {
                continue;
            }
            discoverReferencedDocuments(new File(document.getParentFile(), refPath), visited, onDocumentResolved);
        }
    }

    private File canonicalOrAbsolute(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            return file.getAbsoluteFile();
        }
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
