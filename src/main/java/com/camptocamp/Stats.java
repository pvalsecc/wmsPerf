package com.camptocamp;

import java.util.Collections;
import java.util.List;

public class Stats {
    private final String unit;
    private final double average;
    private final long min;
    private final long max;
    private final double stdDev;

    public Stats(String unit, List<Long> values, int divider) {
        this.unit = unit;
        double average = 0.0;
        Collections.sort(values);
        int nb = 0;
        long min = 0;
        long max = 0;
        boolean takeAll = values.size() <= 2;
        final int first = takeAll ? 0 : 1;
        for (int i = first; i < (takeAll ? values.size() : values.size() - 1); ++i) {
            final long val = values.get(i);
            average += val;
            if (i == first) {
                min = val;
                max = val;
            } else {
                min = Math.min(min, val);
                max = Math.max(max, val);
            }
            nb++;
        }
        average = average / nb;
        this.average = average / divider;
        this.min = min / divider;
        this.max = max / divider;

        double var = 0.0;
        for (int i = first; i < (takeAll ? values.size() : values.size() - 1); ++i) {
            final long val = values.get(i);
            var += (val - average) * (val - average);
        }
        this.stdDev = Math.sqrt(var / nb) / divider;
    }

    @Override
    public String toString() {
        return String.format("%sAvg=%d %sMin=%d %sMax=%d %sStdDev=%d", unit, Math.round(average),
                unit, min, unit, max, unit, Math.round(stdDev));
    }

    public long getAverage() {
        return Math.round(average);
    }

    public long getPrecision() {
        return (max - min) / 2;
    }

    public long getStdDev() {
        return Math.round(stdDev);
    }
}
