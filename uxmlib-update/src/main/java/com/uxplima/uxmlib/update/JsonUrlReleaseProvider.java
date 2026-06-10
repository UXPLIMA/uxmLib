package com.uxplima.uxmlib.update;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

/**
 * An {@link UpdateProvider} backed by any release-JSON endpoint a server owner points it at, rather than a
 * specific host's API shape. The body is read with the real {@link Json} reader for the first present of a list
 * of version fields (by default {@code tag_name} then {@code version}, the two shapes GitHub and a plain
 * release manifest expose); the human page is {@code html_url} when present, falling back to the configured
 * endpoint itself so a notice always has a link to open. Any non-2xx or unparseable response degrades to "no
 * release", the same contract as {@link GitHubReleaseProvider} and {@link ModrinthReleaseProvider}.
 */
public final class JsonUrlReleaseProvider implements UpdateProvider {

    // The version fields tried in order when none are given: a GitHub-style tag first, then a plain "version".
    private static final List<String> DEFAULT_VERSION_FIELDS = List.of("tag_name", "version");
    private static final String PAGE_FIELD = "html_url";

    private final URI endpoint;
    private final List<String> versionFields;

    /**
     * Read from {@code url} using the default version fields ({@code tag_name} then {@code version}).
     *
     * @param url the release-JSON endpoint to query
     */
    public JsonUrlReleaseProvider(String url) {
        this(url, DEFAULT_VERSION_FIELDS);
    }

    /**
     * Read from {@code endpoint} using the default version fields ({@code tag_name} then {@code version}).
     *
     * @param endpoint the release-JSON endpoint to query
     */
    public JsonUrlReleaseProvider(URI endpoint) {
        this(endpoint, DEFAULT_VERSION_FIELDS);
    }

    /**
     * @param url the release-JSON endpoint to query
     * @param versionFields the body fields tried in order for the version string (first present wins)
     */
    public JsonUrlReleaseProvider(String url, List<String> versionFields) {
        this(parse(url), versionFields);
    }

    /**
     * @param endpoint the release-JSON endpoint to query
     * @param versionFields the body fields tried in order for the version string (first present wins)
     */
    public JsonUrlReleaseProvider(URI endpoint, List<String> versionFields) {
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(versionFields, "versionFields");
        requireWebUri(endpoint);
        List<String> fields = List.copyOf(versionFields);
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("versionFields must not be empty");
        }
        this.endpoint = endpoint;
        this.versionFields = fields;
    }

    @Override
    public CompletableFuture<Optional<Release>> latest() {
        return Http.getJson(endpoint, body -> parseLatest(body, versionFields, endpoint));
    }

    /** The endpoint this provider queries. Exposed for wiring/diagnostics and tested. */
    public URI endpoint() {
        return endpoint;
    }

    /** Pull a {@link Release} out of a release-JSON body. Pure and tested; never throws. */
    static Optional<Release> parseLatest(String body, List<String> versionFields, URI endpoint) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(versionFields, "versionFields");
        Objects.requireNonNull(endpoint, "endpoint");
        @Nullable Object root;
        try {
            root = Json.parse(body);
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
        Optional<String> version = firstField(root, versionFields);
        if (version.isEmpty()) {
            return Optional.empty();
        }
        String page = Json.string(root, PAGE_FIELD).orElseGet(endpoint::toString);
        return toRelease(version.get(), page);
    }

    // The first present-and-string field of {@code fields}, or empty when none is a string on the node.
    private static Optional<String> firstField(@Nullable Object node, List<String> fields) {
        for (String field : fields) {
            Optional<String> value = Json.string(node, field);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    private static Optional<Release> toRelease(String version, String url) {
        try {
            return Optional.of(new Release(version, url));
        } catch (IllegalArgumentException rejected) {
            return Optional.empty();
        }
    }

    private static URI parse(String url) {
        Objects.requireNonNull(url, "url");
        try {
            return new URI(url);
        } catch (java.net.URISyntaxException malformed) {
            throw new IllegalArgumentException("malformed endpoint URL: " + url, malformed);
        }
    }

    private static void requireWebUri(URI endpoint) {
        String scheme = endpoint.getScheme();
        if (endpoint.getHost() == null || scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
            throw new IllegalArgumentException("endpoint must be an absolute http/https URL: " + endpoint);
        }
    }
}
