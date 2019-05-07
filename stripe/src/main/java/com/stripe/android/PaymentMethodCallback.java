package com.stripe.android;

import android.support.annotation.NonNull;

import com.stripe.android.model.PaymentMethod;

/**
 * An interface representing a callback to be notified about the results of
 * {@link PaymentMethod} creation.
 */
public interface PaymentMethodCallback {
    /**
     * Error callback method.
     *
     * @param error the error that occurred.
     */
    void onError(@NonNull Exception error);

    /**
     * Success callback method.
     *
     * @param paymentMethod the {@link PaymentMethod} that was found or created.
     */
    void onSuccess(@NonNull PaymentMethod paymentMethod);
}
