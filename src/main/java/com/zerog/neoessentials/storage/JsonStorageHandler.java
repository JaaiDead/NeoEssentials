package com.zerog.neoessentials.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.KitManager;

import net.minecraft.core.BlockPos;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Storage handler that saves data to JSON files
 */
public class JsonStorageHandler implements StorageHandler {
    private static final String BASE_DIR = "neoessentials";
    private static final String ECONOMY_DIR = BASE_DIR + "/economy";
    private static final String KITS_FILE = BASE_DIR + "/kits.json";
    private static final String SPAWN_FILE = BASE_DIR + "/spawn.json";
    
    private final Gson gson;
    
    public JsonStorageHandler() {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }
    
    @Override
    public void initialize() {
        // Create directories if they don't exist
        createDirectory(BASE_DIR);
        createDirectory(ECONOMY_DIR);
        
        NeoEssentials.LOGGER.info("Initialized JSON storage handler");
    }
    
    @Override
    public void shutdown() {
        // Nothing to do here for JSON storage
        NeoEssentials.LOGGER.info("JSON storage handler shut down");
    }
    
    private void createDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) {
            NeoEssentials.LOGGER.error("Failed to create directory: {}", path);
        }
    }

    
    @Override
    public boolean saveKits(Map<String, KitManager.Kit> kits, Map<UUID, Map<String, Long>> cooldowns) {
        try {
            File kitFile = new File(KITS_FILE);
            
            // Create parent directories if they don't exist
            File parentDir = kitFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                NeoEssentials.LOGGER.error("Failed to create directory for kit data: {}", parentDir);
                return false;
            }
            
            JsonObject kitsData = new JsonObject();
            
            // Save kits
            JsonObject kitsObj = new JsonObject();
            for (Map.Entry<String, KitManager.Kit> entry : kits.entrySet()) {
                KitManager.Kit kit = entry.getValue();
                JsonObject kitObj = new JsonObject();
                
                // Save cooldown
                kitObj.addProperty("cooldown", kit.getCooldown());
                
                // Save permission
                if (kit.getPermission() != null) {
                    kitObj.addProperty("permission", kit.getPermission());
                }
                
                // Save items
                JsonArray itemsArray = new JsonArray();
                for (KitManager.ItemDefinition itemDef : kit.getItemDefinitions()) {
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("id", itemDef.getItemId());
                    itemObj.addProperty("count", itemDef.getCount());
                    
                    itemsArray.add(itemObj);
                }
                kitObj.add("items", itemsArray);
                
                kitsObj.add(entry.getKey(), kitObj);
            }
            kitsData.add("kits", kitsObj);
            
            // Save cooldowns
            JsonObject cooldownsObj = new JsonObject();
            for (Map.Entry<UUID, Map<String, Long>> entry : cooldowns.entrySet()) {
                UUID uuid = entry.getKey();
                Map<String, Long> playerCooldowns = entry.getValue();
                
                JsonObject playerCooldownsObj = new JsonObject();
                for (Map.Entry<String, Long> cooldownEntry : playerCooldowns.entrySet()) {
                    playerCooldownsObj.addProperty(cooldownEntry.getKey(), cooldownEntry.getValue());
                }
                
                cooldownsObj.add(uuid.toString(), playerCooldownsObj);
            }
            kitsData.add("cooldowns", cooldownsObj);
            
            // Save to file
            try (FileWriter writer = new FileWriter(kitFile)) {
                gson.toJson(kitsData, writer);
            }
            
            return true;
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Error saving kit data", e);
            return false;
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<Object> loadKits() {
        try {
            File kitFile = new File(KITS_FILE);
            Map<String, KitManager.Kit> kits = new HashMap<>();
            Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
            
            if (kitFile.exists()) {
                JsonObject kitsData = null;
                
                try (FileReader reader = new FileReader(kitFile)) {
                    kitsData = JsonParser.parseReader(reader).getAsJsonObject();
                }
                
                if (kitsData != null) {
                    // Load kits
                    if (kitsData.has("kits")) {
                        JsonObject kitsObj = kitsData.getAsJsonObject("kits");
                        for (Map.Entry<String, JsonElement> entry : kitsObj.entrySet()) {
                            String kitName = entry.getKey();
                            JsonObject kitObj = entry.getValue().getAsJsonObject();
                            
                            // Create the kit
                            KitManager.Kit kit = new KitManager.Kit(kitName);
                            
                            // Set cooldown
                            if (kitObj.has("cooldown")) {
                                kit.setCooldown(kitObj.get("cooldown").getAsLong());
                            }
                            
                            // Set permission
                            if (kitObj.has("permission")) {
                                kit.setPermission(kitObj.get("permission").getAsString());
                            }
                            
                            // Load items (store item IDs and counts)
                            if (kitObj.has("items")) {
                                JsonArray itemsArray = kitObj.getAsJsonArray("items");
                                for (JsonElement itemElement : itemsArray) {
                                    JsonObject itemObj = itemElement.getAsJsonObject();
                                    
                                    String itemId = itemObj.get("id").getAsString();
                                    int count = itemObj.get("count").getAsInt();
                                    
                                    // Add a placeholder item definition
                                    kit.addItemDefinition(itemId, count);
                                }
                            }
                            
                            // Add the kit
                            kits.put(kitName.toLowerCase(), kit);
                        }
                    }
                    
                    // Load cooldowns
                    if (kitsData.has("cooldowns")) {
                        JsonObject cooldownsObj = kitsData.getAsJsonObject("cooldowns");
                        
                        for (Map.Entry<String, JsonElement> entry : cooldownsObj.entrySet()) {
                            String uuidString = entry.getKey();
                            UUID uuid = UUID.fromString(uuidString);
                            JsonObject playerCooldowns = entry.getValue().getAsJsonObject();
                            
                            // Create cooldown map for player
                            Map<String, Long> playerCooldownMap = new HashMap<>();
                            
                            for (Map.Entry<String, JsonElement> cooldownEntry : playerCooldowns.entrySet()) {
                                String kitName = cooldownEntry.getKey();
                                long lastUse = cooldownEntry.getValue().getAsLong();
                                playerCooldownMap.put(kitName.toLowerCase(), lastUse);
                            }
                            
                            cooldowns.put(uuid, playerCooldownMap);
                        }
                    }
                }
            }
            
            List<Object> result = new ArrayList<>();
            result.add(kits);
            result.add(cooldowns);
            return result;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error loading kit data", e);
            return null;
        }
    }
    
    @Override
    public boolean saveSpawnData(Map<String, Object> spawn) {
        try {
            File file = new File(SPAWN_FILE);
            JsonObject rootObj = new JsonObject();
            
            // Add spawn data
            if (spawn.containsKey("dimension")) {
                rootObj.addProperty("dimension", (String) spawn.get("dimension"));
            }
            
            if (spawn.containsKey("position")) {
                BlockPos pos = (BlockPos) spawn.get("position");
                JsonObject posObj = new JsonObject();
                posObj.addProperty("x", pos.getX());
                posObj.addProperty("y", pos.getY());
                posObj.addProperty("z", pos.getZ());
                rootObj.add("position", posObj);
            }
            
            if (spawn.containsKey("pitch")) {
                rootObj.addProperty("pitch", (Float) spawn.get("pitch"));
            }
            
            if (spawn.containsKey("yaw")) {
                rootObj.addProperty("yaw", (Float) spawn.get("yaw"));
            }
            
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(rootObj, writer);
            }
            
            return true;
        } catch (IOException e) {
            NeoEssentials.LOGGER.error("Failed to save spawn data: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public Map<String, Object> loadSpawnData() {
        Map<String, Object> spawn = new HashMap<>();
        
        try {
            File file = new File(SPAWN_FILE);
            
            if (!file.exists()) {
                return spawn;
            }
            
            JsonObject rootObj;
            try (FileReader reader = new FileReader(file)) {
                rootObj = JsonParser.parseReader(reader).getAsJsonObject();
            }
            
            // Load dimension
            if (rootObj.has("dimension")) {
                spawn.put("dimension", rootObj.get("dimension").getAsString());
            }
            
            // Load position
            if (rootObj.has("position")) {
                JsonObject posObj = rootObj.getAsJsonObject("position");
                int x = posObj.get("x").getAsInt();
                int y = posObj.get("y").getAsInt();
                int z = posObj.get("z").getAsInt();
                spawn.put("position", new BlockPos(x, y, z));
            }
            
            // Load rotation
            if (rootObj.has("pitch")) {
                spawn.put("pitch", rootObj.get("pitch").getAsFloat());
            }
            
            if (rootObj.has("yaw")) {
                spawn.put("yaw", rootObj.get("yaw").getAsFloat());
            }
            
            return spawn;
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to load spawn data: {}", e.getMessage());
            return spawn;
        }
    }
}
