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

        try {
            saveDefaultConfig();
            validateConfig();

            this.databaseManager = new DatabaseManager(this);
            this.cacheManager = new CacheManager(this);
            this.economyManager = new EconomyManager(this);
            this.economyValidator = new EconomyValidator(this);

            databaseManager.initialize();

            api = new EconomyAPI(this);

            registerCommands();
            registerListeners();

            getLogger().info("Slown-Economy successfully started!");

        } catch (Exception e) {
            getLogger().severe("Failed to initialize plugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {

            if (economyManager != null) {
                economyManager.shutdown();
            }

            if (cacheManager != null) {
                cacheManager.saveAll();
            }

            if (databaseManager != null) {
                databaseManager.close();
            }

            getLogger().info("Slown-Economy stopped successfully!");

        } catch (Exception e) {
            getLogger().severe("Error during plugin shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void validateConfig() {
        double maxCoins = getConfig().getDouble("economy.max-coins", 999999999.0);
        double maxBank = getConfig().getDouble("economy.max-bank-balance", 999999999.0);
        double startingCoins = getConfig().getDouble("economy.starting-coins", 100.0);
        double startingBank = getConfig().getDouble("economy.starting-bank-balance", 0.0);

        if (maxCoins <= 0 || maxBank <= 0) {
            throw new IllegalArgumentException("Max coins and bank balance must be positive");
        }

        if (startingCoins < 0 || startingBank < 0) {
            throw new IllegalArgumentException("Starting amounts cannot be negative");
        }

        if (startingCoins > maxCoins) {
            getLogger().warning("Starting coins exceed max coins limit, adjusting...");
            getConfig().set("economy.starting-coins", maxCoins);
        }

        if (startingBank > maxBank) {
            getLogger().warning("Starting bank balance exceeds max bank limit, adjusting...");
            getConfig().set("economy.starting-bank-balance", maxBank);
        }

        saveConfig();
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