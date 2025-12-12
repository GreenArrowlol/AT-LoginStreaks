package com.loginstreaks.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Reward {
    private RewardType type;
    private String value;
    private String material;
    private int amount;
    private String name;
    private List<String> lore;
    private List<String> enchantments;
    private String skullTexture;
}