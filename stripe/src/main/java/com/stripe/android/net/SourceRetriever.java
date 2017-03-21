package com.stripe.android.net;

import android.support.annotation.NonNull;

import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Source;

/**
 * Represents something that can retrieve a source.
 */
interface SourceRetriever {
    Source retrieveSource(
            @NonNull String sourceId,
            @NonNull String clientSecret,
            @NonNull String publishableKey)
            throws StripeException;
}
