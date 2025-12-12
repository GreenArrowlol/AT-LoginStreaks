package com.loginstreaks.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DayRange {
    private int start;
    private int end;
    private boolean reset;
    
    public boolean contains(int day) {
        return day >= start && day <= end;
    }
}