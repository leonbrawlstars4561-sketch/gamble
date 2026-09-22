package de.gamble.plugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Sets up and holds the Vault {@link Economy} provider.
 * The plugin never crashes with a NullPointerException if Vault or an
 * economy plugin is missing - {@link #isAvailable()} must be checked
 * before using {@link #getEconomy()}.
 */
public final class VaultHook {

    private final JavaPlugin plugin;
    private Economy economy;

    public VaultHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Attempts to hook into Vault and an Economy provider.
     *
     * @return true if an economy provider was found and hooked successfully
     */
    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault wurde nicht gefunden! Bitte installiere Vault.");
            return false;
        }

        plugin.getLogger().info("Vault gefunden.");

        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            plugin.getLogger().severe("Kein Vault Economy Provider gefunden! Bitte installiere ein kompatibles Economy-Plugin.");
            return false;
        }

        this.economy = provider.getProvider();
        if (this.economy == null) {
            plugin.getLogger().severe("Kein Vault Economy Provider gefunden! Bitte installiere ein kompatibles Economy-Plugin.");
            return false;
        }

        plugin.getLogger().info("Economy gefunden: " + economy.getName());
        return true;
    }

    /**
     * @return true if a working economy provider is currently available
     */
    public boolean isAvailable() {
        return economy != null;
    }

    /**
     * @return the hooked Economy instance, may be null if {@link #isAvailable()} is false
     */
    public Economy getEconomy() {
        return economy;
    }
}
