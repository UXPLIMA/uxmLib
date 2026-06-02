package com.uxplima.uxmlib.hook.economy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;
import net.milkbowl.vault2.economy.EconomyResponse.ResponseType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Maps the VaultUnlocked vault2 provider down onto the {@link EconomyBridge} surface: balances read through
 * getBalance, transactions through withdraw/deposit (honouring the EconomyResponse outcome), and the default
 * currency's names/format. The provider is a Mockito double, so no real economy plugin is needed.
 */
@SuppressWarnings("deprecation") // stubs the same default-currency convenience methods the view deliberately uses
class VaultUnlockedEconomyTest {

    private ServerMock server;
    private Economy provider;
    private PlayerMock player;
    private UUID id;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        provider = mock(Economy.class);
        player = server.addPlayer();
        id = player.getUniqueId();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static EconomyResponse outcome(boolean success) {
        return new EconomyResponse(
                BigDecimal.TEN, BigDecimal.ZERO, success ? ResponseType.SUCCESS : ResponseType.FAILURE, "");
    }

    @Test
    void readsBalanceAndChecksHave() {
        when(provider.getBalance("uxmlib", id)).thenReturn(new BigDecimal("100.0"));
        VaultUnlockedEconomy economy = VaultUnlockedEconomy.of(provider);

        assertThat(economy.balance(player)).isEqualTo(100.0);
        assertThat(economy.has(player, 50)).isTrue();
        assertThat(economy.has(player, 200)).isFalse();
    }

    @Test
    void mapsWithdrawAndDepositToTheTransactionOutcome() {
        when(provider.withdraw(eq("uxmlib"), eq(id), any())).thenReturn(outcome(true));
        when(provider.deposit(eq("uxmlib"), eq(id), any())).thenReturn(outcome(false));
        VaultUnlockedEconomy economy = VaultUnlockedEconomy.of(provider);

        assertThat(economy.withdraw(player, 10)).isTrue();
        assertThat(economy.deposit(player, 10)).isFalse();
    }

    @Test
    void delegatesFormatAndCurrencyNames() {
        when(provider.format(any())).thenReturn("$100.00");
        when(provider.defaultCurrencyNameSingular("uxmlib")).thenReturn("Dollar");
        when(provider.defaultCurrencyNamePlural("uxmlib")).thenReturn("Dollars");
        VaultUnlockedEconomy economy = VaultUnlockedEconomy.of(provider);

        assertThat(economy.format(100)).isEqualTo("$100.00");
        assertThat(economy.currencyNameSingular()).isEqualTo("Dollar");
        assertThat(economy.currencyNamePlural()).isEqualTo("Dollars");
    }

    @Test
    void toBridgeExposesThePresentBackend() {
        when(provider.getBalance("uxmlib", id)).thenReturn(new BigDecimal("42"));
        EconomyBridge bridge = VaultUnlockedEconomy.of(provider).toBridge().orElseThrow();

        assertThat(bridge.isPresent()).isTrue();
        assertThat(bridge.balance(player)).isEqualTo(42.0);
    }
}
