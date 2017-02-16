package com.stripe.android.testharness;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.stripe.android.R;
import com.stripe.android.view.CardInputWidget;
import com.stripe.android.view.CardNumberEditText;
import com.stripe.android.view.StripeEditText;
import com.stripe.android.view.ExpiryDateEditText;

/**
 * Activity used to test UI components. We add the layout programmatically to avoid needing test
 * resource files.
 */
public class CardInputTestActivity extends Activity {

    public static final String VALID_AMEX_NO_SPACES = "378282246310005";
    public static final String VALID_AMEX_WITH_SPACES = "3782 822463 10005";
    public static final String VALID_DINERS_CLUB_NO_SPACES = "30569309025904";
    public static final String VALID_DINERS_CLUB_WITH_SPACES = "3056 9309 0259 04";
    public static final String VALID_VISA_NO_SPACES = "4242424242424242";
    public static final String VALID_VISA_WITH_SPACES = "4242 4242 4242 4242";

    private CardInputWidget mCardInputWidget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCardInputWidget = new CardInputWidget(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.addView(mCardInputWidget);
        setContentView(linearLayout);
    }

    public CardNumberEditText getCardNumberEditText() {
        return (CardNumberEditText) mCardInputWidget.findViewById(R.id.et_card_number);
    }

    public ExpiryDateEditText getExpiryDateEditText() {
        return (ExpiryDateEditText) mCardInputWidget.findViewById(R.id.et_expiry_date);
    }

    public StripeEditText getCvcEditText() {
        return (StripeEditText) mCardInputWidget.findViewById(R.id.et_cvc_number);
    }

    public CardInputWidget getCardInputWidget() {
        return mCardInputWidget;
    }
}
