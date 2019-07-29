package com.stripe.android;

import android.app.Activity;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.StripeIntent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A model representing the result of a {@link StripeIntent} confirmation or authentication attempt
 * via {@link Stripe#confirmPayment(Activity, ConfirmPaymentIntentParams)} or
 * {@link Stripe#authenticatePayment(Activity, String)}}
 *
 * {@link #getIntent()} represents a {@link StripeIntent} retrieved after
 * confirmation/authentication succeeded or failed.
 */
public interface StripeIntentResult<T extends StripeIntent> {

    @NonNull T getIntent();

    @Status int getStatus();

    /**
     * Values that indicate the outcome of confirmation and payment authentication.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Status.UNKNOWN, Status.SUCCEEDED, Status.FAILED, Status.CANCELED, Status.TIMEDOUT})
    @interface Status {
        int UNKNOWN = 0;

        /**
         * Confirmation or payment authentication succeeded
         */
        int SUCCEEDED = 1;

        /**
         * Confirm or payment authentication failed
         */
        int FAILED = 2;

        /**
         * Payment authentication was canceled by the user
         */
        int CANCELED = 3;

        /**
         * Payment authentication timed-out
         */
        int TIMEDOUT = 4;
    }
}
