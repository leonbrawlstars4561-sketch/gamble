package de.gamble.plugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Main class of the Gamble plugin.
 * <p>
 * Loads configuration and the fake name list on startup, wires up Vault,
 * registers the /gamble command and cleans up all running tasks when a
 * player disconnects or the plugin is disabled.
 */
public final class GamblePlugin extends JavaPlugin implements Listener {

    private VaultHook vaultHook;
    private GambleManager gambleManager;

    private List<String> fakeNames = Collections.emptyList();

    private long intervalTicks;
    private long minAmount;
    private long maxAmount;
    private long amountStep;
    private int amountStepCount;

    private String paymentMessageTemplate;
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("fake-names.yml");

        this.gambleManager = new GambleManager(this);

        loadConfigValues();
        loadFakeNames();

        this.vaultHook = new VaultHook(this);
        boolean economyReady = vaultHook.setup();
        if (!economyReady) {
            getLogger().severe("Gamble konnte nicht aktiviert werden: keine funktionierende Vault Economy.");
        } else {
            getLogger().info("Gamble erfolgreich aktiviert.");
        }

        getServer().getPluginManager().registerEvents(this, this);

        GambleCommand commandExecutor = new GambleCommand(this);
        var command = getCommand("gamble");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }
    }

    @Override
    public void onDisable() {
        if (gambleManager != null) {
            gambleManager.stopAll();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        gambleManager.stopGamble(event.getPlayer());
    }

    private void loadConfigValues() {
        reloadConfig();
        FileConfiguration config = getConfig();

        int intervalSeconds = config.getInt("interval-seconds", 5);
        if (intervalSeconds <= 0) {
            getLogger().warning("[Gamble] ungueltige interval-seconds Konfiguration, verwende Standardwert 5.");
            intervalSeconds = 5;
        }
        this.intervalTicks = intervalSeconds * 20L;

        long min = config.getLong("min-amount", 50_000_000L);
        long max = config.getLong("max-amount", 1_000_000_000L);
        long step = config.getLong("amount-step", 10_000_000L);

        if (min <= 0 || max <= 0 || step <= 0 || min > max) {
            getLogger().severe("[Gamble] ungueltige Betragskonfiguration! Es werden Standardwerte verwendet (50m-1b, Step 10m).");
            min = 50_000_000L;
            max = 1_000_000_000L;
            step = 10_000_000L;
        }

        if ((max - min) % step != 0) {
            getLogger().warning("[Gamble] max-amount ist mit dem gewaehlten amount-step nicht exakt erreichbar. "
                    + "Der hoechste tatsaechlich erreichbare Betrag wird stattdessen verwendet.");
        }

        this.minAmount = min;
        this.maxAmount = max;
        this.amountStep = step;
        this.amountStepCount = (int) ((max - min) / step) + 1;

        this.paymentMessageTemplate = config.getString("payment-message", "<gray>{name} paid you <green>$ {amount}");

        this.messages = new Messages(
                config.getString("messages.enabled", "<green>Gamble aktiviert!"),
                config.getString("messages.disabled", "<red>Gamble deaktiviert!"),
                config.getString("messages.no-economy", "<red>Keine Vault Economy gefunden."),
                config.getString("messages.player-only", "<red>Dieser Befehl kann nur von einem Spieler verwendet werden.")
        );
    }

    private void loadFakeNames() {
        File file = new File(getDataFolder(), "fake-names.yml");
        List<String> loaded = new ArrayList<>();

        if (!file.exists()) {
            getLogger().severe("[Gamble] fake-names.yml wurde nicht gefunden! Es kann keine Zahlung angezeigt werden.");
            this.fakeNames = Collections.emptyList();
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<String> namesFromFile = yaml.getStringList("names");

        for (String name : namesFromFile) {
            if (name != null && !name.isBlank()) {
                loaded.add(name);
            }
        }

        if (loaded.isEmpty()) {
            getLogger().severe("[Gamble] fake-names.yml enthaelt keine gueltigen Namen!");
        } else if (loaded.size() < 1000) {
            getLogger().warning("[Gamble] WARNING: fake-names.yml contains less than 1000 names!");
        }

        this.fakeNames = loaded;
    }

    private void saveResourceIfMissing(String resourcePath) {
        File file = new File(getDataFolder(), resourcePath);
        if (file.exists()) {
            return;
        }

        try (InputStream inputStream = getResource(resourcePath)) {
            if (inputStream == null) {
                getLogger().severe("[Gamble] Konnte " + resourcePath + " nicht aus dem Plugin-Jar laden.");
                return;
            }
            getDataFolder().mkdirs();
            Files.copy(inputStream, file.toPath());
        } catch (IOException exception) {
            getLogger().severe("[Gamble] Fehler beim Speichern von " + resourcePath + ": " + exception.getMessage());
        }
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public GambleManager getGambleManager() {
        return gambleManager;
    }

    public List<String> getFakeNames() {
        return fakeNames;
    }

    public long getIntervalTicks() {
        return intervalTicks;
    }

    public long getMinAmount() {
        return minAmount;
    }

    public long getMaxAmount() {
        return maxAmount;
    }

    public long getAmountStep() {
        return amountStep;
    }

    public int getAmountStepCount() {
        return amountStepCount;
    }

    public String getPaymentMessageTemplate() {
        return paymentMessageTemplate;
    }

    public Messages getMessages() {
        return messages;
    }

    /**
     * Small holder for the configurable status/error messages.
     */
    public record Messages(String enabled, String disabled, String noEconomy, String playerOnly) {
    }
}
