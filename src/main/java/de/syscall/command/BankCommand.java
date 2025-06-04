package de.syscall.command;

import de.syscall.SlownEconomy;
import de.syscall.gui.BankGUI;
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

public class BankCommand implements CommandExecutor, TabCompleter {

    private final SlownEconomy plugin;
    private final List<String> subCommands = Arrays.asList("deposit", "withdraw", "balance");
    private final List<String> adminSubcommands = Arrays.asList("set", "add", "remove");

    public BankCommand(SlownEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.component("§cDieser Command kann nur von Spielern ausgeführt werden!"));
            return true;
        }

        if (args.length == 0) {
            new BankGUI(plugin, player).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "balance", "bal" -> handleBalance(player);
            case "deposit", "dep" -> {
                if (args.length < 2) {
                    player.sendMessage(ColorUtil.component("§cVerwendung: /bank deposit <amount>"));
                    return true;
                }
                handleDeposit(player, args[1]);
            }
            case "withdraw", "wd" -> {
                if (args.length < 2) {
                    player.sendMessage(ColorUtil.component("§cVerwendung: /bank withdraw <amount>"));
                    return true;
                }
                handleWithdraw(player, args[1]);
            }
            case "set" -> {
                if (!player.hasPermission("slowneconomy.admin")) {
                    player.sendMessage(ColorUtil.component("§cDu hast keine Berechtigung für diesen Command!"));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ColorUtil.component("§cVerwendung: /bank set <spieler> <amount>"));
                    return true;
                }
                handleSetBank(player, args[1], args[2]);
            }
            case "add" -> {
                if (!player.hasPermission("slowneconomy.admin")) {
                    player.sendMessage(ColorUtil.component("§cDu hast keine Berechtigung für diesen Command!"));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ColorUtil.component("§cVerwendung: /bank add <spieler> <amount>"));
                    return true;
                }
                handleAddBank(player, args[1], args[2]);
            }
            case "remove" -> {
                if (!player.hasPermission("slowneconomy.admin")) {
                    player.sendMessage(ColorUtil.component("§cDu hast keine Berechtigung für diesen Command!"));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ColorUtil.component("§cVerwendung: /bank remove <spieler> <amount>"));
                    return true;
                }
                handleRemoveBank(player, args[1], args[2]);
            }
            default -> player.sendMessage(ColorUtil.component("§cUnbekannter Subcommand! Verwende /bank für das GUI."));
        }

        return true;
    }

    private void handleBalance(Player player) {
        plugin.getEconomyManager().getBankBalance(player.getUniqueId()).thenAccept(balance -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ColorUtil.component("§7Dein Bankguthaben: §6" +
                        String.format("%.2f", balance) + " Coins"));
            });
        });
    }

    private void handleDeposit(Player player, String amountStr) {
        try {
            double amount = parseAmount(amountStr, player);

            if (amount <= 0) {
                player.sendMessage(ColorUtil.component("§cBetrag muss positiv sein!"));
                return;
            }

            plugin.getEconomyManager().depositToBank(player.getUniqueId(), amount).thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        player.sendMessage(ColorUtil.component("§6" + String.format("%.2f", amount) +
                                " Coins §7wurden auf dein Bankkonto eingezahlt."));
                    } else {
                        player.sendMessage(ColorUtil.component("§cNicht genug Coins, ungültige Menge oder Banklimit erreicht!"));
                    }
                });
            });

        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.component("§cUngültige Zahl!"));
        }
    }

    private void handleWithdraw(Player player, String amountStr) {
        try {
            double amount = parseAmount(amountStr, player);

            plugin.getEconomyManager().withdrawFromBank(player.getUniqueId(), amount).thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        player.sendMessage(ColorUtil.component("§6" + String.format("%.2f", amount) +
                                " Coins §7wurden von deinem Bankkonto abgehoben."));
                    } else {
                        player.sendMessage(ColorUtil.component("§cNicht genug Bankguthaben oder ungültige Menge!"));
                    }
                });
            });

        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.component("§cUngültige Zahl!"));
        }
    }

    private void handleSetBank(Player admin, String targetName, String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);

            if (plugin.getEconomyValidator().isBankAmountTooHigh(amount)) {
                admin.sendMessage(ColorUtil.component("§cUngültiger Betrag! Maximum: " +
                        String.format("%.2f", plugin.getEconomyValidator().getMaxBankBalance())));
                return;
            }

            plugin.getCacheManager().getPlayerByName(targetName).thenAccept(target -> {
                if (target == null) {
                    admin.sendMessage(ColorUtil.component("§cSpieler nicht gefunden!"));
                    return;
                }

                plugin.getEconomyManager().setBankBalance(target.getUuid(), amount).thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            admin.sendMessage(ColorUtil.component("§7Bankguthaben von §6" + target.getName() +
                                    " §7auf §6" + String.format("%.2f", amount) + " §7gesetzt."));
                        } else {
                            admin.sendMessage(ColorUtil.component("§cFehler beim Setzen des Bankguthabens!"));
                        }
                    });
                });
            });

        } catch (NumberFormatException e) {
            admin.sendMessage(ColorUtil.component("§cUngültige Zahl!"));
        }
    }

    private void handleAddBank(Player admin, String targetName, String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);

            if (amount <= 0) {
                admin.sendMessage(ColorUtil.component("§cBetrag muss positiv sein!"));
                return;
            }

            plugin.getCacheManager().getPlayerByName(targetName).thenAccept(target -> {
                if (target == null) {
                    admin.sendMessage(ColorUtil.component("§cSpieler nicht gefunden!"));
                    return;
                }

                plugin.getEconomyManager().addBankBalance(target.getUuid(), amount).thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            admin.sendMessage(ColorUtil.component("§6" + String.format("%.2f", amount) +
                                    " Coins §7wurden dem Bankkonto von §6" + target.getName() + " §7hinzugefügt."));
                        } else {
                            admin.sendMessage(ColorUtil.component("§cFehler beim Hinzufügen zum Bankguthaben! Möglicherweise Maximum erreicht."));
                        }
                    });
                });
            });

        } catch (NumberFormatException e) {
            admin.sendMessage(ColorUtil.component("§cUngültige Zahl!"));
        }
    }

    private void handleRemoveBank(Player admin, String targetName, String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);

            plugin.getCacheManager().getPlayerByName(targetName).thenAccept(target -> {
                if (target == null) {
                    admin.sendMessage(ColorUtil.component("§cSpieler nicht gefunden!"));
                    return;
                }

                plugin.getEconomyManager().removeBankBalance(target.getUuid(), amount).thenAccept(success -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (success) {
                            admin.sendMessage(ColorUtil.component("§6" + String.format("%.2f", amount) +
                                    " Coins §7wurden vom Bankkonto von §6" + target.getName() + " §7entfernt."));
                        } else {
                            admin.sendMessage(ColorUtil.component("§cNicht genug Bankguthaben oder Fehler!"));
                        }
                    });
                });
            });

        } catch (NumberFormatException e) {
            admin.sendMessage(ColorUtil.component("§cUngültige Zahl!"));
        }
    }

    private double parseAmount(String amountStr, Player player) throws NumberFormatException {
        if (amountStr.equalsIgnoreCase("all")) {
            return SlownEconomy.getAPI().getCoins(player);
        }

        double amount = Double.parseDouble(amountStr);
        if (amount <= 0) {
            throw new NumberFormatException("Amount must be positive");
        }

        return amount;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }

            if (sender.hasPermission("slowneconomy.admin")) {
                for (String adminCommand : adminSubcommands) {
                    if (adminCommand.startsWith(input)) {
                        completions.add(adminCommand);
                    }
                }
            }

        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("deposit") || subCommand.equals("withdraw")) {
                completions.addAll(Arrays.asList("10", "50", "100", "500", "1000", "all"));

            } else if (adminSubcommands.contains(subCommand) && sender.hasPermission("slowneconomy.admin")) {
                String input = args[1].toLowerCase();
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(input)) {
                        completions.add(player.getName());
                    }
                }
            }

        } else if (args.length == 3 && adminSubcommands.contains(args[0].toLowerCase()) && sender.hasPermission("slowneconomy.admin")) {
            completions.addAll(Arrays.asList("10", "50", "100", "500", "1000"));
        }

        return completions;
    }
}