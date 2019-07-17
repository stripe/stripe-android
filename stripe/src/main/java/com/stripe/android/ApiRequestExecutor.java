package com.stripe.android;

import android.support.annotation.NonNull;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.InvalidRequestException;

interface ApiRequestExecutor {
    @NonNull
    StripeResponse execute(@NonNull ApiRequest request)
            throws APIConnectionException, InvalidRequestException;
}
