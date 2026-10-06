package com.byteforce.domain;

/**
 * Value object representing question attempt metrics categorized by difficulty (EASY, MEDIUM, HARD).
 */
public record DifficultyDistribution(
        long easyTotal,
        long easySolved,
        long mediumTotal,
        long mediumSolved,
        long hardTotal,
        long hardSolved
) {

    public static DifficultyDistribution empty() {
        return new DifficultyDistribution(0, 0, 0, 0, 0, 0);
    }

    public double easyAccuracy() {
        return easyTotal > 0 ? Math.round((easySolved * 1000.0 / easyTotal)) / 10.0 : 0.0;
    }

    public double mediumAccuracy() {
        return mediumTotal > 0 ? Math.round((mediumSolved * 1000.0 / mediumTotal)) / 10.0 : 0.0;
    }

    public double hardAccuracy() {
        return hardTotal > 0 ? Math.round((hardSolved * 1000.0 / hardTotal)) / 10.0 : 0.0;
    }

    public long totalAttempted() {
        return easyTotal + mediumTotal + hardTotal;
    }

    public long totalSolved() {
        return easySolved + mediumSolved + hardSolved;
    }
}
