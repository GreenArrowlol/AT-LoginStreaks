package com.loginstreaks.managers;

import com.cryptomorin.xseries.XEnchantment;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.loginstreaks.LoginStreakRewards;
import com.loginstreaks.models.DayRange;
import com.loginstreaks.models.Reward;
import com.loginstreaks.models.RewardTier;
import com.loginstreaks.models.RewardType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class RewardManager {
    
    private final LoginStreakRewards plugin;
    private final List<RewardTier> tiers;
    
    public RewardManager(LoginStreakRewards plugin) {
        this.plugin = plugin;
        this.tiers = new ArrayList<>();
        loadTiers();
    }
    
    private void loadTiers() {
        tiers.clear();
        ConfigurationSection tiersSection = plugin.getConfig().getConfigurationSection("reward-tiers");
        if (tiersSection == null) return;
        
        for (String tierName : tiersSection.getKeys(false)) {
            ConfigurationSection tierSection = tiersSection.getConfigurationSection(tierName);
            int weight = tierSection.getInt("weight", 0);
            String permission = tierSection.getString("permission", "");
            
            Map<DayRange, List<Reward>> rewards = new HashMap<>();
            ConfigurationSection daysSection = tierSection.getConfigurationSection("days");
            
            if (daysSection != null) {
                for (String dayKey : daysSection.getKeys(false)) {
                    ConfigurationSection daySection = daysSection.getConfigurationSection(dayKey);
                    boolean reset = daySection.getBoolean("reset", false);
                    
                    DayRange range = parseDayRange(dayKey, reset);
                    List<Reward> dayRewards = new ArrayList<>();
                    
                    List<Map<?, ?>> rewardsList = daySection.getMapList("rewards");
                    for (Map<?, ?> rewardMap : rewardsList) {
                        Reward reward = parseReward(rewardMap);
                        if (reward != null) {
                            dayRewards.add(reward);
                        }
                    }
                    
                    rewards.put(range, dayRewards);
                }
            }
            
            tiers.add(new RewardTier(tierName, weight, permission, rewards));
        }
        
        tiers.sort((t1, t2) -> Integer.compare(t2.getWeight(), t1.getWeight()));
    }
    
    private DayRange parseDayRange(String key, boolean reset) {
        if (key.contains("-")) {
            String[] parts = key.split("-");
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            return new DayRange(start, end, reset);
        } else {
            int day = Integer.parseInt(key);
            return new DayRange(day, day, reset);
        }
    }
    
    private Reward parseReward(Map<?, ?> map) {
        try {
            RewardType type = RewardType.valueOf(((String) map.get("type")).toUpperCase());
            Reward reward = new Reward();
            reward.setType(type);
            
            if (type == RewardType.ITEM) {
                reward.setMaterial((String) map.get("material"));
                reward.setAmount(map.containsKey("amount") ? (int) map.get("amount") : 1);
                reward.setName((String) map.get("name"));
                reward.setLore((List<String>) map.get("lore"));
                reward.setEnchantments((List<String>) map.get("enchantments"));
                reward.setSkullTexture((String) map.get("skull_texture"));
            } else {
                reward.setValue((String) map.get("value"));
            }
            
            return reward;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public void giveRewards(Player player, int day) {
        RewardTier tier = getPlayerTier(player);
        if (tier == null) return;
        
        List<Reward> rewards = getRewardsForDay(tier, day, player);
        
        for (Reward reward : rewards) {
            giveReward(player, reward, day);
        }
    }
    
    private List<Reward> getRewardsForDay(RewardTier tier, int day, Player player) {
        DayRange matchingRange = null;
        for (DayRange range : tier.getRewards().keySet()) {
            if (range.contains(day)) {
                matchingRange = range;
                break;
            }
        }
        
        if (matchingRange != null) {
            return tier.getRewards().get(matchingRange);
        }
        
        for (RewardTier fallbackTier : tiers) {
            if (fallbackTier.getWeight() < tier.getWeight()) {
                for (DayRange range : fallbackTier.getRewards().keySet()) {
                    if (range.contains(day)) {
                        return fallbackTier.getRewards().get(range);
                    }
                }
            }
        }
        
        return new ArrayList<>();
    }
    
    public boolean shouldReset(Player player, int day) {
        RewardTier tier = getPlayerTier(player);
        if (tier == null) return false;
        
        for (DayRange range : tier.getRewards().keySet()) {
            if (range.contains(day) && range.isReset()) {
                return true;
            }
        }
        
        for (RewardTier fallbackTier : tiers) {
            if (fallbackTier.getWeight() < tier.getWeight()) {
                for (DayRange range : fallbackTier.getRewards().keySet()) {
                    if (range.contains(day) && range.isReset()) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private RewardTier getPlayerTier(Player player) {
        for (RewardTier tier : tiers) {
            if (tier.getPermission().isEmpty() || player.hasPermission(tier.getPermission())) {
                return tier;
            }
        }
        return tiers.isEmpty() ? null : tiers.get(tiers.size() - 1);
    }
    
    private void giveReward(Player player, Reward reward, int day) {
        String processed = processPlaceholders(reward.getValue() != null ? reward.getValue() : "", player, day);
        
        switch (reward.getType()) {
            case MESSAGE:
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', processed));
                break;
                
            case CONSOLE_COMMAND:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
                break;
                
            case PLAYER_COMMAND:
                player.performCommand(processed);
                break;
                
            case VAULT_MONEY:
                if (plugin.isVaultEnabled() && plugin.getEconomy() != null) {
                    double amount = Double.parseDouble(processed);
                    plugin.getEconomy().depositPlayer(player, amount);
                }
                break;
                
            case ITEM:
                ItemStack item = createItem(reward, player, day);
                if (item != null) {
                    player.getInventory().addItem(item);
                }
                break;
        }
    }
    
    private ItemStack createItem(Reward reward, Player player, int day) {
        ItemStack item = null;
        
        if (reward.getMaterial().equalsIgnoreCase("PLAYER_HEAD") && reward.getSkullTexture() != null) {
            item = XSkull.createItem()
                    .profile(Profileable.detect(reward.getSkullTexture()))
                    .apply();
        } else {
            item = XMaterial.matchXMaterial(reward.getMaterial())
                    .map(XMaterial::parseItem)
                    .orElse(null);
        }
        
        if (item == null) return null;
        
        item.setAmount(reward.getAmount());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (reward.getName() != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
                    processPlaceholders(reward.getName(), player, day)));
            }
            
            if (reward.getLore() != null) {
                meta.setLore(reward.getLore().stream()
                        .map(line -> ChatColor.translateAlternateColorCodes('&', 
                            processPlaceholders(line, player, day)))
                        .collect(Collectors.toList()));
            }
            
            if (reward.getEnchantments() != null) {
                for (String enchantStr : reward.getEnchantments()) {
                    String[] parts = enchantStr.split(":");
                    int level = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    
                    XEnchantment.matchXEnchantment(parts[0]).ifPresent(e -> 
                        meta.addEnchant(e.getEnchant(), level, true));
                }
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private String processPlaceholders(String text, Player player, int day) {
        return text
                .replace("%player%", player.getName())
                .replace("%days%", String.valueOf(day));
    }
    
    public void reload() {
        loadTiers();
    }
}