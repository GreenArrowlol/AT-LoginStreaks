package com.loginstreaks.commands;

import com.loginstreaks.LoginStreakRewards;
import com.loginstreaks.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LoginStreakCommand implements CommandExecutor {
    
    private final LoginStreakRewards plugin;
    
    public LoginStreakCommand(LoginStreakRewards plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("loginstreak.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "teststreak":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /loginstreak teststreak <player> <day>");
                    return true;
                }
                handleTestStreak(sender, args[1], args[2]);
                break;
                
            case "reset":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /loginstreak reset <player>");
                    return true;
                }
                handleReset(sender, args[1]);
                break;
                
            case "check":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /loginstreak check <player>");
                    return true;
                }
                handleCheck(sender, args[1]);
                break;
                
            case "reload":
                plugin.reloadConfig();
                plugin.getRewardManager().reload();
                sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
                break;
                
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void handleTestStreak(CommandSender sender, String playerName, String dayStr) {
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }
        
        try {
            int day = Integer.parseInt(dayStr);
            plugin.getRewardManager().giveRewards(target, day);
            
            String message = plugin.getConfig().getString("messages.admin-test", "&aGave rewards.");
            message = message.replace("%player%", target.getName())
                            .replace("%days%", String.valueOf(day));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid day number.");
        }
    }
    
    private void handleReset(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }
        
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        data.setCurrentStreak(0);
        data.setLastLogin(0);
        data.setNextClaimTime(0);
        plugin.getDataManager().setPlayerData(target.getUniqueId(), data);
        
        String message = plugin.getConfig().getString("messages.admin-reset", "&aReset streak.");
        message = message.replace("%player%", target.getName());
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    private void handleCheck(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }
        
        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        
        String message = plugin.getConfig().getString("messages.admin-check", "&ePlayer streak info.");
        message = message.replace("%player%", target.getName())
                        .replace("%days%", String.valueOf(data.getCurrentStreak()));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        
        long remaining = data.getNextClaimTime() - System.currentTimeMillis();
        if (remaining > 0) {
            long hours = remaining / (1000 * 60 * 60);
            long minutes = (remaining / (1000 * 60)) % 60;
            sender.sendMessage(ChatColor.YELLOW + "Next claim in: " + hours + "h " + minutes + "m");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Next claim: Available now");
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== LoginStreak Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/loginstreak teststreak <player> <day>" + ChatColor.GRAY + " - Test rewards for a specific day");
        sender.sendMessage(ChatColor.YELLOW + "/loginstreak reset <player>" + ChatColor.GRAY + " - Reset player's streak");
        sender.sendMessage(ChatColor.YELLOW + "/loginstreak check <player>" + ChatColor.GRAY + " - Check player's streak");
        sender.sendMessage(ChatColor.YELLOW + "/loginstreak reload" + ChatColor.GRAY + " - Reload configuration");
    }
}