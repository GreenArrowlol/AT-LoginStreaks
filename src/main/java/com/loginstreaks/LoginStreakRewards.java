package com.loginstreaks;

import com.loginstreaks.commands.LoginStreakCommand;
import com.loginstreaks.commands.LoginStreakTabCompleter;
import com.loginstreaks.data.DataManager;
import com.loginstreaks.listeners.PlayerJoinListener;
import com.loginstreaks.managers.PlaceholderManager;
import com.loginstreaks.managers.RewardManager;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class LoginStreakRewards extends JavaPlugin {
    
    private DataManager dataManager;
    private RewardManager rewardManager;
    private PlaceholderManager placeholderManager;
    private Economy economy;
    private boolean vaultEnabled;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        this.dataManager = new DataManager(this);
        this.rewardManager = new RewardManager(this);
        
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        
        getCommand("loginstreak").setExecutor(new LoginStreakCommand(this));
        getCommand("loginstreak").setTabCompleter(new LoginStreakTabCompleter());
        
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderManager = new PlaceholderManager(this);
            this.placeholderManager.register();
            getLogger().info("PlaceholderAPI hooked!");
        }
        
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            setupEconomy();
        }
        
        new Metrics(this, 23918);
        
        getLogger().info("LoginStreakRewards has been enabled!");
    }
    
    @Override
    public void onDisable() {
        if (placeholderManager != null) {
            placeholderManager.unregister();
        }
        
        if (dataManager != null) {
            dataManager.saveAll();
        }
        
        getLogger().info("LoginStreakRewards has been disabled!");
    }
    
    private void setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
            vaultEnabled = true;
            getLogger().info("Vault economy hooked!");
        }
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