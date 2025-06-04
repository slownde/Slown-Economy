package de.syscall.data;

import de.syscall.SlownEconomy;

import java.util.UUID;

public class EconomyPlayer {

    private final UUID uuid;
    private final String name;
    private double coins;
    private double bankBalance;
    private long lastSeen;
    private boolean modified;

    public EconomyPlayer(UUID uuid, String name, double coins, double bankBalance, long lastSeen) {
        this.uuid = uuid;
        this.name = name;
        this.coins = coins;
        this.bankBalance = bankBalance;
        this.lastSeen = lastSeen;
        this.modified = false;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public double getCoins() {
        return coins;
    }

    public void setCoins(double coins) {
        double maxCoins = getMaxCoins();
        this.coins = Math.max(0, Math.min(coins, maxCoins));
        this.modified = true;
    }

    public void addCoins(double amount) {
        setCoins(this.coins + amount);
    }

    public boolean removeCoins(double amount) {
        if (this.coins >= amount) {
            setCoins(this.coins - amount);
            return true;
        }
        return false;
    }

    public double getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(double bankBalance) {
        double maxBankBalance = getMaxBankBalance();
        this.bankBalance = Math.max(0, Math.min(bankBalance, maxBankBalance));
        this.modified = true;
    }

    public void addBankBalance(double amount) {
        setBankBalance(this.bankBalance + amount);
    }

    public boolean removeBankBalance(double amount) {
        if (this.bankBalance >= amount) {
            setBankBalance(this.bankBalance - amount);
            return true;
        }
        return false;
    }

    public boolean depositToBank(double amount) {
        if (removeCoins(amount)) {
            double newBankBalance = this.bankBalance + amount;
            if (newBankBalance <= getMaxBankBalance()) {
                addBankBalance(amount);
                return true;
            } else {
                addCoins(amount);
                return false;
            }
        }
        return false;
    }

    public boolean withdrawFromBank(double amount) {
        if (removeBankBalance(amount)) {
            double newCoins = this.coins + amount;
            if (newCoins <= getMaxCoins()) {
                addCoins(amount);
                return true;
            } else {
                addBankBalance(amount);
                return false;
            }
        }
        return false;
    }

    public boolean wouldExceedCoinsLimit(double additionalAmount) {
        return (this.coins + additionalAmount) > getMaxCoins();
    }

    public boolean wouldExceedBankLimit(double additionalAmount) {
        return (this.bankBalance + additionalAmount) > getMaxBankBalance();
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
        this.modified = true;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public void updateLastSeen() {
        setLastSeen(System.currentTimeMillis());
    }

    public double getTotalWealth() {
        return coins + bankBalance;
    }

    public EconomyPlayer copy() {
        return new EconomyPlayer(uuid, name, coins, bankBalance, lastSeen);
    }

    private double getMaxCoins() {
        return SlownEconomy.getInstance().getEconomyValidator().getMaxCoins();
    }

    private double getMaxBankBalance() {
        return SlownEconomy.getInstance().getEconomyValidator().getMaxBankBalance();
    }

    @Override
    public String toString() {
        return "EconomyPlayer{" +
                "uuid=" + uuid +
                ", name='" + name + '\'' +
                ", coins=" + coins +
                ", bankBalance=" + bankBalance +
                ", lastSeen=" + lastSeen +
                '}';
    }
}