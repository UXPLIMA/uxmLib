package com.uxplima.uxmlib.gui.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.uxplima.uxmlib.gui.ClickContext;
import com.uxplima.uxmlib.gui.GuiResponse;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

/**
 * The declarative {@link GuiAction.Responding} variant: an additive opt-in action whose handler is a pure
 * function {@code ClickContext -> CompletableFuture<List<GuiResponse>>}. It must coexist with the existing
 * imperative {@link GuiAction.Run}/{@link GuiAction.None} contract.
 */
class GuiActionRespondingTest {

    @Test
    void respondingIsAGuiAction() {
        GuiAction action = new GuiAction.Responding(ctx -> GuiResponse.completed(List.of(GuiResponse.close())));

        assertThat(action).isInstanceOf(GuiAction.class);
    }

    @Test
    void syncFactoryWrapsAListInACompletedFuture() {
        MockBukkit.mock();
        try {
            GuiAction.Responding action = GuiItem.respondingSync(ctx -> List.of(GuiResponse.refresh()));
            ClickContext context = sampleContext();

            CompletableFuture<List<GuiResponse>> future = action.handler().apply(context);

            assertThat(future.isDone()).isTrue();
            assertThat(future.join()).containsExactly(GuiResponse.refresh());
        } finally {
            MockBukkit.unmock();
        }
    }

    @Test
    void asyncFactoryPassesTheFutureThrough() {
        CompletableFuture<List<GuiResponse>> pending = new CompletableFuture<>();
        GuiAction.Responding action = GuiItem.respondingAsync(ctx -> pending);

        assertThat(action.handler()).isNotNull();
    }

    private static ClickContext sampleContext() {
        var player = MockBukkit.getMock().addPlayer();
        return new ClickContext(
                player,
                0,
                org.bukkit.event.inventory.ClickType.LEFT,
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE),
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
    }
}
