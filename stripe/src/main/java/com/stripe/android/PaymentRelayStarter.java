package com.stripe.android;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.model.StripeIntent;
import com.stripe.android.utils.ObjectUtils;
import com.stripe.android.view.AuthActivityStarter;
import com.stripe.android.view.PaymentRelayActivity;
import com.stripe.android.view.StripeIntentResultExtras;

/**
 * Starts an instance of {@link PaymentRelayStarter}.
 * Should only be called from {@link PaymentController}.
 */
class PaymentRelayStarter implements AuthActivityStarter<PaymentRelayStarter.Data> {
    @NonNull private final Host mHost;
    private final int mRequestCode;

    PaymentRelayStarter(@NonNull Host host, int requestCode) {
        mHost = host;
        mRequestCode = requestCode;
    }

    @Override
    public void start(@NonNull Data data) {
        final Bundle extras = new Bundle();
        extras.putString(StripeIntentResultExtras.CLIENT_SECRET,
                        data.stripeIntent != null ? data.stripeIntent.getClientSecret() : null);
        extras.putSerializable(StripeIntentResultExtras.AUTH_EXCEPTION, data.exception);
        mHost.startActivityForResult(PaymentRelayActivity.class, extras, mRequestCode);
    }

    public static final class Data {
        @Nullable final StripeIntent stripeIntent;
        @Nullable final Exception exception;

        /**
         * Use when payment authentication completed or can be bypassed.
         */
        Data(@NonNull StripeIntent stripeIntent) {
            this.stripeIntent = stripeIntent;
            this.exception = null;
        }

        /**
         * Use when payment authentication resulted in an error.
         */
        Data(@NonNull Exception exception) {
            this.stripeIntent = null;
            this.exception = exception;
        }

        @Override
        public int hashCode() {
            return ObjectUtils.hash(stripeIntent, exception);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return super.equals(obj) || (obj instanceof Data && typedEquals((Data) obj));
        }

        private boolean typedEquals(@NonNull Data data) {
            return ObjectUtils.equals(stripeIntent, data.stripeIntent) &&
                    ObjectUtils.equals(exception, data.exception);
        }
    }
}
