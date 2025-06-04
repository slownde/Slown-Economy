package de.syscall.event;

import de.syscall.data.EconomyPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BankChangeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final EconomyPlayer player;
    private final double oldAmount;
    private final double newAmount;
    private final Cause cause;

    public BankChangeEvent(EconomyPlayer player, double oldAmount, double newAmount, Cause cause) {
        this.player = player;
        this.oldAmount = oldAmount;
        this.newAmount = newAmount;
        this.cause = cause;
    }

    public EconomyPlayer getPlayer() {
        return player;
    }

    public double getOldAmount() {
        return oldAmount;
    }

    public double getNewAmount() {
        return newAmount;
    }

    public double getDifference() {
        return newAmount - oldAmount;
    }

    public Cause getCause() {
        return cause;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public enum Cause {
        SET,
        ADD,
        REMOVE,
        DEPOSIT,
        WITHDRAW,
        ADMIN,
        PLUGIN
    }
}