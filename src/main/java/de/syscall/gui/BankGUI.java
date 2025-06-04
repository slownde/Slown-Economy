package de.syscall.gui;

import de.syscall.SlownEconomy;
import de.syscall.data.EconomyPlayer;
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

import java.util.List;

public class BankGUI {

    private final SlownEconomy plugin;
    private final Player player;
    private Window window;
    private Gui gui;

    public BankGUI(SlownEconomy plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        plugin.getGUIManager().registerBankGUI(player.getUniqueId(), this);

        bankBalanceItem = new BankBalanceItem();
        coinsItem = new CoinsItem();
        depositAllItem = new DepositAllItem();
        deposit100Item = new Deposit100Item();
        deposit1000Item = new Deposit1000Item();
        withdrawAllItem = new WithdrawAllItem();
        withdraw100Item = new Withdraw100Item();
        withdraw1000Item = new Withdraw1000Item();

        gui = Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# a b c d e f g #",
                        "# h i j k l m n #",
                        "# # # # o # # # #"
                )
                .addIngredient('#', new BackgroundItem())
                .addIngredient('a', bankBalanceItem)
                .addIngredient('b', coinsItem)
                .addIngredient('c', depositAllItem)
                .addIngredient('d', deposit100Item)
                .addIngredient('e', deposit1000Item)
                .addIngredient('f', withdrawAllItem)
                .addIngredient('g', withdraw100Item)
                .addIngredient('h', withdraw1000Item)
                .addIngredient('l', new CoinsGUIItem())
                .addIngredient('n', new HelpItem())
                .addIngredient('o', new CloseItem())
                .build();

        window = Window.single()
                .setViewer(player)
                .setTitle(ColorUtil.colorize("§2§lBank Management"))
                .setGui(gui)
                .build();

