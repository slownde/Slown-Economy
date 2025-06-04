package de.syscall.gui;

import de.syscall.SlownEconomy;
import de.syscall.data.EconomyPlayer;
import de.syscall.util.ColorUtil;
import de.syscall.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;

public class CoinsGUI {

    private final SlownEconomy plugin;
    private final Player player;

    public CoinsGUI(SlownEconomy plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        Gui gui = Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# a b c d e f g #",
                        "# h i j k l m n #",
                        "# # # # o # # # #"
                )
                .addIngredient('#', new BackgroundItem())
                .addIngredient('a', new CoinsInfoItem())
                .addIngredient('b', new BankInfoItem())
                .addIngredient('c', new TotalWealthItem())
                .addIngredient('d', new TransferItem())
                .addIngredient('e', new DepositItem())
                .addIngredient('f', new WithdrawItem())
                .addIngredient('l', new BankGUIItem())
                .addIngredient('o', new CloseItem())
                .build();

        Window window = Window.single()
                .setViewer(player)
                .setTitle(ColorUtil.colorize("§6§lCoins Management"))
                .setGui(gui)
                .build();

        window.open();
    }

    private static class BackgroundItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                    .setDisplayName("§r");
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class CoinsInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());
            double coins = economyPlayer != null ? economyPlayer.getCoins() : 0.0;

            return new ItemBuilder(Material.GOLD_INGOT)
                    .setDisplayName("§6Deine Coins")
                    .setLegacyLore(List.of(
                            "§7Aktueller Betrag:",
                            "§6" + String.format("%.2f", coins) + " Coins",
                            "",
                            "§7Coins kannst du für",
                            "§7Käufe und Transfers",
                            "§7verwenden",
                            "",
                            "§eKlicken für Details"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());
            double coins = economyPlayer != null ? economyPlayer.getCoins() : 0.0;

            player.sendMessage(ColorUtil.component("§6═══ Coins Information ═══"));
            player.sendMessage(ColorUtil.component("§7Aktuelle Coins: §6" + String.format("%.2f", coins)));
            player.sendMessage(ColorUtil.component("§7Verwende §6/coins <spieler> §7um andere Coins anzuzeigen"));
        }
    }

    private class BankInfoItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());
            double bankBalance = economyPlayer != null ? economyPlayer.getBankBalance() : 0.0;

            return new ItemBuilder(Material.CHEST)
                    .setDisplayName("§2Bankguthaben")
                    .setLegacyLore(List.of(
                            "§7Aktueller Betrag:",
                            "§6" + String.format("%.2f", bankBalance) + " Coins",
                            "",
                            "§7Deine Coins sind in der",
                            "§7Bank sicher verwahrt",
                            "",
                            "§eKlicken für Bank-GUI"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new BankGUI(plugin, player).open();
        }
    }

    private class TotalWealthItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());
            double totalWealth = economyPlayer != null ? economyPlayer.getTotalWealth() : 0.0;

            return new ItemBuilder(Material.DIAMOND)
                    .setDisplayName("§bGesamtvermögen")
                    .setLegacyLore(List.of(
                            "§7Coins + Bankguthaben:",
                            "§6" + String.format("%.2f", totalWealth) + " Coins",
                            "",
                            "§7Dein komplettes",
                            "§7Vermögen im System"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class TransferItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            boolean transferEnabled = plugin.getEconomyValidator().isTransferEnabled();
            double minAmount = plugin.getEconomyValidator().getMinTransferAmount();
            double maxAmount = plugin.getEconomyValidator().getMaxTransferAmount();
            double feePercentage = plugin.getEconomyValidator().getTransferFeePercentage();

            List<String> lore = new ArrayList<>();
            lore.add("§7Sende Coins an");
            lore.add("§7andere Spieler");
            lore.add("");

            if (transferEnabled) {
                lore.add("§7Minimum: §6" + String.format("%.2f", minAmount));
                lore.add("§7Maximum: §6" + String.format("%.2f", maxAmount));
                if (feePercentage > 0) {
                    lore.add("§7Gebühr: §6" + String.format("%.1f", feePercentage) + "%");
                }
                lore.add("");
                lore.add("§7Verwende:");
                lore.add("§6/transfer <spieler> <amount>");
                lore.add("");
                lore.add("§eKlicken für Anleitung");
            } else {
                lore.add("§cTransfers sind deaktiviert");
            }

            return new ItemBuilder(transferEnabled ? Material.PAPER : Material.BARRIER)
                    .setDisplayName("§eCoins übertragen")
                    .setLegacyLore(lore);
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            player.closeInventory();

            if (!plugin.getEconomyValidator().isTransferEnabled()) {
                player.sendMessage(ColorUtil.component("§cTransfers sind derzeit deaktiviert!"));
                return;
            }

            player.sendMessage(ColorUtil.component("§6═══ Coins Transfer ═══"));
            player.sendMessage(ColorUtil.component("§7Verwende §6/transfer <spieler> <betrag> §7um Coins zu senden"));
            player.sendMessage(ColorUtil.component("§7Beispiel: §6/transfer Steve 100"));
            player.sendMessage(ColorUtil.component("§7Minimum: §6" +
                    String.format("%.2f", plugin.getEconomyValidator().getMinTransferAmount())));
            player.sendMessage(ColorUtil.component("§7Maximum: §6" +
                    String.format("%.2f", plugin.getEconomyValidator().getMaxTransferAmount())));

            double feePercentage = plugin.getEconomyValidator().getTransferFeePercentage();
            if (feePercentage > 0) {
                player.sendMessage(ColorUtil.component("§7Gebühr: §6" + String.format("%.1f", feePercentage) + "%"));
            }
        }
    }

    private class DepositItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.HOPPER)
                    .setDisplayName("§aEinzahlen")
                    .setLegacyLore(List.of(
                            "§7Zahle Coins auf",
                            "§7dein Bankkonto ein",
                            "",
                            "§7Schnellzugriff:",
                            "§6/bank deposit <amount>",
                            "",
                            "§eKlicken für Bank-GUI"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new BankGUI(plugin, player).open();
        }
    }

    private class WithdrawItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.DROPPER)
                    .setDisplayName("§cAbheben")
                    .setLegacyLore(List.of(
                            "§7Hebe Coins von",
                            "§7deinem Bankkonto ab",
                            "",
                            "§7Schnellzugriff:",
                            "§6/bank withdraw <amount>",
                            "",
                            "§eKlicken für Bank-GUI"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new BankGUI(plugin, player).open();
        }
    }

    private class BankGUIItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.ENDER_CHEST)
                    .setDisplayName("§2Bank verwalten")
                    .setLegacyLore(List.of(
                            "§7Öffne das Bank-GUI",
                            "§7für alle Bankfunktionen",
                            "",
                            "§eKlicken zum Öffnen"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new BankGUI(plugin, player).open();
        }
    }

    private static class CloseItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.BARRIER)
                    .setDisplayName("§cSchließen")
                    .setLegacyLore(List.of(
                            "§7Schließe das GUI",
                            "",
                            "§eKlicken zum Schließen"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            player.closeInventory();
        }
    }
}