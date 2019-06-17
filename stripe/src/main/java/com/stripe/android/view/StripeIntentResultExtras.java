package com.stripe.android.view;

import com.stripe.android.StripeIntentResult;
import com.stripe.android.model.PaymentIntent;

public final class StripeIntentResultExtras {
    /**
     * Should be a {@link PaymentIntent#getClientSecret()}
     */
    public static final String CLIENT_SECRET = "client_secret";

    public static final String AUTH_EXCEPTION = "exception";

    /**
     * See {@link StripeIntentResult.Status} for possible values
     */
    public static final String AUTH_STATUS = "status";

    private StripeIntentResultExtras() {
    }
}
