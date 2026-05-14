package com.upgrademe;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UpgradeCalculatorTest {
    private final UpgradeCalculator calculator = new UpgradeCalculator(24);

    @Test
    public void isEligible_returnsTrueWhenDeviceMeetsMinimumAge() {
        assertTrue(calculator.isEligible(24));
        assertTrue(calculator.isEligible(36));
    }

    @Test
    public void isEligible_returnsFalseWhenDeviceIsTooNew() {
        assertFalse(calculator.isEligible(12));
    }

    @Test
    public void eligibilityMessage_returnsUserFriendlyStatus() {
        assertEquals("Upgrade available", calculator.eligibilityMessage(24));
        assertEquals("Keep your current device", calculator.eligibilityMessage(23));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_rejectsNegativeMinimumAge() {
        new UpgradeCalculator(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void isEligible_rejectsNegativeDeviceAge() {
        calculator.isEligible(-1);
    }
}
