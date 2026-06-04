package com.uxplima.uxmlib.command.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.command.CommandSender;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.uxplima.uxmlib.command.Sender;
import com.uxplima.uxmlib.command.annotation.annotations.Arg;
import com.uxplima.uxmlib.command.annotation.annotations.Command;
import com.uxplima.uxmlib.command.annotation.annotations.Subcommand;
import org.junit.jupiter.api.Test;

/** Covers optional, default, and greedy trailing arguments and their registration-time ordering rules. */
class OptionalArgsTest {

    @Command(name = "opt")
    static class OptionalCommand {
        @Subcommand("page")
        void page(Sender sender, @Arg(value = "n", optional = true, def = "1") int n) {}

        @Subcommand("broadcast")
        void broadcast(Sender sender, @Arg(value = "message", greedy = true) String message) {}
    }

    @Command(name = "bad")
    static class RequiredAfterOptional {
        @Subcommand("go")
        void go(Sender sender, @Arg(value = "a", optional = true) String a, @Arg("b") String b) {}
    }

    @Command(name = "badgreedy")
    static class GreedyNotLast {
        @Subcommand("go")
        void go(Sender sender, @Arg(value = "msg", greedy = true) String msg, @Arg("n") int n) {}
    }

    @Test
    void optionalArgMakesThePrefixExecutable() {
        LiteralCommandNode<CommandSourceStack> node = AnnotatedCommands.buildNode(new OptionalCommand());
        CommandNode<CommandSourceStack> page = node.getChild("page");
        assertThat(page).isNotNull();
        // The optional arg exists as a child...
        assertThat(java.util.Objects.requireNonNull(page).getChild("n")).isNotNull();
        // ...and the literal itself is executable (running /opt page with no number is valid).
        assertThat(java.util.Objects.requireNonNull(page).getCommand()).isNotNull();
    }

    @Test
    void greedyArgIsAccepted() {
        LiteralCommandNode<CommandSourceStack> node = AnnotatedCommands.buildNode(new OptionalCommand());
        CommandNode<CommandSourceStack> bc = node.getChild("broadcast");
        assertThat(bc).isNotNull();
        assertThat(java.util.Objects.requireNonNull(bc).getChild("message")).isNotNull();
    }

    @Test
    void rejectsRequiredAfterOptional() {
        assertThatThrownBy(() -> AnnotatedCommands.buildNode(new RequiredAfterOptional()))
                .isInstanceOf(CommandParseException.class)
                .hasMessageContaining("optional");
    }

    @Test
    void rejectsGreedyNotLast() {
        assertThatThrownBy(() -> AnnotatedCommands.buildNode(new GreedyNotLast()))
                .isInstanceOf(CommandParseException.class)
                .hasMessageContaining("greedy");
    }

    @Command(name = "optstr")
    static class OptionalStringCommand {
        String captured = "UNSET";

        @Subcommand("go")
        void go(Sender sender, @Arg(value = "x", optional = true) String x) {
            captured = x;
        }
    }

    @Test
    void omittedOptionalStringBindsToEmptyNotNull() throws Exception {
        OptionalStringCommand handler = new OptionalStringCommand();
        LiteralCommandNode<CommandSourceStack> node = AnnotatedCommands.buildNode(handler);
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.getRoot().addChild(node);
        CommandSender sender = mock(CommandSender.class);
        CommandSourceStack source = mock(CommandSourceStack.class);
        when(source.getSender()).thenReturn(sender);

        // Omitting the optional String must bind "" (so a consumer's isEmpty() check is NPE-free), not null.
        dispatcher.execute("optstr go", source);

        assertThat(handler.captured).isEqualTo("");
    }
}
