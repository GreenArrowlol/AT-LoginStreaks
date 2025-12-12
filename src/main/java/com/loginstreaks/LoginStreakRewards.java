package com.loginstreaks;

import com.loginstreaks.commands.LoginStreakCommand;
import com.loginstreaks.commands.LoginStreakTabCompleter;
import com.loginstreaks.data.DataManager;
import com.loginstreaks.listeners.PlayerJoinListener;
import com.loginstreaks.managers.PlaceholderManager;
import com.loginstreaks.managers.RewardManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class LoginStreakRewards extends JavaPlugin {

    private DataManager dataManager;
    private RewardManager rewardManager;
    private PlaceholderManager placeholderManager;
    private Economy economy;
    private boolean vaultEnabled = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dataManager = new DataManager(this);
        rewardManager = new RewardManager(this);

        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            setupEconomy();
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderManager = new PlaceholderManager(this);
            placeholderManager.register();
            getLogger().info("PlaceholderAPI hooked successfully!");
        }

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        LoginStreakCommand commandExecutor = new LoginStreakCommand(this);
        getCommand("loginstreak").setExecutor(commandExecutor);
        getCommand("loginstreak").setTabCompleter(new LoginStreakTabCompleter());

        getLogger().info(ChatColor.GREEN + "LoginStreakRewards has been enabled!");
    }

    @Override
    public void onDisable() {
        if (placeholderManager != null) {
            placeholderManager.unregister();
        }

        if (dataManager != null) {
            dataManager.saveAll();
        }

        getLogger().info(ChatColor.RED + "LoginStreakRewards has been disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        vaultEnabled = economy != null;
        return vaultEnabled;
    }

    public void reloadPlugin() {
        reloadConfig();
        rewardManager.reload();
        getLogger().info("Configuration reloaded!");
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean isVaultEnabled() {
        return vaultEnabled;
    }
}