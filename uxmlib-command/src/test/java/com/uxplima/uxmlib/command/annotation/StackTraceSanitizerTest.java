package com.uxplima.uxmlib.command.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

/**
 * The {@link StackTraceSanitizer} trims the framework/reflection frames that bury the real cause of a
 * handler exception before it is logged. Pure: it rewrites a throwable's frames in place, so it is exercised
 * directly with hand-built {@link StackTraceElement} arrays rather than a live dispatch.
 */
class StackTraceSanitizerTest {

    private static StackTraceElement frame(String className, String method) {
        return new StackTraceElement(className, method, "Src.java", 1);
    }

    @Test
    void trimsLeadingFrameworkAndReflectionFramesUpToTheConsumerCode() {
        StackTraceElement[] raw = {
            frame("com.mojang.brigadier.CommandDispatcher", "execute"),
            frame("com.uxplima.uxmlib.command.annotation.CommandExecutors", "invoke"),
            frame("jdk.internal.reflect.DirectMethodHandleAccessor", "invoke"),
            frame("java.lang.reflect.Method", "invoke"),
            frame("com.acme.plugin.HomeCommand", "setHome"),
            frame("com.acme.plugin.Repo", "save"),
        };

        StackTraceElement[] trimmed = StackTraceSanitizer.trimLeadingNoise(raw);

        assertThat(trimmed)
                .extracting(StackTraceElement::getClassName)
                .containsExactly("com.acme.plugin.HomeCommand", "com.acme.plugin.Repo");
    }

    @Test
    void keepsTheTraceWhenEveryFrameIsFramework() {
        StackTraceElement[] raw = {
            frame("com.uxplima.uxmlib.command.annotation.CommandExecutors", "invoke"),
            frame("java.lang.reflect.Method", "invoke"),
        };

        assertThat(StackTraceSanitizer.trimLeadingNoise(raw)).isSameAs(raw);
    }

    @Test
    void keepsTheTraceWhenItAlreadyStartsAtConsumerCode() {
        StackTraceElement[] raw = {
            frame("com.acme.plugin.HomeCommand", "setHome"), frame("com.acme.plugin.Repo", "save"),
        };

        assertThat(StackTraceSanitizer.trimLeadingNoise(raw)).isSameAs(raw);
    }

    @Test
    void sanitizeRewritesTheThrowableInPlaceAndReturnsIt() {
        RuntimeException boom = new RuntimeException("boom");
        boom.setStackTrace(new StackTraceElement[] {
            frame("java.lang.reflect.Method", "invoke"), frame("com.acme.plugin.HomeCommand", "setHome"),
        });

        Throwable sanitized = StackTraceSanitizer.sanitize(boom);

        assertThat(sanitized).isSameAs(boom);
        assertThat(boom.getStackTrace())
                .extracting(StackTraceElement::getClassName)
                .containsExactly("com.acme.plugin.HomeCommand");
    }

    @Test
    void sanitizeUnwrapsAReflectionInvocationTargetException() {
        IllegalStateException real = new IllegalStateException("real cause");
        real.setStackTrace(new StackTraceElement[] {
            frame("jdk.internal.reflect.DirectMethodHandleAccessor", "invoke"),
            frame("com.acme.plugin.HomeCommand", "setHome"),
        });
        InvocationTargetException wrapper = new InvocationTargetException(real);

        Throwable sanitized = StackTraceSanitizer.sanitize(wrapper);

        assertThat(sanitized).isSameAs(real);
        assertThat(real.getStackTrace())
                .extracting(StackTraceElement::getClassName)
                .containsExactly("com.acme.plugin.HomeCommand");
    }

    @Test
    void sanitizeReturnsNullForNull() {
        assertThat(StackTraceSanitizer.sanitize(null)).isNull();
    }
}
