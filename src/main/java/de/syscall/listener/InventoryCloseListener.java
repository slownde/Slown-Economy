package de.syscall.listener;

import de.syscall.SlownEconomy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class InventoryCloseListener implements Listener {

    private final SlownEconomy plugin;

    public InventoryCloseListener(SlownEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            String title = event.getView().getTitle();

            if (title.contains("Bank Management") || title.contains("Coins Management")) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    plugin.getGUIManager().clearPlayerGUIs(player.getUniqueId());
                }, 1L);
            }
        }
    }
}