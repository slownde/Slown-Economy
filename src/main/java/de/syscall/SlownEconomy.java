package de.syscall;

import de.syscall.api.EconomyAPI;
import de.syscall.command.BankCommand;
import de.syscall.command.CoinsCommand;
import de.syscall.command.TransferCommand;
import de.syscall.database.DatabaseManager;
import de.syscall.listener.PlayerJoinListener;
import de.syscall.manager.CacheManager;
import de.syscall.manager.EconomyManager;
import de.syscall.util.EconomyValidator;
import org.bukkit.plugin.java.JavaPlugin;

public class SlownEconomy extends JavaPlugin {

    private static SlownEconomy instance;
    private static EconomyAPI api;

    private DatabaseManager databaseManager;
    private CacheManager cacheManager;
    private EconomyManager economyManager;
    private EconomyValidator economyValidator;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.databaseManager = new DatabaseManager(this);
        this.cacheManager = new CacheManager(this);
        this.economyManager = new EconomyManager(this);
        this.economyValidator = new EconomyValidator(this);

        databaseManager.initialize();

        api = new EconomyAPI(this);

        registerCommands();
        registerListeners();

        getLogger().info("Slown-Economy successfully started!");
    }

    @Override
    public void onDisable() {
        if (economyManager != null) {
            economyManager.shutdown();
        }

        if (cacheManager != null) {
            cacheManager.saveAll();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("Slown-Economy stopped!");
    }

    private void registerCommands() {
        CoinsCommand coinsCommand = new CoinsCommand(this);
        BankCommand bankCommand = new BankCommand(this);
        TransferCommand transferCommand = new TransferCommand(this);

        getCommand("coins").setExecutor(coinsCommand);
        getCommand("coins").setTabCompleter(coinsCommand);

        getCommand("bank").setExecutor(bankCommand);
        getCommand("bank").setTabCompleter(bankCommand);

        getCommand("transfer").setExecutor(transferCommand);
        getCommand("transfer").setTabCompleter(transferCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    }

    public static SlownEconomy getInstance() {
        return instance;
    }

    public static EconomyAPI getAPI() {
        return api;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public EconomyValidator getEconomyValidator() {
        return economyValidator;
    }
}