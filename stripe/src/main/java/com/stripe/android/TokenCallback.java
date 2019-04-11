package com.stripe.android;

import android.support.annotation.NonNull;

import com.stripe.android.model.Token;

/**
 * An interface representing a callback to be notified about the results of
 * {@link Token} creation or requests
 */
public interface TokenCallback {

    /**
     * Error callback method.
     * @param error the error that occurred.
     */
    void onError(@NonNull Exception error);

    /**
     * Success callback method.
     * @param token the {@link Token} that was found or created.
     */
    void onSuccess(@NonNull Token token);
}
