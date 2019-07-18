package com.stripe.android;

import android.support.annotation.NonNull;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.InvalidRequestException;

interface FireAndForgetRequestExecutor {
    /**
     * @return the response status code. Used for testing purposes.
     */
    int execute(@NonNull StripeRequest request)
            throws APIConnectionException, InvalidRequestException;

    /**
     * Call {@link #execute(StripeRequest)} asynchronously.
     */
    void executeAsync(@NonNull StripeRequest request);
}
