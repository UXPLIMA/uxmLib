package com.uxplima.uxmlib.command.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.uxplima.uxmlib.command.Sender;
import com.uxplima.uxmlib.command.annotation.annotations.Command;
import com.uxplima.uxmlib.command.annotation.annotations.Secret;
import com.uxplima.uxmlib.command.annotation.annotations.Subcommand;
import org.junit.jupiter.api.Test;

/**
 * Covers {@code @}{@link Secret}: the {@link Secrets} predicate recognises a method- or class-level mark, and
 * {@link HelpRenderer} keeps a secret branch out of the generated help while still building and registering
 * its node, so it runs for whoever types it.
 */
class SecretTest {

    @Command(name = "warp")
    static class WarpCommand {
        @Subcommand("list")
        void list(Sender sender) {}

        @Secret
        @Subcommand("debug")
        void debug(Sender sender) {}
    }

    @Command(name = "staff")
    @Secret
    static class StaffCommand {
        @Subcommand("panel")
        void panel(Sender sender) {}
    }

    @Test
    void aMethodMarkedSecretIsRecognised() throws Exception {
        Method debug = WarpCommand.class.getDeclaredMethod("debug", Sender.class);
        Method list = WarpCommand.class.getDeclaredMethod("list", Sender.class);

        assertThat(Secrets.isSecret(debug)).isTrue();
        assertThat(Secrets.isSecret(list)).isFalse();
    }

    @Test
    void aClassMarkedSecretMakesEveryBranchSecret() throws Exception {
        Method panel = StaffCommand.class.getDeclaredMethod("panel", Sender.class);

        assertThat(Secrets.isSecret(panel)).isTrue();
    }

    @Test
    void aSecretBranchIsOmittedFromTheGeneratedHelp() throws Exception {
        Method debug = WarpCommand.class.getDeclaredMethod("debug", Sender.class);
        Method list = WarpCommand.class.getDeclaredMethod("list", Sender.class);

        List<HelpRenderer.Entry> entries = HelpRenderer.entriesOf(List.of(list, debug));

        assertThat(entries).extracting(HelpRenderer.Entry::usage).containsExactly("list");
    }

    @Test
    void aSecretBranchStillBuildsAndRegistersItsNode() {
        LiteralCommandNode<CommandSourceStack> node = AnnotatedCommands.buildNode(new WarpCommand());

        // The node is present and runnable — secrecy is help-only, not a node-removal.
        assertThat(node.getChild("debug")).isNotNull();
        assertThat(node.getChild("list")).isNotNull();
    }
}
