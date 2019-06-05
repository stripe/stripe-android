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
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Used by {@link StripeApiHandler} to make HTTP requests
 */
class RequestExecutor {

    private static final String CHARSET = "UTF-8";

    @NonNull private final ConnectionFactory mConnectionFactory;

    RequestExecutor() {
        mConnectionFactory = new ConnectionFactory();
    }

    @NonNull
    StripeResponse execute(@NonNull StripeRequest request)
            throws APIConnectionException, InvalidRequestException {
        // HttpURLConnection verifies SSL cert by default
        HttpURLConnection conn = null;
        try {
            conn = mConnectionFactory.create(request);
            // trigger the request
            final int rCode = conn.getResponseCode();
            final String rBody;
            if (rCode >= 200 && rCode < 300) {
                rBody = getResponseBody(conn.getInputStream());
            } else {
                rBody = getResponseBody(conn.getErrorStream());
            }
            return new StripeResponse(rCode, rBody, conn.getHeaderFields());
        } catch (IOException e) {
            throw new APIConnectionException(
                    String.format(Locale.ENGLISH,
                            "IOException during API request to Stripe (%s): %s "
                                    + "Please check your internet connection and try again. "
                                    + "If this problem persists, you should check Stripe's "
                                    + "service status at https://twitter.com/stripestatus, "
                                    + "or let us know at support@stripe.com.",
                            StripeApiHandler.getTokensUrl(), e.getMessage()), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Nullable
    private String getResponseBody(@NonNull InputStream responseStream)
            throws IOException {
        //\A is the beginning of
        // the stream boundary
        final Scanner scanner = new Scanner(responseStream, CHARSET).useDelimiter("\\A");
        final String rBody = scanner.hasNext() ? scanner.next() : null;
        responseStream.close();
        return rBody;
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