        window.open();
    }

    private BankBalanceItem bankBalanceItem;
    private CoinsItem coinsItem;
    private DepositAllItem depositAllItem;
    private Deposit100Item deposit100Item;
    private Deposit1000Item deposit1000Item;
    private WithdrawAllItem withdrawAllItem;
    private Withdraw100Item withdraw100Item;
    private Withdraw1000Item withdraw1000Item;

    public void updateGUI() {
        if (bankBalanceItem != null) bankBalanceItem.notifyWindows();
        if (coinsItem != null) coinsItem.notifyWindows();
        if (depositAllItem != null) depositAllItem.notifyWindows();
        if (deposit100Item != null) deposit100Item.notifyWindows();
        if (deposit1000Item != null) deposit1000Item.notifyWindows();
        if (withdrawAllItem != null) withdrawAllItem.notifyWindows();
        if (withdraw100Item != null) withdraw100Item.notifyWindows();
        if (withdraw1000Item != null) withdraw1000Item.notifyWindows();
    }

    private static class BackgroundItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE)
                    .setDisplayName("§r");
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class BankBalanceItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());
            double bankBalance = economyPlayer != null ? economyPlayer.getBankBalance() : 0.0;

            return new ItemBuilder(Material.ENDER_CHEST)
                    .setDisplayName("§2Bankguthaben")
                    .setLegacyLore(List.of(
                            "§7Aktueller Betrag:",
                            "§6" + String.format("%.2f", bankBalance) + " Coins",
                            "",
                            "§7Dein Geld ist hier",
                            "§7sicher verwahrt"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class CoinsItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());
            double coins = economyPlayer != null ? economyPlayer.getCoins() : 0.0;

            return new ItemBuilder(Material.GOLD_INGOT)
                    .setDisplayName("§6Verfügbare Coins")
                    .setLegacyLore(List.of(
                            "§7Aktueller Betrag:",
                            "§6" + String.format("%.2f", coins) + " Coins",
                            "",
                            "§7Diese Coins kannst du",
                            "§7einzahlen oder ausgeben"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        }
    }

    private class DepositAllItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());
            double coins = economyPlayer != null ? economyPlayer.getCoins() : 0.0;

            return new ItemBuilder(Material.HOPPER)
                    .setDisplayName("§aAlle Coins einzahlen")
                    .setLegacyLore(List.of(
                            "§7Zahle alle deine Coins",
                            "§7auf die Bank ein",
                            "",
                            "§7Betrag: §6" + String.format("%.2f", coins) + " Coins",
                            "",
                            coins > 0 ? "§eKlicken zum Einzahlen" : "§cKeine Coins verfügbar"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());
            if (economyPlayer == null || economyPlayer.getCoins() <= 0) {
                player.sendMessage(ColorUtil.component("§cDu hast keine Coins zum Einzahlen!"));
                return;
            }

            double amount = economyPlayer.getCoins();
            plugin.getEconomyManager().depositToBank(player.getUniqueId(), amount).thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        player.sendMessage(ColorUtil.component("§6" + String.format("%.2f", amount) +
                                " Coins §7wurden eingezahlt!"));
                        updateGUI();
                    } else {
                        player.sendMessage(ColorUtil.component("§cFehler beim Einzahlen!"));
                    }
                });
            });
        }
    }

    private class Deposit100Item extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());
            double coins = economyPlayer != null ? economyPlayer.getCoins() : 0.0;
            boolean canDeposit = coins >= 100;

            return new ItemBuilder(canDeposit ? Material.LIME_DYE : Material.GRAY_DYE)
                    .setDisplayName("§a100 Coins einzahlen")
                    .setLegacyLore(List.of(
                            "§7Zahle 100 Coins",
                            "§7auf die Bank ein",
                            "",
                            "§7Verfügbar: §6" + String.format("%.2f", coins) + " Coins",
                            "",
                            canDeposit ? "§eKlicken zum Einzahlen" : "§cNicht genug Coins"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            plugin.getEconomyManager().depositToBank(player.getUniqueId(), 100.0).thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        player.sendMessage(ColorUtil.component("§6100 Coins §7wurden eingezahlt!"));
                        updateGUI();
                    } else {
                        player.sendMessage(ColorUtil.component("§cNicht genug Coins!"));
                    }
                });
            });
        }
    }

    private class Deposit1000Item extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());
            double coins = economyPlayer != null ? economyPlayer.getCoins() : 0.0;
            boolean canDeposit = coins >= 1000;

            return new ItemBuilder(canDeposit ? Material.LIME_DYE : Material.GRAY_DYE)
                    .setDisplayName("§a1000 Coins einzahlen")
                    .setLegacyLore(List.of(
                            "§7Zahle 1000 Coins",
                            "§7auf die Bank ein",
                            "",
                            "§7Verfügbar: §6" + String.format("%.2f", coins) + " Coins",
                            "",
                            canDeposit ? "§eKlicken zum Einzahlen" : "§cNicht genug Coins"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            plugin.getEconomyManager().depositToBank(player.getUniqueId(), 1000.0).thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        player.sendMessage(ColorUtil.component("§61000 Coins §7wurden eingezahlt!"));
                        updateGUI();
                    } else {
                        player.sendMessage(ColorUtil.component("§cNicht genug Coins!"));
                    }
                });
            });
        }
    }

    private class WithdrawAllItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());
            double bankBalance = economyPlayer != null ? economyPlayer.getBankBalance() : 0.0;

            return new ItemBuilder(Material.DROPPER)
                    .setDisplayName("§cAlles abheben")
                    .setLegacyLore(List.of(
                            "§7Hebe dein gesamtes",
                            "§7Bankguthaben ab",
                            "",
                            "§7Betrag: §6" + String.format("%.2f", bankBalance) + " Coins",
                            "",
                            bankBalance > 0 ? "§eKlicken zum Abheben" : "§cKein Bankguthaben"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());
            if (economyPlayer == null || economyPlayer.getBankBalance() <= 0) {
                player.sendMessage(ColorUtil.component("§cDu hast kein Bankguthaben zum Abheben!"));
                return;
            }

            double amount = economyPlayer.getBankBalance();
            plugin.getEconomyManager().withdrawFromBank(player.getUniqueId(), amount).thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        player.sendMessage(ColorUtil.component("§6" + String.format("%.2f", amount) +
                                " Coins §7wurden abgehoben!"));
                        updateGUI();
                    } else {
                        player.sendMessage(ColorUtil.component("§cFehler beim Abheben!"));
                    }
                });
            });
        }
    }

    private class Withdraw100Item extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());
            double bankBalance = economyPlayer != null ? economyPlayer.getBankBalance() : 0.0;
            boolean canWithdraw = bankBalance >= 100;

            return new ItemBuilder(canWithdraw ? Material.RED_DYE : Material.GRAY_DYE)
                    .setDisplayName("§c100 Coins abheben")
                    .setLegacyLore(List.of(
                            "§7Hebe 100 Coins",
                            "§7von der Bank ab",
                            "",
                            "§7Bankguthaben: §6" + String.format("%.2f", bankBalance) + " Coins",
                            "",
                            canWithdraw ? "§eKlicken zum Abheben" : "§cNicht genug Bankguthaben"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            plugin.getEconomyManager().withdrawFromBank(player.getUniqueId(), 100.0).thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        player.sendMessage(ColorUtil.component("§6100 Coins §7wurden abgehoben!"));
                        updateGUI();
                    } else {
                        player.sendMessage(ColorUtil.component("§cNicht genug Bankguthaben!"));
                    }
                });
            });
        }
    }

    private class Withdraw1000Item extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());
            double bankBalance = economyPlayer != null ? economyPlayer.getBankBalance() : 0.0;
            boolean canWithdraw = bankBalance >= 1000;

            return new ItemBuilder(canWithdraw ? Material.RED_DYE : Material.GRAY_DYE)
                    .setDisplayName("§c1000 Coins abheben")
                    .setLegacyLore(List.of(
                            "§7Hebe 1000 Coins",
                            "§7von der Bank ab",
                            "",
                            "§7Bankguthaben: §6" + String.format("%.2f", bankBalance) + " Coins",
                            "",
                            canWithdraw ? "§eKlicken zum Abheben" : "§cNicht genug Bankguthaben"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            plugin.getEconomyManager().withdrawFromBank(player.getUniqueId(), 1000.0).thenAccept(success -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (success) {
                        player.sendMessage(ColorUtil.component("§61000 Coins §7wurden abgehoben!"));
                        updateGUI();
                    } else {
                        player.sendMessage(ColorUtil.component("§cNicht genug Bankguthaben!"));
                    }
                });
            });
        }
    }

    private class CoinsGUIItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.GOLD_BLOCK)
                    .setDisplayName("§6Coins verwalten")
                    .setLegacyLore(List.of(
                            "§7Öffne das Coins-GUI",
                            "§7für alle Coin-Funktionen",
                            "",
                            "§eKlicken zum Öffnen"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            new CoinsGUI(plugin, player).open();
        }
    }

    private static class HelpItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.BOOK)
                    .setDisplayName("§eHilfe")
                    .setLegacyLore(List.of(
                            "§7Bank-Commands:",
                            "§6/bank deposit <amount>",
                            "§6/bank withdraw <amount>",
                            "§6/bank balance",
                            "",
                            "§eKlicken für mehr Infos"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            player.closeInventory();
            player.sendMessage(ColorUtil.component("§6═══ Bank Commands ═══"));
            player.sendMessage(ColorUtil.component("§6/bank §7- Bank-GUI öffnen"));
            player.sendMessage(ColorUtil.component("§6/bank deposit <betrag> §7- Coins einzahlen"));
            player.sendMessage(ColorUtil.component("§6/bank withdraw <betrag> §7- Coins abheben"));
            player.sendMessage(ColorUtil.component("§6/bank balance §7- Bankguthaben anzeigen"));
            player.sendMessage(ColorUtil.component("§7Verwende §6'all' §7als Betrag für alles"));
        }
    }

    private class CloseItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider() {
            return new ItemBuilder(Material.BARRIER)
                    .setDisplayName("§cSchließen")
                    .setLegacyLore(List.of(
                            "§7Schließe das Bank-GUI",
                            "",
                            "§eKlicken zum Schließen"
                    ));
        }

        @Override
        public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
            player.closeInventory();
            plugin.getGUIManager().unregisterBankGUI(player.getUniqueId());
        }
    }
}