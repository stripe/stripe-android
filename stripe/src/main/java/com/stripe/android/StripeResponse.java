package com.stripe.android;

import android.support.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Represents a response from the Stripe servers.
 */
class StripeResponse {

    private final int mResponseCode;
    @Nullable private final String mResponseBody;
    @Nullable private final Map<String, List<String>> mResponseHeaders;

    /**
     * Object constructor.
     *
     * @param responseCode the response code (i.e. 404)
     * @param responseBody the body of the response
     * @param responseHeaders any headers associated with the response
     */
    StripeResponse(
            int responseCode,
            @Nullable String responseBody,
            @Nullable Map<String, List<String>> responseHeaders) {
        mResponseCode = responseCode;
        mResponseBody = responseBody;
        mResponseHeaders = responseHeaders;
    }

    /**
     * @return the {@link #mResponseCode response code}.
     */
    int getResponseCode() {
        return mResponseCode;
    }

    /**
     * @return the {@link #mResponseBody response body}.
     */
    @Nullable
    String getResponseBody() {
        return mResponseBody;
    }

    /**
     * @return the {@link #mResponseHeaders response headers}.
     */
    @Nullable
    Map<String, List<String>> getResponseHeaders() {
        return mResponseHeaders;
    }
}
