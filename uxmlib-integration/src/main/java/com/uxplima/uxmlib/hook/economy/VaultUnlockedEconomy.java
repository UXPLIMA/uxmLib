package com.uxplima.uxmlib.hook.economy;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.uxplima.uxmlib.hook.Hooks;
import net.milkbowl.vault2.economy.Economy;

/**
 * A present-guarded view of the VaultUnlocked economy — the {@code net.milkbowl.vault2.economy} provider, a
 * separate plugin from classic Vault with a {@code BigDecimal}, UUID-keyed, multi-currency API. {@link #find()}
 * looks the service up and returns empty when VaultUnlocked (or a provider for it) is absent, so
 * {@link EconomyBridge#find()} can try it after classic Vault and fall through cleanly. The {@code vault2}
 * classes are touched only past the registration check, so a server without VaultUnlocked still loads.
 *
 * <p>Every vault2 call is keyed by a requesting plugin name; the library passes a stable {@code "uxmlib"}
 * identifier (economy plugins use it only for logging / per-plugin scoping). Amounts cross the {@code double}
 * bridge surface as {@link BigDecimal} against the provider's default currency.
 */
public final class VaultUnlockedEconomy {

    private static final String PLUGIN_NAME = "uxmlib";

    private final Economy economy;

    private VaultUnlockedEconomy(Economy economy) {
        this.economy = economy;
    }

    /** The registered VaultUnlocked economy, or empty when VaultUnlocked or a provider for it is absent. */
    public static Optional<VaultUnlockedEconomy> find() {
        if (!Hooks.isPresent("VaultUnlocked")) {
            return Optional.empty();
        }
        RegisteredServiceProvider<Economy> registration =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            return Optional.empty();
        }
        // A registration always carries a non-null provider; the unannotated vault2 API hides that from NullAway.
        Economy provider = Objects.requireNonNull(registration.getProvider(), "provider");
        return Optional.of(new VaultUnlockedEconomy(provider));
    }

    /** Wrap an already-resolved {@code economy} — the seam {@link #find()} uses, exposed for tests. */
    static VaultUnlockedEconomy of(Economy economy) {
        return new VaultUnlockedEconomy(Objects.requireNonNull(economy, "economy"));
    }

    /** A player's balance in the default currency. */
    @SuppressWarnings("deprecation") // the single-currency convenience accessor is the right default-currency surface
    public double balance(OfflinePlayer player) {
        Objects.requireNonNull(player, "player");
        return Objects.requireNonNullElse(economy.getBalance(PLUGIN_NAME, player.getUniqueId()), BigDecimal.ZERO)
                .doubleValue();
    }

    /** Whether a player has at least {@code amount} (VaultUnlocked has no direct check, so the balance is read). */
    public boolean has(OfflinePlayer player, double amount) {
        return balance(player) >= amount;
    }

    /** Withdraw {@code amount} from a player; returns whether the transaction succeeded. */
    public boolean withdraw(OfflinePlayer player, double amount) {
        Objects.requireNonNull(player, "player");
        return economy.withdraw(PLUGIN_NAME, player.getUniqueId(), BigDecimal.valueOf(amount))
                .transactionSuccess();
    }

    /** Deposit {@code amount} to a player; returns whether the transaction succeeded. */
    public boolean deposit(OfflinePlayer player, double amount) {
        Objects.requireNonNull(player, "player");
        return economy.deposit(PLUGIN_NAME, player.getUniqueId(), BigDecimal.valueOf(amount))
                .transactionSuccess();
    }

    /** The provider's own rendering of {@code amount}. */
    @SuppressWarnings("deprecation") // the default-currency format convenience method is exactly this bridge's need
    public String format(double amount) {
        return Objects.requireNonNullElse(economy.format(BigDecimal.valueOf(amount)), "");
    }

    /** The default currency's singular name. */
    public String currencyNameSingular() {
        return Objects.requireNonNullElse(economy.defaultCurrencyNameSingular(PLUGIN_NAME), "");
    }

    /** The default currency's plural name. */
    public String currencyNamePlural() {
        return Objects.requireNonNullElse(economy.defaultCurrencyNamePlural(PLUGIN_NAME), "");
    }

    /** Adapt this view to an {@link EconomyBridge}. */
    Optional<EconomyBridge> toBridge() {
        return Optional.of(new VaultUnlockedEconomyBridge(this));
    }
}
