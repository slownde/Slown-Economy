package de.syscall.event;

import de.syscall.data.EconomyPlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CoinsTransferEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final EconomyPlayer fromPlayer;
    private final EconomyPlayer toPlayer;
    private final double amount;
    private boolean cancelled = false;

    public CoinsTransferEvent(EconomyPlayer fromPlayer, EconomyPlayer toPlayer, double amount) {
        this.fromPlayer = fromPlayer;
        this.toPlayer = toPlayer;
        this.amount = amount;
    }

    public EconomyPlayer getFromPlayer() {
        return fromPlayer;
    }

    public EconomyPlayer getToPlayer() {
        return toPlayer;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}