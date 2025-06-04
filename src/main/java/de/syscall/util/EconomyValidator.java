package de.syscall.util;

import de.syscall.SlownEconomy;

public class EconomyValidator {

    private final SlownEconomy plugin;

    public EconomyValidator(SlownEconomy plugin) {
        this.plugin = plugin;
    }

    public boolean isCoinsAmountTooHigh(double amount) {
        if (amount < 0) return true;
        double maxCoins = getMaxCoins();
        return amount > maxCoins;
    }

    public boolean isBankAmountTooHigh(double amount) {
        if (amount < 0) return true;
        double maxBank = getMaxBankBalance();
        return amount > maxBank;
    }

    public boolean isTransferEnabled() {
        return plugin.getConfig().getBoolean("economy.transfer.enabled", true);
    }

    public boolean isTransferAmountInvalid(double amount) {
        if (!isTransferEnabled()) return true;
        if (amount <= 0) return true;

        double minAmount = getMinTransferAmount();
        double maxAmount = getMaxTransferAmount();

        return amount < minAmount || amount > maxAmount;
    }

    public double getTransferFee(double amount) {
        double feePercentage = getTransferFeePercentage();
        return amount * (feePercentage / 100.0);
    }

    public double getTotalTransferCost(double amount) {
        return amount + getTransferFee(amount);
    }

    public double getMaxCoins() {
        return plugin.getConfig().getDouble("economy.max-coins", 999999999.0);
    }

    public double getMaxBankBalance() {
        return plugin.getConfig().getDouble("economy.max-bank-balance", 999999999.0);
    }

    public double getStartingCoins() {
        return plugin.getConfig().getDouble("economy.starting-coins", 100.0);
    }

    public double getStartingBankBalance() {
        return plugin.getConfig().getDouble("economy.starting-bank-balance", 0.0);
    }

    public double getMinTransferAmount() {
        return plugin.getConfig().getDouble("economy.transfer.min-amount", 1.0);
    }

    public double getMaxTransferAmount() {
        return plugin.getConfig().getDouble("economy.transfer.max-amount", 100000.0);
    }

    public double getTransferFeePercentage() {
        return plugin.getConfig().getDouble("economy.transfer.fee-percentage", 0.0);
    }

    public boolean canAffordTransfer(double playerCoins, double transferAmount) {
        double totalCost = getTotalTransferCost(transferAmount);
        return playerCoins >= totalCost;
    }

    public boolean wouldExceedCoinsLimit(double currentCoins, double addAmount) {
        return (currentCoins + addAmount) > getMaxCoins();
    }

    public boolean wouldExceedBankLimit(double currentBank, double addAmount) {
        return (currentBank + addAmount) > getMaxBankBalance();
    }

    public String getTransferValidationError(double amount) {
        if (!isTransferEnabled()) {
            return "Transfers sind deaktiviert";
        }

        if (amount <= 0) {
            return "Betrag muss positiv sein";
        }

        double minAmount = getMinTransferAmount();
        double maxAmount = getMaxTransferAmount();

        if (amount < minAmount) {
            return "Minimum: " + String.format("%.2f", minAmount) + " Coins";
        }

        if (amount > maxAmount) {
            return "Maximum: " + String.format("%.2f", maxAmount) + " Coins";
        }

        return null;
    }

    public String getCoinsValidationError(double amount) {
        if (amount < 0) {
            return "Betrag kann nicht negativ sein";
        }

        double maxCoins = getMaxCoins();
        if (amount > maxCoins) {
            return "Maximum: " + String.format("%.2f", maxCoins) + " Coins";
        }

        return null;
    }

    public String getBankValidationError(double amount) {
        if (amount < 0) {
            return "Betrag kann nicht negativ sein";
        }

        double maxBank = getMaxBankBalance();
        if (amount > maxBank) {
            return "Maximum: " + String.format("%.2f", maxBank) + " Coins";
        }

        return null;
    }
}