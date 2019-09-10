package com.stripe.android;

import android.app.Activity;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.StripeIntent;
import com.stripe.android.utils.ObjectUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * A model representing the result of a {@link StripeIntent} confirmation or authentication attempt
 * via {@link Stripe#confirmPayment(Activity, ConfirmPaymentIntentParams)} or
 * {@link Stripe#authenticatePayment(Activity, String)}}
 *
 * {@link #getIntent()} represents a {@link StripeIntent} retrieved after
 * confirmation/authentication succeeded or failed.
 */
public abstract class StripeIntentResult<T extends StripeIntent> {
    @NonNull private final T mStripeIntent;
    @Outcome private final int mOutcome;

    StripeIntentResult(@NonNull T stripeIntent, @Outcome int outcome) {
        mStripeIntent = stripeIntent;
        mOutcome = determineOutcome(Objects.requireNonNull(stripeIntent.getStatus()), outcome);
    }

    @StripeIntentResult.Outcome
    private static int determineOutcome(@NonNull StripeIntent.Status stripeIntentStatus,
                                        @StripeIntentResult.Outcome int outcome) {
        if (outcome != StripeIntentResult.Outcome.UNKNOWN) {
            return outcome;
        }

        switch (stripeIntentStatus) {
            case RequiresAction:
            case Canceled: {
                return StripeIntentResult.Outcome.CANCELED;
            }
            case RequiresPaymentMethod: {
                return StripeIntentResult.Outcome.FAILED;
            }
            case Succeeded:
            case RequiresCapture:
            case RequiresConfirmation: {
                return StripeIntentResult.Outcome.SUCCEEDED;
            }
            case Processing:
            default: {
                return StripeIntentResult.Outcome.UNKNOWN;
            }
        }
    }

    @NonNull
    public final T getIntent() {
        return mStripeIntent;
    }

    @Outcome
    public final int getOutcome() {
        return mOutcome;
    }

    @Override
    public final boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof StripeIntentResult &&
                typedEquals((StripeIntentResult) obj));
    }

    private boolean typedEquals(@NonNull StripeIntentResult setupIntentResult) {
        return Objects.equals(mStripeIntent, setupIntentResult.mStripeIntent)
                && Objects.equals(mOutcome, setupIntentResult.mOutcome);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(mStripeIntent, mOutcome);
    }

    /**
     * Values that indicate the outcome of confirmation and payment authentication.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Outcome.UNKNOWN, Outcome.SUCCEEDED, Outcome.FAILED, Outcome.CANCELED,
            Outcome.TIMEDOUT})
    public @interface Outcome {
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
