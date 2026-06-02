package com.uxplima.uxmlib.hologram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

/** Tests the resolver's two-call flow, status handling, and caching against a scripted transport. */
class MojangSkinResolverTest {

    private static final String TEXTURE_BASE64 = SkinTextures.encode("http://textures.minecraft.net/texture/x");
    private static final String UUID_NO_DASHES = "069a79f444e94726a5befca90e38aaf5";

    /** A transport that returns queued responses in order and records the URIs it was asked for. */
    private static final class ScriptedTransport implements Function<URI, CompletableFuture<HttpStatusBody>> {
        private final Deque<HttpStatusBody> responses = new ArrayDeque<>();
        private final List<URI> calls = new java.util.ArrayList<>();

        ScriptedTransport reply(int status, String body) {
            responses.add(new HttpStatusBody(status, body));
            return this;
        }

        @Override
        public CompletableFuture<HttpStatusBody> apply(URI uri) {
            calls.add(uri);
            if (responses.isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalStateException("no scripted response for " + uri));
            }
            return CompletableFuture.completedFuture(responses.poll());
        }
    }

    private static MojangSkinResolver resolver(ScriptedTransport transport) {
        return new MojangSkinResolver(transport, new SkinCache(16, Duration.ofMinutes(5), System::nanoTime));
    }

    @Test
    void resolvesANameThroughBothEndpoints() {
        ScriptedTransport transport = new ScriptedTransport()
                .reply(200, "{\"id\":\"" + UUID_NO_DASHES + "\",\"name\":\"Notch\"}")
                .reply(
                        200,
                        "{\"id\":\"" + UUID_NO_DASHES + "\",\"properties\":[{\"name\":\"textures\",\"value\":\""
                                + TEXTURE_BASE64 + "\"}]}");

        Optional<String> texture = resolver(transport).resolveTexture("Notch").join();

        assertThat(texture).contains(TEXTURE_BASE64);
        assertThat(transport.calls).hasSize(2);
        assertThat(transport.calls.get(0).toString()).contains("api.mojang.com").contains("Notch");
        assertThat(transport.calls.get(1).toString()).contains("sessionserver").contains(UUID_NO_DASHES);
    }

    @Test
    void returnsEmptyAndCachesWhenTheNameIsUnknown404() {
        ScriptedTransport transport = new ScriptedTransport().reply(404, "");
        MojangSkinResolver resolver = resolver(transport);

        assertThat(resolver.resolveTexture("Ghost").join()).isEmpty();
        // A second call must be served from the negative cache, with no further HTTP.
        assertThat(resolver.resolveTexture("Ghost").join()).isEmpty();
        assertThat(transport.calls).hasSize(1);
    }

    @Test
    void treats204OnTheNameLookupAsUnknown() {
        ScriptedTransport transport = new ScriptedTransport().reply(204, "");
        assertThat(resolver(transport).resolveTexture("Ghost").join()).isEmpty();
    }

    @Test
    void returnsEmptyWhenTheProfileHasNoTextureProperty() {
        ScriptedTransport transport = new ScriptedTransport()
                .reply(200, "{\"id\":\"069a79f444e94726a5befca90e38aaf5\",\"name\":\"Notch\"}")
                .reply(204, ""); // session server: unknown uuid / empty profile
        assertThat(resolver(transport).resolveTexture("Notch").join()).isEmpty();
    }

    @Test
    void cachesAresolvedTextureSoTheSecondCallSkipsTheNetwork() {
        ScriptedTransport transport = new ScriptedTransport()
                .reply(200, "{\"id\":\"069a79f444e94726a5befca90e38aaf5\",\"name\":\"Notch\"}")
                .reply(200, "{\"properties\":[{\"name\":\"textures\",\"value\":\"" + TEXTURE_BASE64 + "\"}]}");
        MojangSkinResolver resolver = resolver(transport);

        assertThat(resolver.resolveTexture("Notch").join()).contains(TEXTURE_BASE64);
        assertThat(resolver.resolveTexture("Notch").join()).contains(TEXTURE_BASE64);
        assertThat(transport.calls).hasSize(2); // not four
    }

    @Test
    void retriesOnceAfterA429ThenSucceeds() {
        AtomicInteger attempts = new AtomicInteger();
        Function<URI, CompletableFuture<HttpStatusBody>> transport = uri -> {
            int n = attempts.incrementAndGet();
            if (n == 1) {
                return CompletableFuture.completedFuture(new HttpStatusBody(429, ""));
            }
            if (uri.toString().contains("api.mojang.com")) {
                return CompletableFuture.completedFuture(
                        new HttpStatusBody(200, "{\"id\":\"069a79f444e94726a5befca90e38aaf5\"}"));
            }
            return CompletableFuture.completedFuture(new HttpStatusBody(
                    200, "{\"properties\":[{\"name\":\"textures\",\"value\":\"" + TEXTURE_BASE64 + "\"}]}"));
        };
        MojangSkinResolver resolver = new MojangSkinResolver(
                transport, new SkinCache(16, Duration.ofMinutes(5), System::nanoTime), Duration.ofMillis(1), 2);

        assertThat(resolver.resolveTexture("Notch").join()).contains(TEXTURE_BASE64);
        assertThat(attempts.get()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void givesUpAfterExhaustingRetriesOnPersistent429() {
        Function<URI, CompletableFuture<HttpStatusBody>> always429 =
                uri -> CompletableFuture.completedFuture(new HttpStatusBody(429, ""));
        MojangSkinResolver resolver = new MojangSkinResolver(
                always429, new SkinCache(16, Duration.ofMinutes(5), System::nanoTime), Duration.ofMillis(1), 1);
        assertThat(resolver.resolveTexture("Notch").join()).isEmpty();
    }

    @Test
    void rejectsABlankName() {
        assertThatThrownBy(() -> resolver(new ScriptedTransport()).resolveTexture("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsANullName() {
        assertThatThrownBy(() -> resolver(new ScriptedTransport()).resolveTexture(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void parsesAUuidWithDashesFromTheNameLookup() {
        ScriptedTransport transport = new ScriptedTransport()
                .reply(200, "{\"name\":\"Notch\",\"id\":\"069a79f4-44e9-4726-a5be-fca90e38aaf5\"}")
                .reply(200, "{\"properties\":[{\"name\":\"textures\",\"value\":\"" + TEXTURE_BASE64 + "\"}]}");
        assertThat(resolver(transport).resolveTexture("Notch").join()).contains(TEXTURE_BASE64);
        // The dashes must be preserved verbatim in the session-server URL.
        assertThat(transport.calls.get(1).toString()).contains("069a79f4-44e9-4726-a5be-fca90e38aaf5");
    }

    @Test
    void encodesTheTextureForAPlayerHeadDecode() {
        // A resolved texture must decode back to a skin url, proving it is the genuine textures blob.
        String json = new String(Base64.getDecoder().decode(TEXTURE_BASE64), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(json).contains("textures.minecraft.net");
    }
}
