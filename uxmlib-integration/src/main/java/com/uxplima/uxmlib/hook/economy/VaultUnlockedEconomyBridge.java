package com.uxplima.uxmlib.hook.economy;

import java.util.Objects;

import org.bukkit.OfflinePlayer;

/** An {@link EconomyBridge} backed by the present-guarded {@link VaultUnlockedEconomy} view. */
final class VaultUnlockedEconomyBridge implements EconomyBridge {

    private final VaultUnlockedEconomy economy;

    VaultUnlockedEconomyBridge(VaultUnlockedEconomy economy) {
        this.economy = Objects.requireNonNull(economy, "economy");
    }

    @Override
    public double balance(OfflinePlayer player) {
        return economy.balance(player);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return economy.has(player, amount);
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        return economy.withdraw(player, amount);
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        return economy.deposit(player, amount);
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public String format(double amount) {
        return economy.format(amount);
    }

    @Override
    public String currencySymbol() {
        // VaultUnlocked exposes no separate symbol; its default singular currency name is the closest surface.
        return economy.currencyNameSingular();
    }

    @Override
    public String currencyNameSingular() {
        return economy.currencyNameSingular();
    }

    @Override
    public String currencyNamePlural() {
        return economy.currencyNamePlural();
    }
}
