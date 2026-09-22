package de.gamble.plugin;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central place that tracks which players currently have Gamble active
 * and owns their scheduled {@link BukkitTask}. Guarantees that a player
 * never has more than one active Gamble task at the same time.
 */
public final class GambleManager {

    private final GamblePlugin plugin;
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();

    public GambleManager(GamblePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @return true if the given player currently has an active Gamble task
     */
    public boolean isGambling(Player player) {
        return activeTasks.containsKey(player.getUniqueId());
    }

    /**
     * Starts the Gamble payment loop for the given player, if not already running.
     */
    public void startGamble(Player player) {
        UUID uuid = player.getUniqueId();

        // Prevent duplicate tasks - if one is already scheduled, do nothing.
        if (activeTasks.containsKey(uuid)) {
            return;
        }

        long intervalTicks = plugin.getIntervalTicks();
        GambleTask task = new GambleTask(plugin, player);
        BukkitTask scheduled = task.runTaskTimer(plugin, intervalTicks, intervalTicks);
        activeTasks.put(uuid, scheduled);
    }

    /**
     * Stops the Gamble payment loop for the given player, if running.
     */
    public void stopGamble(Player player) {
        stopGamble(player.getUniqueId());
    }

    /**
     * Stops the Gamble payment loop for the given player UUID, if running.
     */
    public void stopGamble(UUID uuid) {
        BukkitTask task = activeTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Stops every currently running Gamble task. Used on plugin disable.
     */
    public void stopAll() {
        for (BukkitTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
    }
}
