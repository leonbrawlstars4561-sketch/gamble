package de.gamble.plugin;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

/**
 * Runs once every configured interval for exactly one player while Gamble
 * is active for them. Picks a random fake display name and a random
 * step-aligned amount, deposits the real amount into the player's Vault
 * economy balance and sends the payment message.
 */
public final class GambleTask extends BukkitRunnable {

    private static final Random RANDOM = new Random();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final GamblePlugin plugin;
    private final Player player;

    public GambleTask(GamblePlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public void run() {
        // Player went offline between scheduling and execution - stop safely.
        if (!player.isOnline()) {
            plugin.getGambleManager().stopGamble(player);
            return;
        }

        VaultHook vaultHook = plugin.getVaultHook();
        if (vaultHook == null || !vaultHook.isAvailable()) {
            player.sendMessage(MINI_MESSAGE.deserialize(plugin.getMessages().noEconomy()));
            plugin.getGambleManager().stopGamble(player);
            return;
        }

        Economy economy = vaultHook.getEconomy();

        long amount = randomAmount();
        String fakeName = randomFakeName();

        EconomyResponse response = economy.depositPlayer(player, amount);
        if (response == null || !response.transactionSuccess()) {
            plugin.getLogger().warning("Vault Einzahlung fuer " + player.getName() + " ist fehlgeschlagen.");
            return;
        }

        String formattedAmount = MoneyFormatter.format(amount);
        String message = plugin.getPaymentMessageTemplate()
                .replace("{name}", fakeName)
                .replace("{amount}", formattedAmount);

        player.sendMessage(MINI_MESSAGE.deserialize(message));
    }

    private long randomAmount() {
        long min = plugin.getMinAmount();
        long step = plugin.getAmountStep();
        int steps = plugin.getAmountStepCount();
        int index = RANDOM.nextInt(steps);
        return min + ((long) index * step);
    }

    private String randomFakeName() {
        List<String> names = plugin.getFakeNames();
        int index = RANDOM.nextInt(names.size());
        return names.get(index);
    }
}
