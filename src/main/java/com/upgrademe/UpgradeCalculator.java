package com.upgrademe;

/**
 * Calculates whether a device is eligible for an upgrade based on its age.
 */
public final class UpgradeCalculator {
    private final int minimumEligibleAgeMonths;

    public UpgradeCalculator(int minimumEligibleAgeMonths) {
        if (minimumEligibleAgeMonths < 0) {
            throw new IllegalArgumentException("minimumEligibleAgeMonths must be non-negative");
        }
        this.minimumEligibleAgeMonths = minimumEligibleAgeMonths;
    }

    public boolean isEligible(int deviceAgeMonths) {
        if (deviceAgeMonths < 0) {
            throw new IllegalArgumentException("deviceAgeMonths must be non-negative");
        }
        return deviceAgeMonths >= minimumEligibleAgeMonths;
    }

    public String eligibilityMessage(int deviceAgeMonths) {
        return isEligible(deviceAgeMonths)
                ? "Upgrade available"
                : "Keep your current device";
    }
}
