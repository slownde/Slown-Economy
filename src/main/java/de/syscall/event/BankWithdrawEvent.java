package de.syscall.event;

import de.syscall.data.EconomyPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BankWithdrawEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final EconomyPlayer player;
    private final double amount;
    private final double oldCoins;
    private final double newCoins;
    private final double oldBankBalance;
    private final double newBankBalance;

    public BankWithdrawEvent(EconomyPlayer player, double amount, double oldCoins, double newCoins,
                             double oldBankBalance, double newBankBalance) {
        this.player = player;
        this.amount = amount;
        this.oldCoins = oldCoins;
        this.newCoins = newCoins;
        this.oldBankBalance = oldBankBalance;
        this.newBankBalance = newBankBalance;
    }

    public EconomyPlayer getPlayer() {
        return player;
    }

    public double getAmount() {
        return amount;
    }

    public double getOldCoins() {
        return oldCoins;
    }

    public double getNewCoins() {
        return newCoins;
    }

    public double getOldBankBalance() {
        return oldBankBalance;
    }

    public double getNewBankBalance() {
        return newBankBalance;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}