package de.syscall.listener;

import de.syscall.SlownEconomy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {

    private final SlownEconomy plugin;

    public PlayerJoinListener(SlownEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        plugin.getCacheManager().cancelPlayerRemoval(player.getUniqueId());

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getCacheManager().loadPlayer(player.getUniqueId(), player.getName());
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getCacheManager().savePlayer(player.getUniqueId()).thenRun(() -> {
                plugin.getCacheManager().schedulePlayerRemoval(player.getUniqueId());

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    plugin.getCacheManager().removePlayer(player.getUniqueId());
                }, 1200L);
            });
        });
    }
}