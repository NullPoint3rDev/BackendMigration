package org.alloy.models.utils;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Repeating {
    private String type;  // daily, weekly, monthly
    private int interval; // every N days/weeks/months
    private String[] weekDays; // for weekly repeating
    private int[] monthDays;   // for monthly repeating
    private String time;       // time of day for the repeating task
}
