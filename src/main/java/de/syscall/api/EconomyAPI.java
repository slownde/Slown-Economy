package de.syscall.api;

import de.syscall.SlownEconomy;
import de.syscall.data.EconomyPlayer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EconomyAPI {

    private static EconomyAPI instance;
    private final SlownEconomy plugin;

    public EconomyAPI(SlownEconomy plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static EconomyAPI getInstance() {
        return instance;
    }

    public CompletableFuture<Double> getCoins(UUID playerUuid) {
        return plugin.getEconomyManager().getCoins(playerUuid);
    }

    public CompletableFuture<Double> getCoins(OfflinePlayer player) {
        return getCoins(player.getUniqueId());
    }

    public double getCoins(Player player) {
        EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());
        return economyPlayer != null ? economyPlayer.getCoins() : 0.0;
    }

    public CompletableFuture<Boolean> setCoins(UUID playerUuid, double amount) {
        return plugin.getEconomyManager().setCoins(playerUuid, amount);
    }

    public CompletableFuture<Boolean> setCoins(OfflinePlayer player, double amount) {
        return setCoins(player.getUniqueId(), amount);
    }

    public CompletableFuture<Boolean> addCoins(UUID playerUuid, double amount) {
        return plugin.getEconomyManager().addCoins(playerUuid, amount);
    }

    public CompletableFuture<Boolean> addCoins(OfflinePlayer player, double amount) {
        return addCoins(player.getUniqueId(), amount);
    }

    public CompletableFuture<Boolean> removeCoins(UUID playerUuid, double amount) {
        return plugin.getEconomyManager().removeCoins(playerUuid, amount);
    }

    public CompletableFuture<Boolean> removeCoins(OfflinePlayer player, double amount) {
        return removeCoins(player.getUniqueId(), amount);
    }

    public CompletableFuture<Double> getBankBalance(UUID playerUuid) {
        return plugin.getEconomyManager().getBankBalance(playerUuid);
    }

    public CompletableFuture<Double> getBankBalance(OfflinePlayer player) {
        return getBankBalance(player.getUniqueId());
    }

    public double getBankBalance(Player player) {
        EconomyPlayer economyPlayer = plugin.getCacheManager().getPlayer(player.getUniqueId());
        return economyPlayer != null ? economyPlayer.getBankBalance() : 0.0;
    }

    public CompletableFuture<Boolean> setBankBalance(UUID playerUuid, double amount) {
        return plugin.getEconomyManager().setBankBalance(playerUuid, amount);
    }

    public CompletableFuture<Boolean> setBankBalance(OfflinePlayer player, double amount) {
        return setBankBalance(player.getUniqueId(), amount);
    }

    public CompletableFuture<Boolean> addBankBalance(UUID playerUuid, double amount) {
        return plugin.getEconomyManager().addBankBalance(playerUuid, amount);
    }

    public CompletableFuture<Boolean> addBankBalance(OfflinePlayer player, double amount) {
        return addBankBalance(player.getUniqueId(), amount);
    }

    public CompletableFuture<Boolean> removeBankBalance(UUID playerUuid, double amount) {
        return plugin.getEconomyManager().removeBankBalance(playerUuid, amount);
    }

    public CompletableFuture<Boolean> removeBankBalance(OfflinePlayer player, double amount) {
        return removeBankBalance(player.getUniqueId(), amount);
    }

    public CompletableFuture<Boolean> depositToBank(UUID playerUuid, double amount) {
        return plugin.getEconomyManager().depositToBank(playerUuid, amount);
    }

    public CompletableFuture<Boolean> depositToBank(OfflinePlayer player, double amount) {
        return depositToBank(player.getUniqueId(), amount);
    }

    public CompletableFuture<Boolean> withdrawFromBank(UUID playerUuid, double amount) {
        return plugin.getEconomyManager().withdrawFromBank(playerUuid, amount);
    }

    public CompletableFuture<Boolean> withdrawFromBank(OfflinePlayer player, double amount) {
        return withdrawFromBank(player.getUniqueId(), amount);
    }

    public CompletableFuture<Boolean> transferCoins(UUID fromPlayer, UUID toPlayer, double amount) {
        return plugin.getEconomyManager().transferCoins(fromPlayer, toPlayer, amount);
    }

    public CompletableFuture<Boolean> transferCoins(OfflinePlayer fromPlayer, OfflinePlayer toPlayer, double amount) {
        return transferCoins(fromPlayer.getUniqueId(), toPlayer.getUniqueId(), amount);
    }

    public CompletableFuture<Boolean> transferBankBalance(UUID fromPlayer, UUID toPlayer, double amount) {
        return plugin.getEconomyManager().transferBankBalance(fromPlayer, toPlayer, amount);
    }

    public CompletableFuture<Boolean> transferBankBalance(OfflinePlayer fromPlayer, OfflinePlayer toPlayer, double amount) {
        return transferBankBalance(fromPlayer.getUniqueId(), toPlayer.getUniqueId(), amount);
    }

    public CompletableFuture<EconomyPlayer> getEconomyPlayer(UUID playerUuid) {
        return plugin.getEconomyManager().getEconomyPlayer(playerUuid);
    }

    public CompletableFuture<EconomyPlayer> getEconomyPlayer(OfflinePlayer player) {
        return getEconomyPlayer(player.getUniqueId());
    }

    public EconomyPlayer getCachedPlayer(Player player) {
        return plugin.getCacheManager().getPlayer(player.getUniqueId());
    }
}