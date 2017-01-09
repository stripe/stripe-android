package com.stripe.android.testharness;

import android.app.Activity;
import android.os.Bundle;

import com.stripe.android.R;
import com.stripe.android.view.CardNumberEditText;

/**
 * Activity used to test UI components.
 */
public class CardInputTestActivity extends Activity {

    private CardNumberEditText mCardNumberEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card_input_view);
        mCardNumberEditText = (CardNumberEditText) findViewById(R.id.et_card_number);
    }

    public CardNumberEditText getCardNumberEditText() {
        return mCardNumberEditText;
    }
}
