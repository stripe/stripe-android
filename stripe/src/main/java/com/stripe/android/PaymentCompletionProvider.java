package com.stripe.android;

import android.support.annotation.NonNull;

/**
 * Represents a class that can call to a server and process a charge based on input payment data.
 */
public interface PaymentCompletionProvider {

    /**
     * Called to complete a charge action. Note that this method should handle its own threading,
     * as it will be called on the UI thread, and it should notify the listener on the UI thread.
     *
     * @param data the {@link PaymentSessionData} to be used to create a charge
     * @param listener a {@link PaymentResultListener} to notify of the result
     */
    void completePayment(@NonNull PaymentSessionData data,
                         @NonNull PaymentResultListener listener);
}
