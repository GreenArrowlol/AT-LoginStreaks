package com.loginstreaks;

import com.loginstreaks.commands.LoginStreakCommand;
import com.loginstreaks.data.DataManager;
import com.loginstreaks.listeners.PlayerJoinListener;
import com.loginstreaks.managers.PlaceholderManager;
import com.loginstreaks.managers.RewardManager;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class LoginStreakRewards extends JavaPlugin {
    
    private DataManager dataManager;
    private RewardManager rewardManager;
    private Economy economy;
    private boolean vaultEnabled = false;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        new Metrics(this, 28303);
        
        this.dataManager = new DataManager(this);
        this.rewardManager = new RewardManager(this);
        
        setupVault();
        
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getCommand("loginstreak").setExecutor(new LoginStreakCommand(this));
        
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderManager(this).register();
            getLogger().info("PlaceholderAPI hooked!");
        }
        
        getLogger().info("LoginStreakRewards enabled!");
    }
    
    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("LoginStreakRewards disabled!");
    }
    
    private void setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found! Economy rewards disabled.");
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("No economy provider found! Economy rewards disabled.");
            return;
        }
        
        economy = rsp.getProvider();
        vaultEnabled = true;
        getLogger().info("Vault hooked!");
    }
}