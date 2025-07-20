package com.zerog.neoessentials.storage;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.config.DatabaseConfig;
import com.zerog.neoessentials.config.StorageType;
import com.zerog.neoessentials.data.KitManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manager for storage handlers
 */
public class StorageManager {
    private StorageHandler storageHandler;
    private final DatabaseConfig config;
    
    public StorageManager(DatabaseConfig config) {
        this.config = config;
    }
      /**
     * Initialize the storage manager
     * 
     * @return True if initialization was successful, false otherwise
     */
    public boolean initialize() {
        NeoEssentials.LOGGER.info("Initializing storage manager with {} storage", config.storageType.get());
        
        // Create the storage handler based on config
        try {
            storageHandler = StorageFactory.createStorageHandler(config);
            
            if (storageHandler == null) {
                NeoEssentials.LOGGER.error("Failed to create storage handler for type {}", config.storageType.get());
                return false;
            }
            
            // Initialize the storage handler
            storageHandler.initialize();
            
            NeoEssentials.LOGGER.info("Storage manager successfully initialized with {} storage", config.storageType.get());
            return true;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error initializing storage manager: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Shutdown the storage manager
     */
    public void shutdown() {
        if (storageHandler != null) {
            storageHandler.shutdown();
            storageHandler = null;
        }
    }
    
    /**
     * Get the current storage type
     * 
     * @return The storage type
     */
    public StorageType getStorageType() {
        return config.storageType.get();
    }
    
    /**
     * Get the current storage handler
     * 
     * @return The storage handler
     */
    public StorageHandler getStorageHandler() {
        return storageHandler;
    }


    /**
     * Save all kits
     * 
     * @param kits The kits to save
     * @param cooldowns The kit cooldowns
     * @return True if successful, false otherwise
     */
    public boolean saveKits(Map<String, KitManager.Kit> kits, Map<UUID, Map<String, Long>> cooldowns) {
        if (storageHandler != null) {
            return storageHandler.saveKits(kits, cooldowns);
        }
        return false;
    }
    
    /**
     * Load all kits
     * 
     * @return A list containing the kits and cooldowns, or null if an error occurs
     */
    public List<Object> loadKits() {
        if (storageHandler != null) {
            return storageHandler.loadKits();
        }
        return null;
    }
    
    /**
     * Save all spawn data
     * 
     * @param spawn The spawn data
     * @return True if successful, false otherwise
     */
    public boolean saveSpawnData(Map<String, Object> spawn) {
        if (storageHandler != null) {
            return storageHandler.saveSpawnData(spawn);
        }
        return false;
    }
    
    /**
     * Load all spawn data
     * 
     * @return The spawn data, or an empty map if an error occurs
     */
    public Map<String, Object> loadSpawnData() {
        if (storageHandler != null) {
            return storageHandler.loadSpawnData();
        }
        return Map.of();
    }
}
