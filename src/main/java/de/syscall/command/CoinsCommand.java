package de.syscall.command;

import de.syscall.SlownEconomy;
import de.syscall.gui.CoinsGUI;
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

public class CoinsCommand implements CommandExecutor, TabCompleter {

    private final SlownEconomy plugin;
    private final List<String> adminSubcommands = Arrays.asList("set", "add", "remove", "give", "take");

    public CoinsCommand(SlownEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.component("§cDieser Command kann nur von Spielern ausgeführt werden!"));
            return true;
        }

        if (args.length == 0) {
            new CoinsGUI(plugin, player).open();
            return true;
        }

        if (args.length == 1) {
            String targetName = args[0];

            if (adminSubcommands.contains(targetName.toLowerCase()) && !player.hasPermission("slowneconomy.admin")) {
                player.sendMessage(ColorUtil.component("§cDu hast keine Berechtigung für diesen Command!"));
                return true;
            }

            handlePlayerCoinsCheck(player, targetName);
            return true;
        }

        handleAdminCommands(player, args);
        return true;
    }

    private void handlePlayerCoinsCheck(Player player, String targetName) {
        plugin.getCacheManager().getPlayerByName(targetName).thenAccept(target -> {
            if (target == null) {
                player.sendMessage(ColorUtil.component("§cSpieler nicht gefunden!"));
                return;
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ColorUtil.component("§6" + target.getName() + " §7hat §6" +
                        String.format("%.2f", target.getCoins()) + " Coins§7."));
            });
        });
    }

    private void handleAdminCommands(Player player, String[] args) {
        if (!player.hasPermission("slowneconomy.admin")) {
            player.sendMessage(ColorUtil.component("§cDu hast keine Berechtigung für diesen Command!"));
            return;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];

        switch (action) {
            case "set" -> {
                if (args.length < 3) {
                    player.sendMessage(ColorUtil.component("§cVerwendung: /coins set <spieler> <amount>"));
                    return;
                }

                try {
                    double amount = Double.parseDouble(args[2]);
                    handleSetCoins(player, targetName, amount);
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.component("§cUngültige Zahl!"));
                }
            }

            case "add", "give" -> {
                if (args.length < 3) {
                    player.sendMessage(ColorUtil.component("§cVerwendung: /coins " + action + " <spieler> <amount>"));
                    return;
                }

                try {
                    double amount = Double.parseDouble(args[2]);
                    handleAddCoins(player, targetName, amount);
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.component("§cUngültige Zahl!"));
                }
            }

            case "remove", "take" -> {
                if (args.length < 3) {
                    player.sendMessage(ColorUtil.component("§cVerwendung: /coins " + action + " <spieler> <amount>"));
                    return;
                }

                try {
                    double amount = Double.parseDouble(args[2]);
                    handleRemoveCoins(player, targetName, amount);
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.component("§cUngültige Zahl!"));
                }
            }

            default -> player.sendMessage(ColorUtil.component("§cUnbekannter Subcommand!"));
        }
    }

    private void handleSetCoins(Player admin, String targetName, double amount) {
        if (plugin.getEconomyValidator().isCoinsAmountTooHigh(amount)) {
            admin.sendMessage(ColorUtil.component("§cUngültiger Betrag! Maximum: " +
                    String.format("%.2f", plugin.getEconomyValidator().getMaxCoins())));
            return;
        }

        plugin.getCacheManager().getPlayerByName(targetName).thenAccept(target -> {
            if (target == null) {
                admin.sendMessage(ColorUtil.component("§cSpieler nicht gefunden!"));
                return;
            }

            plugin.getEconomyManager().setCoins(target.getUuid(), amount).thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        admin.sendMessage(ColorUtil.component("§7Coins von §6" + target.getName() +
                                " §7auf §6" + String.format("%.2f", amount) + " §7gesetzt."));
                    } else {
                        admin.sendMessage(ColorUtil.component("§cFehler beim Setzen der Coins!"));
                    }
                });
            });
        });
    }

    private void handleAddCoins(Player admin, String targetName, double amount) {
        if (plugin.getEconomyValidator().isCoinsAmountTooHigh(amount)) {
            admin.sendMessage(ColorUtil.component("§cUngültiger Betrag! Maximum: " +
                    String.format("%.2f", plugin.getEconomyValidator().getMaxCoins())));
            return;
        }

        plugin.getCacheManager().getPlayerByName(targetName).thenAccept(target -> {
            if (target == null) {
                admin.sendMessage(ColorUtil.component("§cSpieler nicht gefunden!"));
                return;
            }

            plugin.getEconomyManager().addCoins(target.getUuid(), amount).thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        admin.sendMessage(ColorUtil.component("§6" + String.format("%.2f", amount) +
                                " Coins §7wurden §6" + target.getName() + " §7hinzugefügt."));
                    } else {
                        admin.sendMessage(ColorUtil.component("§cFehler beim Hinzufügen der Coins! Möglicherweise Maximum erreicht."));
                    }
                });
            });
        });
    }

    private void handleRemoveCoins(Player admin, String targetName, double amount) {
        plugin.getCacheManager().getPlayerByName(targetName).thenAccept(target -> {
            if (target == null) {
                admin.sendMessage(ColorUtil.component("§cSpieler nicht gefunden!"));
                return;
            }

            plugin.getEconomyManager().removeCoins(target.getUuid(), amount).thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        admin.sendMessage(ColorUtil.component("§6" + String.format("%.2f", amount) +
                                " Coins §7wurden von §6" + target.getName() + " §7entfernt."));
                    } else {
                        admin.sendMessage(ColorUtil.component("§cNicht genug Coins oder Fehler!"));
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

            if (sender.hasPermission("slowneconomy.admin")) {
                for (String subcommand : adminSubcommands) {
                    if (subcommand.startsWith(input)) {
                        completions.add(subcommand);
                    }
                }
            }

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }

        } else if (args.length == 2 && adminSubcommands.contains(args[0].toLowerCase()) && sender.hasPermission("slowneconomy.admin")) {
            String input = args[1].toLowerCase();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }

        } else if (args.length == 3 && adminSubcommands.contains(args[0].toLowerCase()) && sender.hasPermission("slowneconomy.admin")) {
            completions.addAll(Arrays.asList("10", "50", "100", "500", "1000"));
        }

        return completions;
    }
}