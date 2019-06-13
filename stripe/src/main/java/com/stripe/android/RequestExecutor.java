package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.InvalidRequestException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Used by {@link StripeApiHandler} to make HTTP requests
 */
class RequestExecutor {

    private static final String CHARSET = StandardCharsets.UTF_8.name();

    @NonNull private final ConnectionFactory mConnectionFactory;

    RequestExecutor() {
        mConnectionFactory = new ConnectionFactory();
    }

    /**
     * Make the request and return the response as a {@link StripeResponse}
     */
    @NonNull
    StripeResponse execute(@NonNull StripeRequest request)
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
            throw createApiConnectionException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Make the request and ignore the response
     *
     * @return the response status code. Used for testing purposes.
     */
    int executeAndForget(@NonNull StripeRequest request)
            throws APIConnectionException, InvalidRequestException {
        // HttpURLConnection verifies SSL cert by default
        HttpURLConnection conn = null;
        try {
            conn = mConnectionFactory.create(request);
            // required to trigger the request
            return conn.getResponseCode();
        } catch (IOException e) {
            throw createApiConnectionException(e);
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

    @NonNull
    private APIConnectionException createApiConnectionException(@NonNull Exception e) {
        return new APIConnectionException(
                String.format(Locale.ENGLISH,
                        "IOException during API request to Stripe (%s): %s "
                                + "Please check your internet connection and try again. "
                                + "If this problem persists, you should check Stripe's "
                                + "service status at https://twitter.com/stripestatus, "
                                + "or let us know at support@stripe.com.",
                        StripeApiHandler.getTokensUrl(), e.getMessage()), e);
    }

    static class ConnectionFactory {
        private static final SSLSocketFactory SSL_SOCKET_FACTORY = new StripeSSLSocketFactory();

        @NonNull
        private HttpURLConnection create(@NonNull StripeRequest request)
                throws IOException, InvalidRequestException {
            final URL stripeURL = new URL(request.getUrl());
            final HttpURLConnection conn = (HttpURLConnection) stripeURL.openConnection();
            conn.setConnectTimeout(30 * 1000);
            conn.setReadTimeout(80 * 1000);
            conn.setUseCaches(false);

            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                conn.setRequestProperty(header.getKey(), header.getValue());
            }

            if (conn instanceof HttpsURLConnection) {
                ((HttpsURLConnection) conn).setSSLSocketFactory(SSL_SOCKET_FACTORY);
            }

            conn.setRequestMethod(request.method.code);

            if (StripeRequest.Method.POST == request.method) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", request.getContentType());

                try (OutputStream output = conn.getOutputStream()) {
                    output.write(getRequestOutputBytes(request));
                }
            }

            return conn;
        }

        @NonNull
        byte[] getRequestOutputBytes(@NonNull StripeRequest request)
                throws InvalidRequestException {
            try {
                return request.getOutputBytes();
            } catch (UnsupportedEncodingException e) {
                throw new InvalidRequestException("Unable to encode parameters to " + CHARSET
                        + ". Please contact support@stripe.com for assistance.",
                        null, null, 0, null, null, null, e);
            }
        }
    }
}
