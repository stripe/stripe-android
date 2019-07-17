package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.InvalidRequestException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Used by {@link StripeApiHandler} to make HTTP requests
 */
final class StripeApiRequestExecutor implements ApiRequestExecutor {

    private static final String CHARSET = StandardCharsets.UTF_8.name();

    @NonNull private final ConnectionFactory mConnectionFactory;

    StripeApiRequestExecutor() {
        mConnectionFactory = new ConnectionFactory();
    }

    /**
     * Make the request and return the response as a {@link StripeResponse}
     */
    @NonNull
    public StripeResponse execute(@NonNull ApiRequest request)
            throws APIConnectionException, InvalidRequestException {
        // HttpURLConnection verifies SSL cert by default
        HttpURLConnection conn = null;
        try {
            conn = mConnectionFactory.create(request);
            // trigger the request
            final int responseCode = conn.getResponseCode();
            final String responseBody;
            if (responseCode >= 200 && responseCode < 300) {
                responseBody = getResponseBody(conn.getInputStream());
            } else {
                responseBody = getResponseBody(conn.getErrorStream());
            }
            return new StripeResponse(responseCode, responseBody, conn.getHeaderFields());
        } catch (IOException e) {
            throw APIConnectionException.create(request.getBaseUrl(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Nullable
    private String getResponseBody(@Nullable InputStream responseStream)
            throws IOException {
        if (responseStream == null) {
            return null;
        }

        //\A is the beginning of
        // the stream boundary
        final Scanner scanner = new Scanner(responseStream, CHARSET).useDelimiter("\\A");
        final String rBody = scanner.hasNext() ? scanner.next() : null;
        responseStream.close();
        return rBody;
    }
}
