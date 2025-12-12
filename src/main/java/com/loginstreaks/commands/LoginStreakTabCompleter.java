package com.loginstreaks.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LoginStreakTabCompleter implements TabCompleter {
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("help", "check", "reset", "teststreak", "reload");
            return subcommands.stream()
                .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                .filter(sub -> hasPermissionForSubcommand(sender, sub))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            
            if (subcommand.equals("help")) {
                return Arrays.asList("1", "2", "3");
            }
            
            if (subcommand.equals("check") || subcommand.equals("reset") || subcommand.equals("teststreak")) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("teststreak")) {
            return Arrays.asList("1", "7", "14", "30", "60", "90");
        }
        
        return completions;
    }
    
    private boolean hasPermissionForSubcommand(CommandSender sender, String subcommand) {
        if (subcommand.equals("help")) {
            return sender.hasPermission("loginstreak.help");
        }
        return sender.hasPermission("loginstreak.admin");
    }
}