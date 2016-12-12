package com.stripe.android.net;

import android.support.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Represents a response from the Stripe servers.
 */
public class StripeResponse {

    private String mResponseBody;
    private int mResponseCode;
    private Map<String, List<String>> mResponseHeaders;

    /**
     * Object constructor.
     *
     * @param responseCode the response code (i.e. 404)
     * @param responseBody the body of the response
     * @param responseHeaders any headers associated with the response
     */
    public StripeResponse(
            int responseCode,
            String responseBody,
            @Nullable Map<String, List<String>> responseHeaders) {
        mResponseCode = responseCode;
        mResponseBody = responseBody;
        mResponseHeaders = responseHeaders;
    }

    /**
     * @return the {@link #mResponseCode response code}.
     */
    public int getResponseCode() {
        return mResponseCode;
    }

    /**
     * @return the {@link #mResponseBody response body}.
     */
    public String getResponseBody() {
        return mResponseBody;
    }

    /**
     * @return the {@link #mResponseHeaders response headers}.
     */
    @Nullable
    public Map<String, List<String>> getResponseHeaders() {
        return mResponseHeaders;
    }
}
