package de.syscall.command;

import de.syscall.SlownEconomy;
import de.syscall.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TransferCommand implements CommandExecutor, TabCompleter {

    private final SlownEconomy plugin;

    public TransferCommand(SlownEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.component("§cDieser Command kann nur von Spielern ausgeführt werden!"));
            return true;
        }

        if (!plugin.getEconomyValidator().isTransferEnabled()) {
            player.sendMessage(ColorUtil.component("§cTransfers sind derzeit deaktiviert!"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ColorUtil.component("§cVerwendung: /transfer <spieler> <amount>"));
            player.sendMessage(ColorUtil.component("§7Minimum: §6" +
                    String.format("%.2f", plugin.getEconomyValidator().getMinTransferAmount())));
            player.sendMessage(ColorUtil.component("§7Maximum: §6" +
                    String.format("%.2f", plugin.getEconomyValidator().getMaxTransferAmount())));

            double feePercentage = plugin.getEconomyValidator().getTransferFeePercentage();
            if (feePercentage > 0) {
                player.sendMessage(ColorUtil.component("§7Gebühr: §6" + String.format("%.1f", feePercentage) + "%"));
            }
            return true;
        }

        String targetName = args[0];

        try {
            double amount = Double.parseDouble(args[1]);
            handleTransfer(player, targetName, amount);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.component("§cUngültige Zahl!"));
        }

        return true;
    }

    private void handleTransfer(Player sender, String targetName, double amount) {
        if (plugin.getEconomyValidator().isTransferAmountInvalid(amount)) {
            sender.sendMessage(ColorUtil.component("§cUngültiger Transfer-Betrag!"));
            sender.sendMessage(ColorUtil.component("§7Minimum: §6" +
                    String.format("%.2f", plugin.getEconomyValidator().getMinTransferAmount())));
            sender.sendMessage(ColorUtil.component("§7Maximum: §6" +
                    String.format("%.2f", plugin.getEconomyValidator().getMaxTransferAmount())));
            return;
        }

        if (sender.getName().equalsIgnoreCase(targetName)) {
            sender.sendMessage(ColorUtil.component("§cDu kannst dir nicht selbst Coins senden!"));
            return;
        }

        double fee = plugin.getEconomyValidator().getTransferFee(amount);
        double totalCost = plugin.getEconomyValidator().getTotalTransferCost(amount);

        double senderCoins = SlownEconomy.getAPI().getCoins(sender);
        if (senderCoins < totalCost) {
            sender.sendMessage(ColorUtil.component("§cNicht genug Coins!"));
            sender.sendMessage(ColorUtil.component("§7Benötigt: §6" + String.format("%.2f", totalCost) + " Coins"));
            sender.sendMessage(ColorUtil.component("§7Verfügbar: §6" + String.format("%.2f", senderCoins) + " Coins"));
            if (fee > 0) {
                sender.sendMessage(ColorUtil.component("§7(§6" + String.format("%.2f", amount) + " §7+ §6" +
                        String.format("%.2f", fee) + " §7Gebühr)"));
            }
            return;
        }

        plugin.getCacheManager().getPlayerByName(targetName).thenAccept(target -> {
            if (target == null) {
                sender.sendMessage(ColorUtil.component("§cSpieler nicht gefunden!"));
                return;
            }

            plugin.getEconomyManager().transferCoins(sender.getUniqueId(), target.getUuid(), amount).thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        sender.sendMessage(ColorUtil.component("§6" + String.format("%.2f", amount) +
                                " Coins §7wurden an §6" + target.getName() + " §7übertragen!"));

                        if (fee > 0) {
                            sender.sendMessage(ColorUtil.component("§7Gebühr: §6" + String.format("%.2f", fee) + " Coins"));
                        }

                        Player targetPlayer = plugin.getServer().getPlayer(target.getUuid());
                        if (targetPlayer != null) {
                            targetPlayer.sendMessage(ColorUtil.component("§7Du hast §6" + String.format("%.2f", amount) +
                                    " Coins §7von §6" + sender.getName() + " §7erhalten!"));
                        }
                    } else {
                        sender.sendMessage(ColorUtil.component("§cFehler beim Transfer! Möglicherweise Maximum erreicht."));
                    }
                });
            });
        });
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!player.equals(sender) && player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            completions.addAll(Arrays.asList("10", "50", "100", "500", "1000"));
        }

        return completions;
    }
}