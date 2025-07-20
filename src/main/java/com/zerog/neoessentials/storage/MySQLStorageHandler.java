package com.zerog.neoessentials.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.DatabaseConfig;
import com.zerog.neoessentials.data.KitManager;

import net.minecraft.core.BlockPos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Storage handler that saves data to a MySQL database
 */
public class MySQLStorageHandler implements StorageHandler {
    private Connection connection;
    private final Gson gson;
    private final DatabaseConfig config;
    private final String tablePrefix;
    
    public MySQLStorageHandler(DatabaseConfig config) {
        this.config = config;
        this.tablePrefix = config.getTablePrefix();
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }
    
    @Override
    public void initialize() {
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Build connection URL
            String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&autoReconnect=true",
                    config.getHost(),
                    config.getPort(),
                    config.getDatabase(),
                    config.isUseSsl());
            
            // Create connection
            connection = DriverManager.getConnection(url, config.getUsername(), config.getPassword());
            
            // Create tables
            createTables();
            
            NeoEssentials.LOGGER.info("Initialized MySQL storage handler");
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to initialize MySQL storage handler: {}", e.getMessage());
            connection = null;
        }
    }
    
    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
            NeoEssentials.LOGGER.info("MySQL storage handler shut down");
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to close MySQL connection: {}", e.getMessage());
        }
    }
    
    private void createTables() {
        if (connection == null) {
            return;
        }
        
        try (Statement stmt = connection.createStatement()) {
            // Create homes table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "homes` (" +
                "`uuid` VARCHAR(36) NOT NULL, " +
                "`home_name` VARCHAR(64) NOT NULL, " +
                "`dimension` VARCHAR(64) NOT NULL, " +
                "`x` INT NOT NULL, " +
                "`y` INT NOT NULL, " +
                "`z` INT NOT NULL, " +
                "`pitch` FLOAT NOT NULL, " +
                "`yaw` FLOAT NOT NULL, " +
                "PRIMARY KEY (`uuid`, `home_name`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            
            // Create warps table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "warps` (" +
                "`name` VARCHAR(64) PRIMARY KEY, " +
                "`dimension` VARCHAR(64) NOT NULL, " +
                "`x` INT NOT NULL, " +
                "`y` INT NOT NULL, " +
                "`z` INT NOT NULL, " +
                "`pitch` FLOAT NOT NULL, " +
                "`yaw` FLOAT NOT NULL, " +
                "`permission` VARCHAR(128)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            
            // Create economy table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "economy` (" +
                "`uuid` VARCHAR(36) PRIMARY KEY, " +
                "`balance` VARCHAR(50) NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            
            // Create transactions table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "transactions` (" +
                "`id` INT AUTO_INCREMENT PRIMARY KEY, " +
                "`uuid` VARCHAR(36) NOT NULL, " +
                "`description` VARCHAR(255) NOT NULL, " +
                "`amount` VARCHAR(50) NOT NULL, " +
                "`timestamp` BIGINT NOT NULL, " +
                "INDEX (`uuid`), " +
                "FOREIGN KEY (`uuid`) REFERENCES `" + tablePrefix + "economy`(`uuid`) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            
            // Create kits table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "kits` (" +
                "`name` VARCHAR(64) PRIMARY KEY, " +
                "`cooldown` BIGINT NOT NULL, " +
                "`permission` VARCHAR(128), " +
                "`items` LONGTEXT NOT NULL" +  // JSON array of items
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            
            // Create kit cooldowns table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "kit_cooldowns` (" +
                "`uuid` VARCHAR(36) NOT NULL, " +
                "`kit_name` VARCHAR(64) NOT NULL, " +
                "`last_use` BIGINT NOT NULL, " +
                "PRIMARY KEY (`uuid`, `kit_name`), " +
                "FOREIGN KEY (`kit_name`) REFERENCES `" + tablePrefix + "kits`(`name`) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            
            // Create spawn table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "spawn` (" +
                "`id` INT PRIMARY KEY CHECK (id = 1), " +  // Ensure only one spawn record
                "`dimension` VARCHAR(64) NOT NULL, " +
                "`x` INT NOT NULL, " +
                "`y` INT NOT NULL, " +
                "`z` INT NOT NULL, " +
                "`pitch` FLOAT NOT NULL, " +
                "`yaw` FLOAT NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to create MySQL tables: {}", e.getMessage());
        }
    }
    

    
    @Override
    public boolean saveKits(Map<String, KitManager.Kit> kits, Map<UUID, Map<String, Long>> cooldowns) {
        if (connection == null) {
            return false;
        }
        
        try {
            // Start transaction
            connection.setAutoCommit(false);
            
            // Clear existing kits and cooldowns
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM `" + tablePrefix + "kit_cooldowns`");
                stmt.executeUpdate("DELETE FROM `" + tablePrefix + "kits`");
            }
            
            // Insert kits
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO `" + tablePrefix + "kits` (name, cooldown, permission, items) VALUES (?, ?, ?, ?)")) {
                
                for (Map.Entry<String, KitManager.Kit> entry : kits.entrySet()) {
                    KitManager.Kit kit = entry.getValue();
                    
                    // Convert items to JSON
                    JsonArray itemsArray = new JsonArray();
                    for (KitManager.ItemDefinition itemDef : kit.getItemDefinitions()) {
                        JsonObject itemObj = new JsonObject();
                        itemObj.addProperty("id", itemDef.getItemId());
                        itemObj.addProperty("count", itemDef.getCount());
                        itemsArray.add(itemObj);
                    }
                    
                    stmt.setString(1, kit.getName());
                    stmt.setLong(2, kit.getCooldown());
                    stmt.setString(3, kit.getPermission());
                    stmt.setString(4, itemsArray.toString());
                    
                    stmt.executeUpdate();
                }
            }
            
            // Insert cooldowns
            if (!cooldowns.isEmpty()) {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO `" + tablePrefix + "kit_cooldowns` (uuid, kit_name, last_use) VALUES (?, ?, ?)")) {
                    
                    for (Map.Entry<UUID, Map<String, Long>> entry : cooldowns.entrySet()) {
                        UUID uuid = entry.getKey();
                        Map<String, Long> playerCooldowns = entry.getValue();
                        
                        for (Map.Entry<String, Long> cooldownEntry : playerCooldowns.entrySet()) {
                            String kitName = cooldownEntry.getKey();
                            long lastUse = cooldownEntry.getValue();
                            
                            stmt.setString(1, uuid.toString());
                            stmt.setString(2, kitName);
                            stmt.setLong(3, lastUse);
                            
                            stmt.addBatch();
                        }
                    }
                    
                    stmt.executeBatch();
                }
            }
            
            // Commit transaction
            connection.commit();
            connection.setAutoCommit(true);
            
            return true;
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                NeoEssentials.LOGGER.error("Failed to rollback transaction: {}", ex.getMessage());
            }
            
            NeoEssentials.LOGGER.error("Failed to save kits: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<Object> loadKits() {
        Map<String, KitManager.Kit> kits = new HashMap<>();
        Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
        
        if (connection == null) {
            List<Object> result = new ArrayList<>();
            result.add(kits);
            result.add(cooldowns);
            return result;
        }
        
        try {
            // Load kits
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT name, cooldown, permission, items FROM `" + tablePrefix + "kits`");
                
                while (rs.next()) {
                    String name = rs.getString("name");
                    long cooldown = rs.getLong("cooldown");
                    String permission = rs.getString("permission");
                    String itemsJson = rs.getString("items");
                    
                    KitManager.Kit kit = new KitManager.Kit(name);
                    kit.setCooldown(cooldown);
                    kit.setPermission(permission);
                    
                    // Parse items from JSON
                    try {
                        JsonArray itemsArray = gson.fromJson(itemsJson, JsonArray.class);
                        for (int i = 0; i < itemsArray.size(); i++) {
                            JsonObject itemObj = itemsArray.get(i).getAsJsonObject();
                            String id = itemObj.get("id").getAsString();
                            int count = itemObj.get("count").getAsInt();
                            kit.addItemDefinition(id, count);
                        }
                    } catch (Exception e) {
                        NeoEssentials.LOGGER.error("Failed to parse items for kit {}: {}", name, e.getMessage());
                    }
                    
                    kits.put(name.toLowerCase(), kit);
                }
            }
            
            // Load cooldowns
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT uuid, kit_name, last_use FROM `" + tablePrefix + "kit_cooldowns`");
                
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String kitName = rs.getString("kit_name");
                    long lastUse = rs.getLong("last_use");
                    
                    Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(uuid, k -> new HashMap<>());
                    playerCooldowns.put(kitName.toLowerCase(), lastUse);
                }
            }
            
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load kits: {}", e.getMessage());
        }
        
        List<Object> result = new ArrayList<>();
        result.add(kits);
        result.add(cooldowns);
        return result;
    }
    
    @Override
    public boolean saveSpawnData(Map<String, Object> spawn) {
        if (connection == null || !spawn.containsKey("dimension") || !spawn.containsKey("position")) {
            return false;
        }
        
        try {
            String dimension = (String) spawn.get("dimension");
            BlockPos pos = (BlockPos) spawn.get("position");
            float pitch = spawn.containsKey("pitch") ? (Float) spawn.get("pitch") : 0.0f;
            float yaw = spawn.containsKey("yaw") ? (Float) spawn.get("yaw") : 0.0f;
            
            // Delete existing spawn
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("DELETE FROM `" + tablePrefix + "spawn`");
            }
            
            // Insert new spawn
            try (PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO `" + tablePrefix + "spawn` (id, dimension, x, y, z, pitch, yaw) VALUES (1, ?, ?, ?, ?, ?, ?)")) {
                
                stmt.setString(1, dimension);
                stmt.setInt(2, pos.getX());
                stmt.setInt(3, pos.getY());
                stmt.setInt(4, pos.getZ());
                stmt.setFloat(5, pitch);
                stmt.setFloat(6, yaw);
                
                stmt.executeUpdate();
            }
            
            return true;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to save spawn data: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public Map<String, Object> loadSpawnData() {
        Map<String, Object> spawn = new HashMap<>();
        
        if (connection == null) {
            return spawn;
        }
        
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT dimension, x, y, z, pitch, yaw FROM `" + tablePrefix + "spawn` WHERE id = 1");
            
            if (rs.next()) {
                String dimension = rs.getString("dimension");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                float pitch = rs.getFloat("pitch");
                float yaw = rs.getFloat("yaw");
                
                spawn.put("dimension", dimension);
                spawn.put("position", new BlockPos(x, y, z));
                spawn.put("pitch", pitch);
                spawn.put("yaw", yaw);
            }
            
            return spawn;
        } catch (SQLException e) {
            NeoEssentials.LOGGER.error("Failed to load spawn data: {}", e.getMessage());
            return spawn;
        }
    }
}
