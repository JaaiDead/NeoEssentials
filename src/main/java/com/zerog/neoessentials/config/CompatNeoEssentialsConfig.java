package com.zerog.neoessentials.config;

import com.zerog.neoessentials.NeoEssentials;
import java.util.HashMap;
import java.util.Map;

/**
 * Compatibility layer that adapts our TOML configs to the old config structure.
 * This allows existing code to continue working with the new config system.
 * 
 * This class uses lazy loading for config values - they are fetched directly
 * from the TOML configs when needed rather than being initialized at startup.
 */
public class CompatNeoEssentialsConfig {
    // Command settings - cache for performance
    private final Map<String, Boolean> commandsEnabled = new HashMap<>();
    
    // Permission settings - cache for performance
    private final Map<String, Boolean> defaultPermissions = new HashMap<>();
    
    /**
     * Constructor - doesn't load any config values
     */
    public CompatNeoEssentialsConfig() {
        // Empty constructor - no initialization of config values
    }    /**
     * Initializes default values from configs after configs are loaded.
     * This should only be called by ModConfigManager after all configs are loaded.
     */
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing compatibility config layer");
        
        // Initialize default permissions if needed
        // We don't need to pre-populate other values since we use lazy loading now
        try {

              if (ConfigUtil.isConfigAvailable(GeneralConfig.ENABLE_KITS)) {
                commandsEnabled.put("kit", GeneralConfig.ENABLE_KITS.get());
                NeoEssentials.LOGGER.debug("Pre-cached kit command state: " + commandsEnabled.get("kit"));
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.warn("Error during compatibility config initialization", e);
            // If we get here, configs are still not loaded or another issue occurred
            // This is fine - we'll just use lazy loading for everything
        }
        
        // Initialize default permissions map for commonly used permissions
        try {
            defaultPermissions.put("neoessentials.command.spawn", true);
            defaultPermissions.put("neoessentials.command.kit", true);
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error initializing default permissions", e);
        }
    }
      /**
     * Gets whether debug mode is enabled
     * @return True if debug mode is enabled
     */
    public boolean isDebug() {
        return ConfigUtil.getConfigSafe(GeneralConfig.DEBUG_MODE, false);
    }
    
    /**
     * Gets the default language code
     * @return The language code (e.g. "en_us")
     */
    public String getDefaultLanguage() {
        return "en_us";
    }
      /**
     * Gets whether economy is enabled
     * @return True if economy is enabled
     */
    public boolean isEconomyEnabled() {
        return ConfigUtil.getConfigSafe(GeneralConfig.ENABLE_ECONOMY, true);
    }

    /**
     * Gets the singular name of the currency
     * @return The currency name (e.g. "Dollar")
     */
    public String getCurrencyNameSingular() {
        return "Dollar"; // Default value since economy system is removed
    }
    
    /**
     * Gets the plural name of the currency
     * @return The currency name (e.g. "Dollars")
     */
    public String getCurrencyNamePlural() {
        return "Dollars"; // Default value since economy system is removed
    }
    
    /**
     * Gets the currency symbol
     * @return The currency symbol (e.g. "$")
     */
    public String getCurrencySymbol() {
        return "$"; // Default value since economy system is removed
    }

    /**
     * Gets the starting balance for new players
     * @return The starting balance
     */
    public double getStartingBalance() {
        return 100.0; // Default value since economy system is removed
    }


      /**
     * Gets whether a command is enabled
     * @param command The command name
     * @return True if the command is enabled
     */
    public boolean isCommandEnabled(String command) {
        if (commandsEnabled.containsKey(command)) {
            return commandsEnabled.get(command);
        }
        
        boolean enabled = true;
        String commandLower = command.toLowerCase();
        
        switch (commandLower) {
            case "kit":
                enabled = ConfigUtil.getConfigSafe(GeneralConfig.ENABLE_KITS, true);
                break;
            default:
                enabled = true;
        }
        
        // Cache the result
        commandsEnabled.put(commandLower, enabled);
        return enabled;
    }
    
    /**
     * Gets the map of default permissions
     * @return Map of permission names to boolean values
     */
    public Map<String, Boolean> defaultPermissions() {
        return defaultPermissions;
    }
}
