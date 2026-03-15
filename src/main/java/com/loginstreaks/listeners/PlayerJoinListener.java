package com.loginstreaks.listeners;

import com.loginstreaks.LoginStreakRewards;
import com.loginstreaks.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    
    private final LoginStreakRewards plugin;
    
    public PlayerJoinListener(LoginStreakRewards plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        
        long now = System.currentTimeMillis();
        long checkInterval = plugin.getConfig().getLong("claim-interval-hours",
                plugin.getConfig().getLong("check-interval-hours", 24)) * 60L * 60L * 1000L;
        
        if (data.getNextClaimTime() == 0) {
            handleFirstJoin(player, data, now, checkInterval);
        } else if (now >= data.getNextClaimTime()) {
            long missedTime = now - data.getNextClaimTime();
            long gracePeriod = checkInterval;
            
            if (missedTime > gracePeriod) {
                handleMissedStreak(player, data, now, checkInterval);
            } else {
                handleOnTimeJoin(player, data, now, checkInterval);
            }
        } else {
            handleTooEarly(player, data);
        }
        
        plugin.getDataManager().setPlayerData(player.getUniqueId(), data);
    }
    
    private void handleFirstJoin(Player player, PlayerData data, long now, long checkInterval) {
        data.setCurrentStreak(1);
        data.setLastLogin(now);
        data.setNextClaimTime(now + checkInterval);
        
        String message = plugin.getConfig().getString("messages.on-time", "&aWelcome! Here's your first reward!");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', replacePlaceholders(message)));
        
        plugin.getRewardManager().giveRewards(player, 1);
        
        String claimed = plugin.getConfig().getString("messages.reward-claimed", "&aRewards claimed!");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            replacePlaceholders(claimed).replace("%days%", "1")));
    }
    
    private void handleOnTimeJoin(Player player, PlayerData data, long now, long checkInterval) {
        int newStreak = data.getCurrentStreak() + 1;
        data.setCurrentStreak(newStreak);
        data.setLastLogin(now);
        data.setNextClaimTime(now + checkInterval);
        
        String message = plugin.getConfig().getString("messages.on-time", "&aYou've joined right on time! Here's your rewards:");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', replacePlaceholders(message)));
        
        plugin.getRewardManager().giveRewards(player, newStreak);
        
        String claimed = plugin.getConfig().getString("messages.reward-claimed", "&aRewards claimed!");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            replacePlaceholders(claimed).replace("%days%", String.valueOf(newStreak))));
        
        if (plugin.getRewardManager().shouldReset(player, newStreak)) {
            data.setCurrentStreak(0);
        }
    }
    
    private void handleMissedStreak(Player player, PlayerData data, long now, long checkInterval) {
        data.setCurrentStreak(1);
        data.setLastLogin(now);
        data.setNextClaimTime(now + checkInterval);
        
        String message = plugin.getConfig().getString("messages.missed-streak", "&cYou missed the streak, the streak has been restarted.");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', replacePlaceholders(message)));
        
        plugin.getRewardManager().giveRewards(player, 1);
        
        String claimed = plugin.getConfig().getString("messages.reward-claimed", "&aRewards claimed!");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            replacePlaceholders(claimed).replace("%days%", "1")));
    }
    
    private void handleTooEarly(Player player, PlayerData data) {
        long remaining = data.getNextClaimTime() - System.currentTimeMillis();
        long hours = remaining / (1000 * 60 * 60);
        long minutes = (remaining / (1000 * 60)) % 60;
        
        String message = plugin.getConfig().getString("messages.time-remaining", "&e%hours% hours and %minutes% minutes remaining for next streak!");
        message = replacePlaceholders(message)
                        .replace("%hours%", String.valueOf(hours))
                        .replace("%minutes%", String.valueOf(minutes));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    private String replacePlaceholders(String message) {
        String prefix = plugin.getConfig().getString("prefix", "");
        return message.replace("%prefix%", prefix);
    }
}
