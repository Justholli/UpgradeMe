package com.upgrademe;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class MainActivityRobolectricTest {
    @Test
    public void checkUpgrade_showsEligibleMessageForOlderDevice() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        EditText ageInput = activity.findViewById(MainActivity.AGE_INPUT_ID);
        Button checkButton = activity.findViewById(MainActivity.CHECK_BUTTON_ID);
        TextView resultText = activity.findViewById(MainActivity.RESULT_TEXT_ID);

        ageInput.setText("24");
        checkButton.performClick();

        assertEquals("Upgrade available", resultText.getText().toString());
    }

    @Test
    public void checkUpgrade_showsKeepDeviceMessageForNewerDevice() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        EditText ageInput = activity.findViewById(MainActivity.AGE_INPUT_ID);
        Button checkButton = activity.findViewById(MainActivity.CHECK_BUTTON_ID);
        TextView resultText = activity.findViewById(MainActivity.RESULT_TEXT_ID);

        ageInput.setText("12");
        checkButton.performClick();

        assertEquals("Keep your current device", resultText.getText().toString());
    }

    @Test
    public void checkUpgrade_promptsUserWhenAgeIsEmpty() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        Button checkButton = activity.findViewById(MainActivity.CHECK_BUTTON_ID);
        TextView resultText = activity.findViewById(MainActivity.RESULT_TEXT_ID);

        checkButton.performClick();

        assertEquals("Enter device age", resultText.getText().toString());
    }
}
