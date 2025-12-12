package com.loginstreaks.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class RewardTier {
    private String name;
    private int weight;
    private String permission;
    private Map<DayRange, List<Reward>> rewards;
}