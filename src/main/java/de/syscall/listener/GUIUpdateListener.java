package de.syscall.listener;

import de.syscall.SlownEconomy;
import de.syscall.event.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class GUIUpdateListener implements Listener {

    private final SlownEconomy plugin;

    public GUIUpdateListener(SlownEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCoinsChange(CoinsChangeEvent event) {
        Player player = plugin.getServer().getPlayer(event.getPlayer().getUuid());
        if (player != null) {
            updatePlayerGUIs(player);
        }
    }

    @EventHandler
    public void onBankChange(BankChangeEvent event) {
        Player player = plugin.getServer().getPlayer(event.getPlayer().getUuid());
        if (player != null) {
            updatePlayerGUIs(player);
        }
    }

    @EventHandler
    public void onBankDeposit(BankDepositEvent event) {
        Player player = plugin.getServer().getPlayer(event.getPlayer().getUuid());
        if (player != null) {
            updatePlayerGUIs(player);
        }
    }

    @EventHandler
    public void onBankWithdraw(BankWithdrawEvent event) {
        Player player = plugin.getServer().getPlayer(event.getPlayer().getUuid());
        if (player != null) {
            updatePlayerGUIs(player);
        }
    }

    @EventHandler
    public void onCoinsTransfer(CoinsTransferEvent event) {
        Player fromPlayer = plugin.getServer().getPlayer(event.getFromPlayer().getUuid());
        Player toPlayer = plugin.getServer().getPlayer(event.getToPlayer().getUuid());

        if (fromPlayer != null) {
            updatePlayerGUIs(fromPlayer);
        }
        if (toPlayer != null) {
            updatePlayerGUIs(toPlayer);
        }
    }

    private void updatePlayerGUIs(Player player) {
        plugin.getGUIManager().updatePlayerGUIs(player.getUniqueId());
    }
}