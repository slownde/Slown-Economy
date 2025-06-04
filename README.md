# Slown Economy API

Private economy system for the Slown Network. This documentation covers API usage for internal development.

## Features

- **Dual Currency System**: Players have both coins (for immediate use) and bank balance (for storage)
- **Bank Transfers Only**: Player-to-player transfers work exclusively with bank balances
- **Real-time GUI Updates**: All GUIs automatically refresh when economy events occur
- **Transfer Fees**: Configurable percentage-based fees on transfers
- **Limits & Validation**: Configurable min/max amounts with automatic validation
- **MySQL/SQLite Support**: Automatic migration from SQLite to MySQL
- **Caching System**: High-performance player data caching with auto-cleanup
- **Event System**: Comprehensive events for all economy actions

## Quick Start

```java
// Get API instance
EconomyAPI api = SlownEconomy.getAPI();

// Basic coin operations
double coins = api.getCoins(player);
api.addCoins(player, 100.0);
api.removeCoins(player, 50.0);

// Bank operations
double bankBalance = api.getBankBalance(player);
api.depositToBank(player, 100.0);
api.withdrawFromBank(player, 50.0);

// Bank transfers (only way players can send money)
api.transferBankBalance(fromPlayer, toPlayer, 100.0);
```

## Core Methods

### Coins (Immediate Currency)
```java
// Synchronous (cached players only)
double getCoins(Player player)

// Asynchronous
CompletableFuture<Double> getCoins(UUID/OfflinePlayer)
CompletableFuture<Boolean> setCoins(UUID/OfflinePlayer, double)
CompletableFuture<Boolean> addCoins(UUID/OfflinePlayer, double)
CompletableFuture<Boolean> removeCoins(UUID/OfflinePlayer, double)
```

### Bank Balance (Storage Currency)
```java
// Synchronous (cached players only)
double getBankBalance(Player player)

// Asynchronous
CompletableFuture<Double> getBankBalance(UUID/OfflinePlayer)
CompletableFuture<Boolean> setBankBalance(UUID/OfflinePlayer, double)
CompletableFuture<Boolean> addBankBalance(UUID/OfflinePlayer, double)
CompletableFuture<Boolean> removeBankBalance(UUID/OfflinePlayer, double)
```

### Bank Operations
```java
CompletableFuture<Boolean> depositToBank(UUID/OfflinePlayer, double)
CompletableFuture<Boolean> withdrawFromBank(UUID/OfflinePlayer, double)
```

### Transfers
```java
// Legacy coin transfers (for admin/plugin use)
CompletableFuture<Boolean> transferCoins(UUID/OfflinePlayer from, UUID/OfflinePlayer to, double)

// Bank transfers (player-to-player)
CompletableFuture<Boolean> transferBankBalance(UUID/OfflinePlayer from, UUID/OfflinePlayer to, double)
```

## Events

Listen to economy changes in your plugins:

```java
@EventHandler
public void onCoinsChange(CoinsChangeEvent event) {
    EconomyPlayer player = event.getPlayer();
    double oldAmount = event.getOldAmount();
    double newAmount = event.getNewAmount();
    CoinsChangeEvent.Cause cause = event.getCause();
}

@EventHandler
public void onBankChange(BankChangeEvent event) {
    // Similar to CoinsChangeEvent
}

@EventHandler
public void onTransfer(CoinsTransferEvent event) {
    EconomyPlayer from = event.getFromPlayer();
    EconomyPlayer to = event.getToPlayer();
    double amount = event.getAmount();
    
    // Cancel transfer if needed
    event.setCancelled(true);
}

@EventHandler
public void onBankDeposit(BankDepositEvent event) {
    // Triggered on coin -> bank deposits
}

@EventHandler
public void onBankWithdraw(BankWithdrawEvent event) {
    // Triggered on bank -> coin withdrawals
}
```

## Player Commands

- `/coins` - Open coins GUI
- `/coins <player>` - Check player's coins
- `/bank` - Open bank GUI
- `/bank deposit <amount>` - Deposit coins to bank
- `/bank withdraw <amount>` - Withdraw from bank to coins
- `/transfer <player> <amount>` - Transfer bank balance to another player

## Admin Commands

```bash
# Coins management
/coins set <player> <amount>
/coins add <player> <amount>
/coins remove <player> <amount>

# Bank management
/bank set <player> <amount>
/bank add <player> <amount>
/bank remove <player> <amount>
```

## Configuration

```yaml
economy:
  starting-coins: 100.0
  starting-bank-balance: 0.0
  max-coins: 999999999.0
  max-bank-balance: 999999999.0
  
  transfer:
    enabled: true
    min-amount: 1.0
    max-amount: 100000.0
    fee-percentage: 0.0  # 0% = no fees, 5.0 = 5% fee

database:
  mysql:
    enabled: false  # Set to true for MySQL
    host: "localhost"
    port: 3306
    database: "slown_economy"
    username: "root"
    password: ""

cache:
  save-interval: 300    # seconds
  cleanup-interval: 600 # seconds
  max-age: 1800        # seconds
```

## Important Notes

- **All async methods return CompletableFuture** - handle them properly
- **Bank transfers only** - players can't directly transfer coins
- **Auto-validation** - API handles all limit checks automatically
- **Events are fired** - for all economy changes, including admin commands
- **Caching** - Online players are cached, offline players hit database
- **Thread-safe** - All operations are async and thread-safe

## Dependencies

Add to your `plugin.yml`:
```yaml
depend: [Slown-Economy]
```

## Support

Internal use only - contact devs for questions or issues.