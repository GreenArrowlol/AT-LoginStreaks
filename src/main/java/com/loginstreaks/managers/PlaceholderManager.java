package com.loginstreaks.managers;

import com.loginstreaks.LoginStreakRewards;
import com.loginstreaks.data.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderManager extends PlaceholderExpansion {
    
    private final LoginStreakRewards plugin;
    
    public PlaceholderManager(LoginStreakRewards plugin) {
        this.plugin = plugin;
    }
    
    @Override
    @NotNull
    public String getIdentifier() {
        return "loginstreak";
    }
    
    @Override
    @NotNull
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        
        if (params.equalsIgnoreCase("count")) {
            return String.valueOf(data.getCurrentStreak());
        }
        
        if (params.equalsIgnoreCase("next_hours")) {
            long remaining = data.getNextClaimTime() - System.currentTimeMillis();
            if (remaining <= 0) return "0";
            return String.valueOf(remaining / (1000 * 60 * 60));
        }
        
        if (params.equalsIgnoreCase("next_minutes")) {
            long remaining = data.getNextClaimTime() - System.currentTimeMillis();
            if (remaining <= 0) return "0";
            return String.valueOf((remaining / (1000 * 60)) % 60);
        }
        
        return null;
    }
}