package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.InvalidRequestException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Used by {@link StripeApiHandler} to make HTTP requests
 */
class RequestExecutor {
    static final String API_HOST = "https://api.stripe.com";
    static final String ANALYTICS_HOST = "https://q.stripe.com";
    static final String FINGERPRINTING_ENDPOINT = "https://m.stripe.com/4";

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

        @NonNull private final ApiVersion mApiVersion;

        @VisibleForTesting
        ConnectionFactory() {
            this(ApiVersion.getDefault());
        }

        private ConnectionFactory(@NonNull ApiVersion apiVersion) {
            mApiVersion = apiVersion;
        }

        @NonNull
        private HttpURLConnection create(@NonNull StripeRequest request)
                throws IOException, InvalidRequestException {
            final URL stripeURL = new URL(request.getUrl());
            final HttpURLConnection conn = (HttpURLConnection) stripeURL.openConnection();
            conn.setConnectTimeout(30 * 1000);
            conn.setReadTimeout(80 * 1000);
            conn.setUseCaches(false);

            if (request.urlStartsWith(API_HOST, ANALYTICS_HOST)) {
                for (Map.Entry<String, String> header :
                        request.getHeaders(mApiVersion).entrySet()) {
                    conn.setRequestProperty(header.getKey(), header.getValue());
                }
            }

            if (request.urlStartsWith(FINGERPRINTING_ENDPOINT)) {
                attachPseudoCookie(conn, request.options);
            }

            if (conn instanceof HttpsURLConnection) {
                ((HttpsURLConnection) conn).setSSLSocketFactory(SSL_SOCKET_FACTORY);
            }

            conn.setRequestMethod(request.method.code);

            if (StripeRequest.Method.POST == request.method) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", request.getContentType());

                OutputStream output = null;
                try {
                    output = conn.getOutputStream();
                    output.write(getOutputBytes(request));
                } finally {
                    if (output != null) {
                        output.close();
                    }
                }
            }

            return conn;
        }

        private void attachPseudoCookie(
                @NonNull HttpURLConnection connection,
                @NonNull RequestOptions options) {
            if (options.getGuid() != null && !TextUtils.isEmpty(options.getGuid())) {
                connection.setRequestProperty("Cookie", "m=" + options.getGuid());
            }
        }

        @NonNull
        private byte[] getOutputBytes(@NonNull StripeRequest request)
                throws InvalidRequestException {
            try {
                if (RequestOptions.RequestType.FINGERPRINTING == request.options.getRequestType()) {
                    final JSONObject jsonData = mapToJsonObject(request.params);
                    if (jsonData == null) {
                        throw new InvalidRequestException("Unable to create JSON data from " +
                                "parameters. Please contact support@stripe.com for assistance.",
                                null, null, 0, null, null, null, null);
                    }
                    return jsonData.toString().getBytes(CHARSET);
                } else {
                    return request.createQuery().getBytes(CHARSET);
                }
            } catch (UnsupportedEncodingException e) {
                throw new InvalidRequestException("Unable to encode parameters to " + CHARSET
                        + ". Please contact support@stripe.com for assistance.",
                        null, null, 0, null, null, null, e);
            }
        }

        /**
         * Converts a string-keyed {@link Map} into a {@link JSONObject}. This will cause a
         * {@link ClassCastException} if any sub-map has keys that are not {@link String Strings}.
         *
         * @param mapObject the {@link Map} that you'd like in JSON form
         * @return a {@link JSONObject} representing the input map, or {@code null} if the input
         * object is {@code null}
         */
        @Nullable
        @SuppressWarnings("unchecked")
        private JSONObject mapToJsonObject(@Nullable Map<String, ?> mapObject) {
            if (mapObject == null) {
                return null;
            }
            JSONObject jsonObject = new JSONObject();
            for (String key : mapObject.keySet()) {
                Object value = mapObject.get(key);
                if (value == null) {
                    continue;
                }

                try {
                    if (value instanceof Map<?, ?>) {
                        try {
                            Map<String, Object> mapValue = (Map<String, Object>) value;
                            jsonObject.put(key, mapToJsonObject(mapValue));
                        } catch (ClassCastException classCastException) {
                            // don't include the item in the JSONObject if the keys are not Strings
                        }
                    } else if (value instanceof List<?>) {
                        jsonObject.put(key, listToJsonArray((List<Object>) value));
                    } else if (value instanceof Number || value instanceof Boolean) {
                        jsonObject.put(key, value);
                    } else {
                        jsonObject.put(key, value.toString());
                    }
                } catch (JSONException jsonException) {
                    // Simply skip this value
                }
            }
            return jsonObject;
        }

        /**
         * Converts a {@link List} into a {@link JSONArray}. A {@link ClassCastException} will be
         * thrown if any object in the list (or any sub-list or sub-map) is a {@link Map} whose keys
         * are not {@link String Strings}.
         *
         * @param values a {@link List} of values to be put in a {@link JSONArray}
         * @return a {@link JSONArray}, or {@code null} if the input was {@code null}
         */
        @Nullable
        @SuppressWarnings("unchecked")
        private JSONArray listToJsonArray(@Nullable List<?> values) {
            if (values == null) {
                return null;
            }

            final JSONArray jsonArray = new JSONArray();
            for (Object object : values) {
                if (object instanceof Map<?, ?>) {
                    // We are ignoring type erasure here and crashing on bad input.
                    // Now that this method is not public, we have more control on what is
                    // passed to it.
                    final Map<String, Object> mapObject = (Map<String, Object>) object;
                    jsonArray.put(mapToJsonObject(mapObject));
                } else if (object instanceof List<?>) {
                    jsonArray.put(listToJsonArray((List) object));
                } else if (object instanceof Number || object instanceof Boolean) {
                    jsonArray.put(object);
                } else {
                    jsonArray.put(object.toString());
                }
            }
            return jsonArray;
        }
    }

}
