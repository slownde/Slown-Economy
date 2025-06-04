package de.syscall.gui;

import de.syscall.SlownEconomy;
import de.syscall.data.EconomyPlayer;
import de.syscall.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
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
    private BukkitTask updateTask;
    private Gui gui;

    public CoinsGUI(SlownEconomy plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        this.gui = Gui.normal()
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
                .addIngredient('g', new TopPlayersItem())
                .addIngredient('h', new HistoryItem())
                .addIngredient('i', new StatsItem())
                .addIngredient('j', new PayItem())
                .addIngredient('k', new ExchangeItem())
                .addIngredient('l', new BankGUIItem())
                .addIngredient('m', new RefreshItem())
                .addIngredient('n', new SettingsItem())
                .addIngredient('o', new CloseItem())
                .build();

        Window window = Window.single()
                .setViewer(player)
                .setTitle(ColorUtil.colorize("§6§lCoins Management"))
                .setGui(gui)
                .build();

        window.open();
        startUpdateTask();
    }

    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.getOpenInventory().getTopInventory().getHolder() != gui) {
                    cancel();
                    return;
                }

                gui.notifyAll();
            }
        }.runTaskTimer(plugin, 20L, 20L);
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

    private static class TopPlayersItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.GOLDEN_HELMET)
                    .setDisplayName("§6Top Spieler")
                    .setLegacyLore(List.of(
                            "§7Rangliste der reichsten",
                            "§7Spieler im Netzwerk",
                            "",
                            "§7Feature kommt bald!"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            player.sendMessage(ColorUtil.component("§7Die Rangliste wird in einem zukünftigen Update verfügbar sein!"));
        }
    }

    private static class HistoryItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.WRITABLE_BOOK)
                    .setDisplayName("§9Transaktions-Historie")
                    .setLegacyLore(List.of(
                            "§7Verlauf deiner letzten",
                            "§7Coin-Transaktionen",
                            "",
                            "§7Feature kommt bald!"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            player.sendMessage(ColorUtil.component("§7Die Transaktions-Historie wird in einem zukünftigen Update verfügbar sein!"));
        }
    }

    private class StatsItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.PAPER)
                    .setDisplayName("§eStatistiken")
                    .setLegacyLore(List.of(
                            "§7Deine Economy-Statistiken",
                            "§7und Aktivitäten",
                            "",
                            "§7Cache-Größe: §6" + plugin.getCacheManager().getCacheSize(),
                            "",
                            "§eKlicken für Details"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());

            player.sendMessage(ColorUtil.component("§6═══ Economy Statistiken ═══"));
            if (economyPlayer != null) {
                player.sendMessage(ColorUtil.component("§7Coins: §6" + String.format("%.2f", economyPlayer.getCoins())));
                player.sendMessage(ColorUtil.component("§7Bank: §6" + String.format("%.2f", economyPlayer.getBankBalance())));
                player.sendMessage(ColorUtil.component("§7Gesamt: §6" + String.format("%.2f", economyPlayer.getTotalWealth())));
                player.sendMessage(ColorUtil.component("§7Letzter Login: §6" + new java.util.Date(economyPlayer.getLastSeen())));
            }
        }
    }

    private static class PayItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.EMERALD)
                    .setDisplayName("§aBezahlen")
                    .setLegacyLore(List.of(
                            "§7Schnelles Bezahlen",
                            "§7für Shops und Services",
                            "",
                            "§7Feature kommt bald!"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            player.sendMessage(ColorUtil.component("§7Das Bezahlsystem wird in einem zukünftigen Update verfügbar sein!"));
        }
    }

    private static class ExchangeItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.GOLD_BLOCK)
                    .setDisplayName("§6Börse")
                    .setLegacyLore(List.of(
                            "§7Coins gegen Items",
                            "§7oder andere Währungen",
                            "§7tauschen",
                            "",
                            "§7Feature kommt bald!"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            player.sendMessage(ColorUtil.component("§7Die Börse wird in einem zukünftigen Update verfügbar sein!"));
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

    private class RefreshItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.COMPASS)
                    .setDisplayName("§bAktualisieren")
                    .setLegacyLore(List.of(
                            "§7Lade die neuesten",
                            "§7Daten vom Server",
                            "",
                            "§eKlicken zum Aktualisieren"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            plugin.getCacheManager().loadPlayer(player.getUniqueId(), player.getName()).thenRun(() -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    gui.notifyAll();
                    player.sendMessage(ColorUtil.component("§aDaten aktualisiert!"));
                });
            });
        }
    }

    private static class SettingsItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.REDSTONE)
                    .setDisplayName("§cEinstellungen")
                    .setLegacyLore(List.of(
                            "§7Economy-Einstellungen",
                            "§7und Präferenzen",
                            "",
                            "§7Feature kommt bald!"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            player.sendMessage(ColorUtil.component("§7Einstellungen werden in einem zukünftigen Update verfügbar sein!"));
        }
    }

    private class CloseItem extends AbstractItem {
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
            if (updateTask != null) {
                updateTask.cancel();
            }
        }
    }
}