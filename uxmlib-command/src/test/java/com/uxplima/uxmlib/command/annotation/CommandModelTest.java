package com.uxplima.uxmlib.command.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.papermc.paper.command.brigadier.CommandSourceStack;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.uxplima.uxmlib.command.Sender;
import com.uxplima.uxmlib.command.annotation.annotations.Arg;
import com.uxplima.uxmlib.command.annotation.annotations.Command;
import com.uxplima.uxmlib.command.annotation.annotations.Permission;
import com.uxplima.uxmlib.command.annotation.annotations.Subcommand;
import org.junit.jupiter.api.Test;

/**
 * Verifies the reflect &rarr; model &rarr; render seam: {@link CommandModels} produces a platform-neutral
 * {@link CommandModel} whose branches carry the right path, permission, and ordered arguments, and a
 * {@link BrigadierRenderer} walking that model emits the same Brigadier tree the public {@code buildNode}
 * does. Asserting the model directly proves the IR exists between reflection and Brigadier, not just that
 * the final tree is correct.
 */
class CommandModelTest {

    @Command(name = "shop", aliases = "store", description = "Open the shop")
    static class ShopCommand {
        @Subcommand("")
        void root(Sender sender) {}

        @Subcommand("buy")
        void buy(Sender sender, @Arg("item") String item, @Arg(value = "amount", min = 1, max = 64) int amount) {}

        @Subcommand("admin reload")
        @Permission("shop.admin")
        void reload(CommandSourceStack source) {}
    }

    @Test
    void reflectProducesBranchesWithPathPermissionAndArgs() {
        CommandModel model = CommandModels.reflect(new ShopCommand(), ParamResolvers.withDefaults());

        assertThat(model.name()).isEqualTo("shop");
        assertThat(model.command().aliases()).containsExactly("store");

        BranchModel buy = branch(model, "buy");
        assertThat(buy.permission()).isNull();
        assertThat(buy.args()).extracting(ArgBinder.ParamArg::name).containsExactly("item", "amount");
        assertThat(buy.hasFlags()).isFalse();

        BranchModel reload = branch(model, "admin reload");
        assertThat(reload.permission()).isNotNull();
        assertThat(java.util.Objects.requireNonNull(reload.permission()).value())
                .isEqualTo("shop.admin");
        assertThat(reload.literals()).containsExactly("admin", "reload");

        BranchModel root = branch(model, "");
        assertThat(root.args()).isEmpty();
        assertThat(root.literals()).isEmpty();
    }

    @Test
    void renderingTheModelEmitsTheBrigadierTree() {
        CommandModel model = CommandModels.reflect(new ShopCommand(), ParamResolvers.withDefaults());
        LiteralCommandNode<CommandSourceStack> node =
                new BrigadierRenderer(ParamResolvers.withDefaults(), new SameThreadScheduler()).render(model);

        assertThat(node.getLiteral()).isEqualTo("shop");
        CommandNode<CommandSourceStack> buy = node.getChild("buy");
        assertThat(buy).isNotNull();
        CommandNode<CommandSourceStack> item =
                java.util.Objects.requireNonNull(buy).getChild("item");
        assertThat(item).isNotNull();
        assertThat(java.util.Objects.requireNonNull(item).getChild("amount")).isNotNull();
        CommandNode<CommandSourceStack> admin = node.getChild("admin");
        assertThat(java.util.Objects.requireNonNull(admin).getChild("reload")).isNotNull();
    }

    @Test
    void reflectRejectsAMalformedHandlerBeforeRendering() {
        assertThatThrownBy(() -> CommandModels.reflect(new Object(), ParamResolvers.withDefaults()))
                .isInstanceOf(CommandParseException.class);
    }

    private static BranchModel branch(CommandModel model, String path) {
        return model.branches().stream()
                .filter(b -> b.path().equals(path))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no branch with path '" + path + "'"));
    }
}
