package com.loginstreaks.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerData {
    private int currentStreak;
    private long lastLogin;
    private long nextClaimTime;
}