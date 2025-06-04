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
    private volatile boolean initialized = false;

    public DatabaseManager(SlownEconomy plugin) {
        this.plugin = plugin;
        this.useMySQL = plugin.getConfig().getBoolean("database.mysql.enabled", false);
        this.sqliteFile = plugin.getDataFolder() + File.separator + "economy.db";
    }

    public void initialize() {
        try {
            if (useMySQL) {
                setupMySQL();
            } else {
                setupSQLite();
            }

            if (!testConnection()) {
                throw new RuntimeException("Database connection test failed");
            }

            createTables();

            if (useMySQL && shouldMigrateSQLiteData()) {
                migrateSQLiteToMySQL();
            }

            initialized = true;
            plugin.getLogger().info("Database initialized successfully");

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    private boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            plugin.getLogger().severe("Database connection test failed: " + e.getMessage());
            return false;
        }
    }

    private void setupMySQL() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL driver not found", e);
        }

        HikariConfig config = new HikariConfig();

        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "economy");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "");

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true");
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setMaxLifetime(1800000);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setLeakDetectionThreshold(60000);
        config.setValidationTimeout(5000);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");

        dataSource = new HikariDataSource(config);
        plugin.getLogger().info("MySQL connection pool established");
    }

    private void setupSQLite() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite driver not found", e);
        }

        File dbFile = new File(sqliteFile);
        if (!dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + sqliteFile);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");
        config.setConnectionTimeout(30000);

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
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"
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
                statement.execute("PRAGMA journal_mode=WAL;");
                statement.execute("PRAGMA synchronous=NORMAL;");
                statement.execute("PRAGMA cache_size=10000;");
            }

            plugin.getLogger().info("Database tables created/verified");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create tables: " + e.getMessage(), e);
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

            if (mysqlRecords > 0) {
                plugin.getLogger().info("MySQL database already contains data, skipping migration");
                return false;
            }

            try (Connection sqliteConn = DriverManager.getConnection("jdbc:sqlite:" + this.sqliteFile);
                 PreparedStatement sqliteStmt = sqliteConn.prepareStatement("SELECT COUNT(*) FROM economy_players");
                 ResultSet sqliteResult = sqliteStmt.executeQuery()) {

                sqliteResult.next();
                int sqliteRecords = sqliteResult.getInt(1);

                if (sqliteRecords > 0) {
                    plugin.getLogger().info("Found " + sqliteRecords + " records in SQLite, migration needed");
                    return true;
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Could not check migration status: " + e.getMessage());
        }

        return false;
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
                 Connection mysqlConn = getConnection()) {

                mysqlConn.setAutoCommit(false);

                String selectSQL = "SELECT uuid, name, coins, bank_balance, last_seen FROM economy_players";
                String insertSQL = "INSERT INTO economy_players (uuid, name, coins, bank_balance, last_seen) VALUES (?, ?, ?, ?, ?)";

                try (PreparedStatement selectStmt = sqliteConn.prepareStatement(selectSQL);
                     PreparedStatement insertStmt = mysqlConn.prepareStatement(insertSQL);
                     ResultSet resultSet = selectStmt.executeQuery()) {

                    int migrated = 0;
                    int batchSize = 1000;

                    while (resultSet.next()) {
                        insertStmt.setString(1, resultSet.getString("uuid"));
                        insertStmt.setString(2, resultSet.getString("name"));
                        insertStmt.setDouble(3, resultSet.getDouble("coins"));
                        insertStmt.setDouble(4, resultSet.getDouble("bank_balance"));
                        insertStmt.setLong(5, resultSet.getLong("last_seen"));
                        insertStmt.addBatch();

                        migrated++;

                        if (migrated % batchSize == 0) {
                            insertStmt.executeBatch();
                            mysqlConn.commit();
                            plugin.getLogger().info("Migrated " + migrated + " players...");
                        }
                    }

                    insertStmt.executeBatch();
                    mysqlConn.commit();

                    plugin.getLogger().info("Migration completed successfully! Migrated " + migrated + " players to MySQL");

                } catch (SQLException e) {
                    mysqlConn.rollback();
                    throw e;
                }

            }

        } catch (Exception e) {
            plugin.getLogger().severe("Migration failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Migration failed", e);
        }
    }

    public CompletableFuture<EconomyPlayer> loadPlayer(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) {
                throw new RuntimeException("Database not initialized");
            }

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT uuid, name, coins, bank_balance, last_seen FROM economy_players WHERE uuid = ?")) {

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
                throw new RuntimeException("Database error", e);
            }

            double startingCoins = plugin.getConfig().getDouble("economy.starting-coins", 100.0);
            double startingBank = plugin.getConfig().getDouble("economy.starting-bank-balance", 0.0);
            return new EconomyPlayer(uuid, name, startingCoins, startingBank, System.currentTimeMillis());
        });
    }

    public CompletableFuture<Boolean> savePlayer(EconomyPlayer player) {
        return CompletableFuture.supplyAsync(() -> {
            if (!initialized) {
                throw new RuntimeException("Database not initialized");
            }

            String sql = useMySQL ?
                    "INSERT INTO economy_players (uuid, name, coins, bank_balance, last_seen) " +
                            "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                            "name = VALUES(name), coins = VALUES(coins), bank_balance = VALUES(bank_balance), last_seen = VALUES(last_seen)"
                    :
                    "INSERT OR REPLACE INTO economy_players (uuid, name, coins, bank_balance, last_seen) VALUES (?, ?, ?, ?, ?)";

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, player.getUuid().toString());
                statement.setString(2, player.getName());
                statement.setDouble(3, player.getCoins());
                statement.setDouble(4, player.getBankBalance());
                statement.setLong(5, player.getLastSeen());

                int result = statement.executeUpdate();
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
            if (!initialized) {
                throw new RuntimeException("Database not initialized");
            }

            List<EconomyPlayer> topPlayers = new ArrayList<>();
            String orderBy = byCoins ? "coins" : "bank_balance";

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT uuid, name, coins, bank_balance, last_seen FROM economy_players ORDER BY " + orderBy + " DESC LIMIT ?")) {

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
            if (!initialized) {
                throw new RuntimeException("Database not initialized");
            }

            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "SELECT uuid, name, coins, bank_balance, last_seen FROM economy_players WHERE name = ? LIMIT 1")) {

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
        if (!initialized || dataSource == null) {
            throw new SQLException("Database not initialized");
        }

        Connection connection = dataSource.getConnection();
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Could not obtain database connection");
        }

        return connection;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
                initialized = false;
                plugin.getLogger().info("Database connection pool closed");
            } catch (Exception e) {
                plugin.getLogger().warning("Error closing database: " + e.getMessage());
            }
        }
    }
}