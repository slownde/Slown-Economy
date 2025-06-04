package de.syscall.gui;

import de.syscall.SlownEconomy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import xyz.xenondevs.invui.gui.Gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class GUIHandler implements Listener {

    protected final SlownEconomy plugin;
    private static final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();

    public GUIHandler(SlownEconomy plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    protected void startUpdateTask(Player player, Gui gui) {
        stopUpdateTask(player);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.getOpenInventory().getTopInventory().getHolder() != gui) {
                    cancel();
                    activeTasks.remove(player.getUniqueId());
                    return;
                }
                gui.notifyAll();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        activeTasks.put(player.getUniqueId(), task);
    }

    protected void stopUpdateTask(Player player) {
        BukkitTask task = activeTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            stopUpdateTask(player);
        }
    }

    public static void cleanupAllTasks() {
        activeTasks.values().forEach(BukkitTask::cancel);
        activeTasks.clear();
    }
}