package com.upgrademe;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    public static final int AGE_INPUT_ID = 1001;
    public static final int CHECK_BUTTON_ID = 1002;
    public static final int RESULT_TEXT_ID = 1003;

    private final UpgradeCalculator calculator = new UpgradeCalculator(24);
    private EditText ageInput;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        ageInput = new EditText(this);
        ageInput.setId(AGE_INPUT_ID);
        ageInput.setHint("Device age in months");
        ageInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        root.addView(ageInput);

        Button checkButton = new Button(this);
        checkButton.setId(CHECK_BUTTON_ID);
        checkButton.setText("Check upgrade");
        root.addView(checkButton);

        resultText = new TextView(this);
        resultText.setId(RESULT_TEXT_ID);
        root.addView(resultText);

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showUpgradeStatus();
            }
        });

        setContentView(root);
    }

    private void showUpgradeStatus() {
        String enteredAge = ageInput.getText().toString().trim();
        if (enteredAge.isEmpty()) {
            resultText.setText("Enter device age");
            return;
        }

        int ageMonths = Integer.parseInt(enteredAge);
        resultText.setText(calculator.eligibilityMessage(ageMonths));
    }
}
