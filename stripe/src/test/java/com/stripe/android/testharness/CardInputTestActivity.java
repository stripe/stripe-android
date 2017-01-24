package com.stripe.android.testharness;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.stripe.android.R;
import com.stripe.android.view.CardInputView;
import com.stripe.android.view.CardNumberEditText;
import com.stripe.android.view.StripeEditText;
import com.stripe.android.view.ExpiryDateEditText;

/**
 * Activity used to test UI components. We add the layout programmatically to avoid needing test
 * resource files.
 */
public class CardInputTestActivity extends Activity {

    public static final String VALID_AMEX_WITH_SPACES = "3782 822463 10005";
    public static final String VALID_VISA_WITH_SPACES = "4242 4242 4242 4242";

    private CardInputView mCardInputView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCardInputView = new CardInputView(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.addView(mCardInputView);
        setContentView(linearLayout);
    }

    public CardNumberEditText getCardNumberEditText() {
        return (CardNumberEditText) mCardInputView.findViewById(R.id.et_card_number);
    }

    public ExpiryDateEditText getExpiryDateEditText() {
        return (ExpiryDateEditText) mCardInputView.findViewById(R.id.et_expiry_date);
    }

    public StripeEditText getCvcEditText() {
        return (StripeEditText) mCardInputView.findViewById(R.id.et_cvc_number);
    }

    public CardInputView getCardInputView() {
        return mCardInputView;
    }
}
