package com.stripe.android;

import android.support.annotation.NonNull;

import com.stripe.android.exception.InvalidRequestException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

final class ConnectionFactory {
    private static final SSLSocketFactory SSL_SOCKET_FACTORY = new StripeSSLSocketFactory();

    @NonNull
    HttpURLConnection create(@NonNull StripeRequest request)
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
            throw new InvalidRequestException("Unable to encode parameters to " +
                    StandardCharsets.UTF_8.name()
                    + ". Please contact support@stripe.com for assistance.",
                    null, null, 0, null, null, null, e);
        }
    }
}
