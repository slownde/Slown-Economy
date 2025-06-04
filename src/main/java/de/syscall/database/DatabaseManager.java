package de.syscall.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.syscall.SlownEconomy;
import de.syscall.data.EconomyPlayer;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final SlownEconomy plugin;
    private HikariDataSource dataSource;
    private final boolean useMySQL;
    private final String sqliteFile;

    public DatabaseManager(SlownEconomy plugin) {
        this.plugin = plugin;
        this.useMySQL = plugin.getConfig().getBoolean("database.mysql.enabled", false);
        this.sqliteFile = plugin.getDataFolder() + File.separator + "economy.db";
    }

    public void initialize() {
        if (useMySQL) {
            setupMySQL();
        } else {
            setupSQLite();
        }

        createTables();

        if (useMySQL && shouldMigrateSQLiteData()) {
            migrateSQLiteToMySQL();
        }
    }

    private void setupMySQL() {
        HikariConfig config = new HikariConfig();

        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "economy");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "");

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setLeakDetectionThreshold(60000);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);

        plugin.getLogger().info("MySQL connection established");
    }

    private void setupSQLite() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl("jdbc:sqlite:" + sqliteFile);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");

        dataSource = new HikariDataSource(config);

        plugin.getLogger().info("SQLite connection established");
    }

    private void createTables() {
        String createTableSQL = useMySQL ?
                "CREATE TABLE IF NOT EXISTS economy_players (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "name VARCHAR(16) NOT NULL," +
                        "coins DOUBLE DEFAULT 0.0," +
                        "bank_balance DOUBLE DEFAULT 0.0," +
                        "last_seen BIGINT DEFAULT 0," +
                        "INDEX idx_name (name)," +
                        "INDEX idx_last_seen (last_seen)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
                :
                "CREATE TABLE IF NOT EXISTS economy_players (" +
                        "uuid TEXT PRIMARY KEY," +
                        "name TEXT NOT NULL," +
                        "coins REAL DEFAULT 0.0," +
                        "bank_balance REAL DEFAULT 0.0," +
                        "last_seen INTEGER DEFAULT 0" +
                        ");";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(createTableSQL);

            if (!useMySQL) {
                statement.execute("CREATE INDEX IF NOT EXISTS idx_name ON economy_players(name);");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_last_seen ON economy_players(last_seen);");
            }

            plugin.getLogger().info("Database tables created/verified");

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean shouldMigrateSQLiteData() {
        File sqliteFile = new File(this.sqliteFile);
        if (!sqliteFile.exists()) {
            return false;
        }

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM economy_players");
             ResultSet resultSet = statement.executeQuery()) {

            resultSet.next();
            int mysqlRecords = resultSet.getInt(1);

            return mysqlRecords == 0;

        } catch (SQLException e) {
            return false;
        }
    }

    private void migrateSQLiteToMySQL() {
        plugin.getLogger().info("Starting SQLite to MySQL migration...");

        try {
            HikariConfig sqliteConfig = new HikariConfig();
            sqliteConfig.setJdbcUrl("jdbc:sqlite:" + sqliteFile);
            sqliteConfig.setDriverClassName("org.sqlite.JDBC");
            sqliteConfig.setMaximumPoolSize(1);

            try (HikariDataSource sqliteSource = new HikariDataSource(sqliteConfig);
                 Connection sqliteConn = sqliteSource.getConnection();
                 Connection mysqlConn = getConnection();
                 PreparedStatement selectStmt = sqliteConn.prepareStatement("SELECT * FROM economy_players");
                 PreparedStatement insertStmt = mysqlConn.prepareStatement(
                         "INSERT INTO economy_players (uuid, name, coins, bank_balance, last_seen) VALUES (?, ?, ?, ?, ?)")) {

                mysqlConn.setAutoCommit(false);

                try (ResultSet resultSet = selectStmt.executeQuery()) {
                    int migrated = 0;

                    while (resultSet.next()) {
                        insertStmt.setString(1, resultSet.getString("uuid"));
                        insertStmt.setString(2, resultSet.getString("name"));
                        insertStmt.setDouble(3, resultSet.getDouble("coins"));
                        insertStmt.setDouble(4, resultSet.getDouble("bank_balance"));
                        insertStmt.setLong(5, resultSet.getLong("last_seen"));
                        insertStmt.addBatch();

                        migrated++;

                        if (migrated % 1000 == 0) {
                            insertStmt.executeBatch();
                            mysqlConn.commit();
                        }
                    }

                    insertStmt.executeBatch();
                    mysqlConn.commit();

                    plugin.getLogger().info("Migration completed! Migrated " + migrated + " players to MySQL");
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("Migration failed: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Migration setup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public CompletableFuture<EconomyPlayer> loadPlayer(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM economy_players WHERE uuid = ?")) {

                statement.setString(1, uuid.toString());

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return new EconomyPlayer(
                                uuid,
                                resultSet.getString("name"),
                                resultSet.getDouble("coins"),
                                resultSet.getDouble("bank_balance"),
                                resultSet.getLong("last_seen")
                        );
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player " + uuid + ": " + e.getMessage());
                e.printStackTrace();
            }

            double startingCoins = plugin.getConfig().getDouble("economy.starting-coins", 100.0);
            double startingBank = plugin.getConfig().getDouble("economy.starting-bank-balance", 0.0);
            return new EconomyPlayer(uuid, name, startingCoins, startingBank, System.currentTimeMillis());
        });
    }

    public CompletableFuture<Boolean> savePlayer(EconomyPlayer player) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO economy_players (uuid, name, coins, bank_balance, last_seen) " +
                                 "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                                 "name = VALUES(name), coins = VALUES(coins), bank_balance = VALUES(bank_balance), last_seen = VALUES(last_seen)")) {

                statement.setString(1, player.getUuid().toString());
                statement.setString(2, player.getName());
                statement.setDouble(3, player.getCoins());
                statement.setDouble(4, player.getBankBalance());
                statement.setLong(5, player.getLastSeen());

                int result = statement.executeUpdate();
                player.setModified(false);

                return result > 0;

            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save player " + player.getUuid() + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<List<EconomyPlayer>> getTopPlayers(int limit, boolean byCoins) {
        return CompletableFuture.supplyAsync(() -> {
            List<EconomyPlayer> topPlayers = new ArrayList<>();

            String orderBy = byCoins ? "coins" : "bank_balance";

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM economy_players ORDER BY " + orderBy + " DESC LIMIT ?")) {

                statement.setInt(1, limit);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        topPlayers.add(new EconomyPlayer(
                                UUID.fromString(resultSet.getString("uuid")),
                                resultSet.getString("name"),
                                resultSet.getDouble("coins"),
                                resultSet.getDouble("bank_balance"),
                                resultSet.getLong("last_seen")
                        ));
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to get top players: " + e.getMessage());
                e.printStackTrace();
            }

            return topPlayers;
        });
    }

    public CompletableFuture<EconomyPlayer> findPlayerByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT * FROM economy_players WHERE name = ? LIMIT 1")) {

                statement.setString(1, name);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return new EconomyPlayer(
                                UUID.fromString(resultSet.getString("uuid")),
                                resultSet.getString("name"),
                                resultSet.getDouble("coins"),
                                resultSet.getDouble("bank_balance"),
                                resultSet.getLong("last_seen")
                        );
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to find player by name " + name + ": " + e.getMessage());
                e.printStackTrace();
            }

            return null;
        });
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection closed");
        }
    }
}