package com.stripe.android;

import android.app.Activity;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
    @Status private final int mStatus;

    public StripeIntentResult(@NonNull T stripeIntent, @Status int status) {
        mStripeIntent = stripeIntent;
        mStatus = calculateStatus(Objects.requireNonNull(stripeIntent.getStatus()), status);
    }

    @StripeIntentResult.Status
    private static int calculateStatus(@NonNull StripeIntent.Status stripeIntentStatus,
                                       @StripeIntentResult.Status int authStatus) {
        if (authStatus != StripeIntentResult.Status.UNKNOWN) {
            return authStatus;
        }

        switch (stripeIntentStatus) {
            case RequiresAction:
            case Canceled: {
                return StripeIntentResult.Status.CANCELED;
            }
            case RequiresPaymentMethod: {
                return StripeIntentResult.Status.FAILED;
            }
            case Succeeded:
            case RequiresCapture:
            case RequiresConfirmation: {
                return StripeIntentResult.Status.SUCCEEDED;
            }
            case Processing:
            default: {
                return StripeIntentResult.Status.UNKNOWN;
            }
        }
    }

    @NonNull
    public final T getIntent() {
        return mStripeIntent;
    }

    public final int getStatus() {
        return mStatus;
    }

    @Override
    public final boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof StripeIntentResult &&
                typedEquals((StripeIntentResult) obj));
    }

    private boolean typedEquals(@NonNull StripeIntentResult setupIntentResult) {
        return ObjectUtils.equals(mStripeIntent, setupIntentResult.mStripeIntent)
                && ObjectUtils.equals(mStatus, setupIntentResult.mStatus);
    }

    @Override
    public final int hashCode() {
        return ObjectUtils.hash(mStripeIntent, mStatus);
    }

    /**
     * Values that indicate the outcome of confirmation and payment authentication.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Status.UNKNOWN, Status.SUCCEEDED, Status.FAILED, Status.CANCELED, Status.TIMEDOUT})
    public @interface Status {
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
