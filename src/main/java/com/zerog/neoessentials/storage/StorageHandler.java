package com.zerog.neoessentials.storage;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.zerog.neoessentials.data.KitManager.Kit;

/**
 * Interface for storage handlers
 */
public interface StorageHandler {
    
    /**
     * Initializes the storage handler
     */
    void initialize();
    
    /**
     * Shuts down the storage handler
     */
    void shutdown();
    

    /**
     * Saves all kits
     * 
     * @param kits The kits to save
     * @param cooldowns The kit cooldowns
     * @return True if successful, false otherwise
     */
    boolean saveKits(Map<String, Kit> kits, Map<UUID, Map<String, Long>> cooldowns);
    
    /**
     * Loads all kits
     * 
     * @return A list containing the kits and cooldowns, or null if an error occurs
     */
    List<Object> loadKits();
    
    /**
     * Saves all spawn data
     * 
     * @param spawn The spawn data
     * @return True if successful, false otherwise
     */
    boolean saveSpawnData(Map<String, Object> spawn);
    
    /**
     * Loads all spawn data
     * 
     * @return The spawn data, or null if an error occurs
     */
    Map<String, Object> loadSpawnData();
}
