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
            if (tierSection == null) {
                continue;
            }

            int weight = tierSection.getInt("weight", 0);
            String permission = tierSection.getString("permission", "");

            Map<DayRange, List<Reward>> rewards = new LinkedHashMap<>();
            ConfigurationSection daysSection = tierSection.getConfigurationSection("days");

            if (daysSection != null) {
                for (String dayKey : daysSection.getKeys(false)) {
                    ConfigurationSection daySection = daysSection.getConfigurationSection(dayKey);
                    if (daySection == null) {
                        continue;
                    }

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
            } else {
                DayRange range = parseLegacyDayRange(tierSection);
                if (range != null) {
                    List<Reward> dayRewards = parseLegacyRewards(tierSection);
                    rewards.put(range, dayRewards);
                }
            }

            tiers.add(new RewardTier(tierName, weight, permission, rewards));
        }
        
        tiers.sort((t1, t2) -> Integer.compare(t2.getWeight(), t1.getWeight()));
    }
    

    private DayRange parseLegacyDayRange(ConfigurationSection tierSection) {
        String dayRange = tierSection.getString("day-range", "").trim();
        if (dayRange.isEmpty()) {
            return null;
        }

        if (dayRange.endsWith("+")) {
            int start = Integer.parseInt(dayRange.substring(0, dayRange.length() - 1));
            return new DayRange(start, Integer.MAX_VALUE, false);
        }

        return parseDayRange(dayRange, false);
    }

    private List<Reward> parseLegacyRewards(ConfigurationSection tierSection) {
        List<Reward> rewards = new ArrayList<>();

        String type = tierSection.getString("type", "").toUpperCase(Locale.ROOT);
        if (!"COMMAND".equals(type)) {
            return rewards;
        }

        List<String> commands = tierSection.getStringList("commands");
        for (String command : commands) {
            Reward reward = new Reward();
            reward.setType(RewardType.CONSOLE_COMMAND);
            reward.setValue(command);
            rewards.add(reward);
        }

        String message = tierSection.getString("message", "");
        if (!message.isEmpty()) {
            Reward reward = new Reward();
            reward.setType(RewardType.MESSAGE);
            reward.setValue(message);
            rewards.add(reward);
        }

        String broadcastMessage = tierSection.getString("broadcast-message", "");
        if (tierSection.getBoolean("broadcast", false) && !broadcastMessage.isEmpty()) {
            Reward reward = new Reward();
            reward.setType(RewardType.CONSOLE_COMMAND);
            reward.setValue("broadcast " + broadcastMessage);
            rewards.add(reward);
        }

        return rewards;
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
        
        List<Reward> rewards = getRewardsForDay(tier, day);
        
        for (Reward reward : rewards) {
            giveReward(player, reward, day);
        }
    }
    
    private List<Reward> getRewardsForDay(RewardTier tier, int day) {
        List<Reward> tierRewards = findRewardsForDay(tier, day);
        if (!tierRewards.isEmpty()) {
            return tierRewards;
        }

        for (RewardTier fallbackTier : tiers) {
            if (fallbackTier.getWeight() < tier.getWeight()) {
                List<Reward> fallbackRewards = findRewardsForDay(fallbackTier, day);
                if (!fallbackRewards.isEmpty()) {
                    return fallbackRewards;
                }
            }
        }

        return new ArrayList<>();
    }
    

    private List<Reward> findRewardsForDay(RewardTier tier, int day) {
        DayRange range = findMatchingRange(tier, day);
        return range != null ? tier.getRewards().getOrDefault(range, Collections.emptyList()) : Collections.emptyList();
    }

    private DayRange findMatchingRange(RewardTier tier, int day) {
        return tier.getRewards().keySet().stream()
                .filter(range -> range.contains(day))
                .min(Comparator.comparingInt(DayRange::getStart)
                        .thenComparingInt(range -> range.getEnd() - range.getStart()))
                .orElse(null);
    }

    public boolean shouldReset(Player player, int day) {
        RewardTier tier = getPlayerTier(player);
        if (tier == null) return false;
        
        DayRange tierRange = findMatchingRange(tier, day);
        if (tierRange != null && tierRange.isReset()) {
            return true;
        }

        for (RewardTier fallbackTier : tiers) {
            if (fallbackTier.getWeight() < tier.getWeight()) {
                DayRange fallbackRange = findMatchingRange(fallbackTier, day);
                if (fallbackRange != null && fallbackRange.isReset()) {
                    return true;
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
        String prefix = plugin.getConfig().getString("prefix", "");
        return text
                .replace("%prefix%", prefix)
                .replace("%player%", player.getName())
                .replace("%days%", String.valueOf(day));
    }
    
    public void reload() {
        loadTiers();
    }
}