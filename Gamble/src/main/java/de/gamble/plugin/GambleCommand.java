package de.gamble.plugin;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Handles the /gamble command. It takes no arguments and simply toggles
 * the Gamble payment loop on or off for the executing player.
 */
public final class GambleCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final GamblePlugin plugin;

    public GambleCommand(GamblePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MINI_MESSAGE.deserialize(plugin.getMessages().playerOnly()));
            return true;
        }

        if (!player.hasPermission("gamble.use")) {
            player.sendMessage(MINI_MESSAGE.deserialize("<red>Du hast keine Berechtigung dafuer."));
            return true;
        }

        if (!plugin.getVaultHook().isAvailable()) {
            player.sendMessage(MINI_MESSAGE.deserialize(plugin.getMessages().noEconomy()));
            return true;
        }

        GambleManager manager = plugin.getGambleManager();

        if (manager.isGambling(player)) {
            manager.stopGamble(player);
            player.sendMessage(MINI_MESSAGE.deserialize(plugin.getMessages().disabled()));
        } else {
            manager.startGamble(player);
            player.sendMessage(MINI_MESSAGE.deserialize(plugin.getMessages().enabled()));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // /gamble takes no arguments, so there is nothing to suggest.
        return Collections.emptyList();
    }
}
