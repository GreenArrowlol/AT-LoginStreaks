package com.loginstreaks.data;

import com.loginstreaks.LoginStreakRewards;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    
    private final LoginStreakRewards plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, PlayerData> playerDataCache;
    
    public DataManager(LoginStreakRewards plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        this.playerDataCache = new HashMap<>();
        loadData();
    }
    
    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        for (String key : dataConfig.getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            int streak = dataConfig.getInt(key + ".streak", 0);
            long lastLogin = dataConfig.getLong(key + ".lastLogin", 0);
            long nextClaim = dataConfig.getLong(key + ".nextClaim", 0);
            playerDataCache.put(uuid, new PlayerData(streak, lastLogin, nextClaim));
        }
    }
    
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataCache.computeIfAbsent(uuid, k -> new PlayerData(0, 0, 0));
    }
    
    public void setPlayerData(UUID uuid, PlayerData data) {
        playerDataCache.put(uuid, data);
    }
    
    public void saveAll() {
        for (Map.Entry<UUID, PlayerData> entry : playerDataCache.entrySet()) {
            String path = entry.getKey().toString();
            PlayerData data = entry.getValue();
            dataConfig.set(path + ".streak", data.getCurrentStreak());
            dataConfig.set(path + ".lastLogin", data.getLastLogin());
            dataConfig.set(path + ".nextClaim", data.getNextClaimTime());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}