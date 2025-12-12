package com.loginstreaks.commands;

import com.loginstreaks.LoginStreakRewards;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class HelpCommand {
    
    private final LoginStreakRewards plugin;
    private final List<HelpEntry> helpEntries;
    private final int entriesPerPage;
    
    public HelpCommand(LoginStreakRewards plugin) {
        this.plugin = plugin;
        this.entriesPerPage = plugin.getConfig().getInt("help.entries-per-page", 5);
        this.helpEntries = new ArrayList<>();
        loadHelpEntries();
    }
    
    private void loadHelpEntries() {
        helpEntries.clear();
        
        helpEntries.add(new HelpEntry(
            "/loginstreak help [page]",
            "Show this help menu",
            "loginstreak.help"
        ));
        
        helpEntries.add(new HelpEntry(
            "/loginstreak check <player>",
            "Check a player's current streak",
            "loginstreak.admin"
        ));
        
        helpEntries.add(new HelpEntry(
            "/loginstreak reset <player>",
            "Reset a player's login streak",
            "loginstreak.admin"
        ));
        
        helpEntries.add(new HelpEntry(
            "/loginstreak teststreak <player> <day>",
            "Test rewards for a specific day",
            "loginstreak.admin"
        ));
        
        helpEntries.add(new HelpEntry(
            "/loginstreak reload",
            "Reload the plugin configuration",
            "loginstreak.admin"
        ));
    }
    
    public void sendHelp(CommandSender sender, int page) {
        List<HelpEntry> visibleEntries = new ArrayList<>();
        
        for (HelpEntry entry : helpEntries) {
            if (entry.permission == null || sender.hasPermission(entry.permission)) {
                visibleEntries.add(entry);
            }
        }
        
        if (visibleEntries.isEmpty()) {
            sender.sendMessage(color(plugin.getConfig().getString("help.no-commands", "&cNo commands available.")));
            return;
        }
        
        int totalPages = (int) Math.ceil((double) visibleEntries.size() / entriesPerPage);
        
        if (page < 1 || page > totalPages) {
            sender.sendMessage(color(plugin.getConfig().getString("help.invalid-page", "&cInvalid page number. Use 1-%max%.")
                .replace("%max%", String.valueOf(totalPages))));
            return;
        }
        
        String header = plugin.getConfig().getString("help.header", "&6&l=== &e&lLoginStreak Help &6&l(&e%page%&6/&e%max%&6&l) &6&l===");
        header = header.replace("%page%", String.valueOf(page))
                       .replace("%max%", String.valueOf(totalPages));
        sender.sendMessage(color(header));
        
        int startIndex = (page - 1) * entriesPerPage;
        int endIndex = Math.min(startIndex + entriesPerPage, visibleEntries.size());
        
        String commandFormat = plugin.getConfig().getString("help.command-format", "&e%command% &7- &f%description%");
        
        for (int i = startIndex; i < endIndex; i++) {
            HelpEntry entry = visibleEntries.get(i);
            String line = commandFormat.replace("%command%", entry.command)
                                      .replace("%description%", entry.description);
            sender.sendMessage(color(line));
        }
        
        if (page < totalPages) {
            String footer = plugin.getConfig().getString("help.footer", "&7Use &e/loginstreak help %next% &7for next page");
            footer = footer.replace("%next%", String.valueOf(page + 1));
            sender.sendMessage(color(footer));
        }
        
        String bottomBorder = plugin.getConfig().getString("help.bottom-border", "&6&l================================");
        sender.sendMessage(color(bottomBorder));
    }
    
    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    private static class HelpEntry {
        private final String command;
        private final String description;
        private final String permission;
        
        public HelpEntry(String command, String description, String permission) {
            this.command = command;
            this.description = description;
            this.permission = permission;
        }
    }
}