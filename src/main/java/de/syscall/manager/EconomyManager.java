package de.syscall.manager;

import de.syscall.SlownEconomy;
import de.syscall.data.EconomyPlayer;
import de.syscall.event.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EconomyManager {

    private final SlownEconomy plugin;

    public EconomyManager(SlownEconomy plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<EconomyPlayer> getEconomyPlayer(UUID uuid) {
        EconomyPlayer cached = plugin.getCacheManager().getPlayer(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";

        return plugin.getCacheManager().loadPlayer(uuid, name);
    }

    public CompletableFuture<Double> getCoins(UUID uuid) {
        return getEconomyPlayer(uuid).thenApply(EconomyPlayer::getCoins);
    }

    public CompletableFuture<Boolean> setCoins(UUID uuid, double amount) {
        return getEconomyPlayer(uuid).thenApply(player -> {
            double oldAmount = player.getCoins();
            player.setCoins(amount);
            plugin.getCacheManager().updatePlayer(player);

            CoinsChangeEvent event = new CoinsChangeEvent(player, oldAmount, amount, CoinsChangeEvent.Cause.SET);
            Bukkit.getPluginManager().callEvent(event);

            return true;
        });
    }

    public CompletableFuture<Boolean> addCoins(UUID uuid, double amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(false);

        return getEconomyPlayer(uuid).thenApply(player -> {
            double oldAmount = player.getCoins();
            player.addCoins(amount);
            plugin.getCacheManager().updatePlayer(player);

            CoinsChangeEvent event = new CoinsChangeEvent(player, oldAmount, player.getCoins(), CoinsChangeEvent.Cause.ADD);
            Bukkit.getPluginManager().callEvent(event);

            return true;
        });
    }

    public CompletableFuture<Boolean> removeCoins(UUID uuid, double amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(false);

        return getEconomyPlayer(uuid).thenApply(player -> {
            double oldAmount = player.getCoins();
            boolean success = player.removeCoins(amount);

            if (success) {
                plugin.getCacheManager().updatePlayer(player);

                CoinsChangeEvent event = new CoinsChangeEvent(player, oldAmount, player.getCoins(), CoinsChangeEvent.Cause.REMOVE);
                Bukkit.getPluginManager().callEvent(event);
            }

            return success;
        });
    }

    public CompletableFuture<Double> getBankBalance(UUID uuid) {
        return getEconomyPlayer(uuid).thenApply(EconomyPlayer::getBankBalance);
    }

    public CompletableFuture<Boolean> setBankBalance(UUID uuid, double amount) {
        return getEconomyPlayer(uuid).thenApply(player -> {
            double oldAmount = player.getBankBalance();
            player.setBankBalance(amount);
            plugin.getCacheManager().updatePlayer(player);

            BankChangeEvent event = new BankChangeEvent(player, oldAmount, amount, BankChangeEvent.Cause.SET);
            Bukkit.getPluginManager().callEvent(event);

            return true;
        });
    }

    public CompletableFuture<Boolean> addBankBalance(UUID uuid, double amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(false);

        return getEconomyPlayer(uuid).thenApply(player -> {
            double oldAmount = player.getBankBalance();
            player.addBankBalance(amount);
            plugin.getCacheManager().updatePlayer(player);

            BankChangeEvent event = new BankChangeEvent(player, oldAmount, player.getBankBalance(), BankChangeEvent.Cause.ADD);
            Bukkit.getPluginManager().callEvent(event);

            return true;
        });
    }

    public CompletableFuture<Boolean> removeBankBalance(UUID uuid, double amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(false);

        return getEconomyPlayer(uuid).thenApply(player -> {
            double oldAmount = player.getBankBalance();
            boolean success = player.removeBankBalance(amount);

            if (success) {
                plugin.getCacheManager().updatePlayer(player);

                BankChangeEvent event = new BankChangeEvent(player, oldAmount, player.getBankBalance(), BankChangeEvent.Cause.REMOVE);
                Bukkit.getPluginManager().callEvent(event);
            }

            return success;
        });
    }

    public CompletableFuture<Boolean> depositToBank(UUID uuid, double amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(false);

        return getEconomyPlayer(uuid).thenApply(player -> {
            double oldCoins = player.getCoins();
            double oldBank = player.getBankBalance();

            boolean success = player.depositToBank(amount);

            if (success) {
                plugin.getCacheManager().updatePlayer(player);

                BankDepositEvent event = new BankDepositEvent(player, amount, oldCoins, player.getCoins(), oldBank, player.getBankBalance());
                Bukkit.getPluginManager().callEvent(event);
            }

            return success;
        });
    }

    public CompletableFuture<Boolean> withdrawFromBank(UUID uuid, double amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(false);

        return getEconomyPlayer(uuid).thenApply(player -> {
            double oldCoins = player.getCoins();
            double oldBank = player.getBankBalance();

            boolean success = player.withdrawFromBank(amount);

            if (success) {
                plugin.getCacheManager().updatePlayer(player);

                BankWithdrawEvent event = new BankWithdrawEvent(player, amount, oldCoins, player.getCoins(), oldBank, player.getBankBalance());
                Bukkit.getPluginManager().callEvent(event);
            }

            return success;
        });
    }

    public CompletableFuture<Boolean> transferCoins(UUID fromUuid, UUID toUuid, double amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(false);

        if (!isTransferValid(amount)) {
            return CompletableFuture.completedFuture(false);
        }

        return getEconomyPlayer(fromUuid).thenCompose(fromPlayer -> {
            if (fromPlayer.getCoins() < amount) {
                return CompletableFuture.completedFuture(false);
            }

            return getEconomyPlayer(toUuid).thenApply(toPlayer -> {
                double fee = calculateTransferFee(amount);
                double totalDeduction = amount + fee;

                if (fromPlayer.getCoins() < totalDeduction) {
                    return false;
                }

                double fromOldCoins = fromPlayer.getCoins();
                double toOldCoins = toPlayer.getCoins();

                fromPlayer.removeCoins(totalDeduction);
                toPlayer.addCoins(amount);

                plugin.getCacheManager().updatePlayer(fromPlayer);
                plugin.getCacheManager().updatePlayer(toPlayer);

                CoinsTransferEvent event = new CoinsTransferEvent(fromPlayer, toPlayer, amount);
                Bukkit.getPluginManager().callEvent(event);

                return true;
            });
        });
    }

    private boolean isTransferValid(double amount) {
        if (!plugin.getConfig().getBoolean("economy.transfer.enabled", true)) {
            return false;
        }

        return !plugin.getEconomyValidator().isTransferAmountInvalid(amount);
    }

    private double calculateTransferFee(double amount) {
        double feePercentage = plugin.getConfig().getDouble("economy.transfer.fee-percentage", 0.0);
        return amount * (feePercentage / 100.0);
    }

    public void shutdown() {
        plugin.getCacheManager().saveAll();
    }
}