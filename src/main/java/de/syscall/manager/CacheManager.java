package de.syscall.manager;

import de.syscall.SlownEconomy;
import de.syscall.data.EconomyPlayer;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {

    private final SlownEconomy plugin;
    private final Map<UUID, EconomyPlayer> playerCache;
    private final Map<String, UUID> nameToUuidCache;
    private final Set<UUID> pendingRemovals;
    private BukkitTask saveTask;
    private BukkitTask cleanupTask;

    public CacheManager(SlownEconomy plugin) {
        this.plugin = plugin;
        this.playerCache = new ConcurrentHashMap<>();
        this.nameToUuidCache = new ConcurrentHashMap<>();
        this.pendingRemovals = ConcurrentHashMap.newKeySet();
        startTasks();
    }

    private void startTasks() {
        long saveInterval = plugin.getConfig().getLong("cache.save-interval", 300) * 20L;
        long cleanupInterval = plugin.getConfig().getLong("cache.cleanup-interval", 600) * 20L;

        saveTask = new BukkitRunnable() {
            @Override
            public void run() {
                saveModifiedPlayers();
            }
        }.runTaskTimerAsynchronously(plugin, saveInterval, saveInterval);

        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupCache();
            }
        }.runTaskTimerAsynchronously(plugin, cleanupInterval, cleanupInterval);
    }

    public CompletableFuture<EconomyPlayer> loadPlayer(UUID uuid, String name) {
        pendingRemovals.remove(uuid);

        EconomyPlayer cached = playerCache.get(uuid);
        if (cached != null) {
            cached.updateLastSeen();
            nameToUuidCache.put(name.toLowerCase(), uuid);
            return CompletableFuture.completedFuture(cached);
        }

        return plugin.getDatabaseManager().loadPlayer(uuid, name).thenApply(player -> {
            player.updateLastSeen();
            playerCache.put(uuid, player);
            nameToUuidCache.put(name.toLowerCase(), uuid);
            return player;
        });
    }

    public EconomyPlayer getPlayer(UUID uuid) {
        return playerCache.get(uuid);
    }

    public CompletableFuture<EconomyPlayer> getPlayerByName(String name) {
        UUID uuid = nameToUuidCache.get(name.toLowerCase());
        if (uuid != null) {
            EconomyPlayer player = playerCache.get(uuid);
            if (player != null) {
                return CompletableFuture.completedFuture(player);
            }
        }

        return plugin.getDatabaseManager().findPlayerByName(name).thenApply(player -> {
            if (player != null) {
                playerCache.put(player.getUuid(), player);
                nameToUuidCache.put(name.toLowerCase(), player.getUuid());
            }
            return player;
        });
    }

    public void updatePlayer(EconomyPlayer player) {
        player.setModified(true);
        player.updateLastSeen();
        playerCache.put(player.getUuid(), player);
        nameToUuidCache.put(player.getName().toLowerCase(), player.getUuid());
    }

    public CompletableFuture<Void> savePlayer(UUID uuid) {
        EconomyPlayer player = playerCache.get(uuid);
        if (player != null && player.isModified()) {
            return plugin.getDatabaseManager().savePlayer(player).thenRun(() -> {
                player.setModified(false);
            });
        }
        return CompletableFuture.completedFuture(null);
    }

    public void schedulePlayerRemoval(UUID uuid) {
        pendingRemovals.add(uuid);
    }

    public void cancelPlayerRemoval(UUID uuid) {
        pendingRemovals.remove(uuid);
    }

    public void removePlayer(UUID uuid) {
        if (!pendingRemovals.contains(uuid)) {
            return;
        }

        EconomyPlayer player = playerCache.remove(uuid);
        if (player != null) {
            nameToUuidCache.remove(player.getName().toLowerCase());
            pendingRemovals.remove(uuid);

            if (player.isModified()) {
                plugin.getDatabaseManager().savePlayer(player);
            }
        }
    }

    public void saveModifiedPlayers() {
        playerCache.values().parallelStream()
                .filter(EconomyPlayer::isModified)
                .forEach(player -> {
                    plugin.getDatabaseManager().savePlayer(player).thenRun(() -> {
                        player.setModified(false);
                    });
                });
    }

    public void saveAll() {
        if (saveTask != null) {
            saveTask.cancel();
        }

        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        for (EconomyPlayer player : playerCache.values()) {
            if (player.isModified()) {
                plugin.getDatabaseManager().savePlayer(player).join();
                player.setModified(false);
            }
        }

        plugin.getLogger().info("All cached data saved");
    }

    private void cleanupCache() {
        long maxAge = plugin.getConfig().getLong("cache.max-age", 1800) * 1000L;
        long currentTime = System.currentTimeMillis();

        playerCache.entrySet().removeIf(entry -> {
            EconomyPlayer player = entry.getValue();
            UUID uuid = entry.getKey();

            if (plugin.getServer().getPlayer(uuid) != null) {
                return false;
            }

            boolean shouldRemove = (currentTime - player.getLastSeen()) > maxAge;

            if (shouldRemove) {
                nameToUuidCache.remove(player.getName().toLowerCase());
                pendingRemovals.remove(uuid);

                if (player.isModified()) {
                    plugin.getDatabaseManager().savePlayer(player);
                }

                return true;
            }

            return false;
        });
    }

    public int getCacheSize() {
        return playerCache.size();
    }

    public void clearCache() {
        saveModifiedPlayers();
        playerCache.clear();
        nameToUuidCache.clear();
        pendingRemovals.clear();
    }
}