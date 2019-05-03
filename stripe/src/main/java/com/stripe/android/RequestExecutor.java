package com.stripe.android;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
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
    static final String LIVE_API_BASE = "https://api.stripe.com";
    static final String LIVE_LOGGING_BASE = "https://q.stripe.com";
    static final String LOGGING_ENDPOINT = "https://m.stripe.com/4";

    private static final String CHARSET = "UTF-8";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({RestMethod.GET, RestMethod.POST, RestMethod.DELETE})
    @interface RestMethod {
        String GET = "GET";
        String POST = "POST";
        String DELETE = "DELETE";
    }

    @NonNull private final ConnectionFactory mConnectionFactory;

    RequestExecutor() {
        mConnectionFactory = new ConnectionFactory();
    }

    @NonNull
    StripeResponse execute(
            @RestMethod @NonNull String method,
            @NonNull String url,
            @Nullable Map<String, ?> params,
            @NonNull RequestOptions options)
            throws APIConnectionException, InvalidRequestException {
        // HttpURLConnection verifies SSL cert by default
        HttpURLConnection conn = null;
        try {
            switch (method) {
                case RestMethod.GET: {
                    conn = mConnectionFactory.create(RestMethod.GET, url, params, options);
                    break;
                }
                case RestMethod.POST: {
                    conn = mConnectionFactory.create(RestMethod.POST, url, params, options);
                    break;
                }
                case RestMethod.DELETE: {
                    conn = mConnectionFactory.create(RestMethod.DELETE, url, null, options);
                    break;
                }
                default: {
                    throw new APIConnectionException(String.format(Locale.ENGLISH,
                            "Unrecognized HTTP method %s. "
                                    + "This indicates a bug in the Stripe bindings. "
                                    + "Please contact support@stripe.com for assistance.",
                            method));
                }
            }
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
        private HttpURLConnection create(@RestMethod @NonNull String method,
                                         @NonNull String url,
                                         @Nullable Map<String, ?> params,
                                         @NonNull RequestOptions options)
                throws IOException, InvalidRequestException {
            final URL stripeURL = new URL(getUrl(method, url, params));
            final HttpURLConnection conn = (HttpURLConnection) stripeURL.openConnection();
            conn.setConnectTimeout(30 * 1000);
            conn.setReadTimeout(80 * 1000);
            conn.setUseCaches(false);

            if (urlNeedsHeaderData(url)) {
                for (Map.Entry<String, String> header : getHeaders(options).entrySet()) {
                    conn.setRequestProperty(header.getKey(), header.getValue());
                }
            }

            if (urlNeedsPseudoCookie(url)) {
                attachPseudoCookie(conn, options);
            }

            if (conn instanceof HttpsURLConnection) {
                ((HttpsURLConnection) conn).setSSLSocketFactory(SSL_SOCKET_FACTORY);
            }

            conn.setRequestMethod(method);

            if (RestMethod.POST.equals(method)) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", getContentType(options));

                OutputStream output = null;
                try {
                    output = conn.getOutputStream();
                    output.write(getOutputBytes(params, options));
                } finally {
                    if (output != null) {
                        output.close();
                    }
                }
            }

            return conn;
        }

        @NonNull
        private String getUrl(@RestMethod @NonNull String method,
                              @NonNull String url,
                              @Nullable Map<String, ?> params)
                throws UnsupportedEncodingException, InvalidRequestException {
            if (RestMethod.GET.equals(method)) {
                return formatUrl(url, createQuery(params));
            }

            return url;
        }

        @NonNull
        private String formatUrl(@NonNull String url, @Nullable String query) {
            if (query == null || query.isEmpty()) {
                return url;
            } else {
                // In some cases, URL can already contain a question mark
                // (eg, upcoming invoice lines)
                final String separator = url.contains("?") ? "&" : "?";
                return String.format(Locale.ROOT, "%s%s%s", url, separator, query);
            }
        }

        private boolean urlNeedsHeaderData(@NonNull String url) {
            return url.startsWith(LIVE_API_BASE) || url.startsWith(LIVE_LOGGING_BASE);
        }

        private boolean urlNeedsPseudoCookie(@NonNull String url) {
            return url.startsWith(LOGGING_ENDPOINT);
        }

        private void attachPseudoCookie(
                @NonNull HttpURLConnection connection,
                @NonNull RequestOptions options) {
            if (options.getGuid() != null && !TextUtils.isEmpty(options.getGuid())) {
                connection.setRequestProperty("Cookie", "m=" + options.getGuid());
            }
        }

        @NonNull
        private String getContentType(@NonNull RequestOptions options) {
            if (RequestOptions.TYPE_JSON.equals(options.getRequestType())) {
                return String.format(Locale.ROOT, "application/json; charset=%s", CHARSET);
            } else {
                return String.format(Locale.ROOT, "application/x-www-form-urlencoded;charset=%s",
                        CHARSET);
            }
        }

        @NonNull
        private byte[] getOutputBytes(
                @Nullable Map<String, ?> params,
                @NonNull RequestOptions options) throws InvalidRequestException {
            try {
                if (RequestOptions.TYPE_JSON.equals(options.getRequestType())) {
                    JSONObject jsonData = mapToJsonObject(params);
                    if (jsonData == null) {
                        throw new InvalidRequestException("Unable to create JSON data from " +
                                "parameters. Please contact support@stripe.com for assistance.",
                                null, null, 0, null, null, null, null);
                    }
                    return jsonData.toString().getBytes(CHARSET);
                } else {
                    return createQuery(params).getBytes(CHARSET);
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

        @NonNull
        String createQuery(@Nullable Map<String, ?> params)
                throws UnsupportedEncodingException, InvalidRequestException {
            final StringBuilder queryStringBuffer = new StringBuilder();
            final List<Parameter> flatParams = flattenParams(params);

            for (Parameter flatParam : flatParams) {
                if (queryStringBuffer.length() > 0) {
                    queryStringBuffer.append("&");
                }
                queryStringBuffer.append(urlEncodePair(flatParam.key, flatParam.value));
            }

            return queryStringBuffer.toString();
        }

        @NonNull
        private List<Parameter> flattenParams(@Nullable Map<String, ?> params)
                throws InvalidRequestException {
            return flattenParamsMap(params, null);
        }

        @NonNull
        private List<Parameter> flattenParamsList(@NonNull List<?> params,
                                                  @NonNull String keyPrefix)
                throws InvalidRequestException {
            final List<Parameter> flatParams = new LinkedList<>();

            // Because application/x-www-form-urlencoded cannot represent an empty
            // list, convention is to take the list parameter and just set it to an
            // empty string. (e.g. A regular list might look like `a[]=1&b[]=2`.
            // Emptying it would look like `a=`.)
            if (params.isEmpty()) {
                flatParams.add(new Parameter(keyPrefix, ""));
            } else {
                final String newPrefix = String.format(Locale.ROOT, "%s[]", keyPrefix);
                for (Object param : params) {
                    flatParams.addAll(flattenParamsValue(param, newPrefix));
                }
            }

            return flatParams;
        }

        @NonNull
        private List<Parameter> flattenParamsMap(@Nullable Map<String, ?> params,
                                                 @Nullable String keyPrefix)
                throws InvalidRequestException {
            final List<Parameter> flatParams = new LinkedList<>();
            if (params == null) {
                return flatParams;
            }

            for (Map.Entry<String, ?> entry : params.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                final String newPrefix;
                if (keyPrefix != null) {
                    newPrefix = String.format(Locale.ROOT, "%s[%s]", keyPrefix, key);
                } else {
                    newPrefix = key;
                }

                flatParams.addAll(flattenParamsValue(value, newPrefix));
            }

            return flatParams;
        }

        @NonNull
        private List<Parameter> flattenParamsValue(@Nullable Object value,
                                                   @NonNull String keyPrefix)
                throws InvalidRequestException {
            final List<Parameter> flatParams;
            if (value instanceof Map<?, ?>) {
                flatParams = flattenParamsMap((Map<String, Object>) value, keyPrefix);
            } else if (value instanceof List<?>) {
                flatParams = flattenParamsList((List<?>) value, keyPrefix);
            } else if ("".equals(value)) {
                throw new InvalidRequestException("You cannot set '" + keyPrefix + "' to an empty "
                        + "string. " + "We interpret empty strings as null in requests. "
                        + "You may set '" + keyPrefix + "' to null to delete the property.",
                        keyPrefix, null, 0, null, null, null, null);
            } else if (value == null) {
                flatParams = new LinkedList<>();
                flatParams.add(new Parameter(keyPrefix, ""));
            } else {
                flatParams = new LinkedList<>();
                flatParams.add(new Parameter(keyPrefix, value.toString()));
            }

            return flatParams;
        }

        @NonNull
        private String urlEncodePair(@NonNull String k, @NonNull String v)
                throws UnsupportedEncodingException {
            return String.format(Locale.ROOT, "%s=%s", urlEncode(k), urlEncode(v));
        }

        @Nullable
        private String urlEncode(@Nullable String str) throws UnsupportedEncodingException {
            // Preserve original behavior that passing null for an object id will lead
            // to us actually making a request to /v1/foo/null
            if (str == null) {
                return null;
            } else {
                return URLEncoder.encode(str, CHARSET);
            }
        }

        @NonNull
        @VisibleForTesting
        Map<String, String> getHeaders(@NonNull RequestOptions options) {
            final Map<String, String> headers = new HashMap<>();
            headers.put("Accept-Charset", CHARSET);
            headers.put("Accept", "application/json");
            headers.put("User-Agent",
                    String.format(Locale.ROOT, "Stripe/v1 AndroidBindings/%s",
                            BuildConfig.VERSION_NAME));

            headers.put("Authorization", String.format(Locale.ENGLISH,
                    "Bearer %s", options.getPublishableApiKey()));

            // debug headers
            final AbstractMap<String, String> propertyMap = new HashMap<>();
            propertyMap.put("java.version", System.getProperty("java.version"));
            propertyMap.put("os.name", "android");
            propertyMap.put("os.version", String.valueOf(Build.VERSION.SDK_INT));
            propertyMap.put("bindings.version", BuildConfig.VERSION_NAME);
            propertyMap.put("lang", "Java");
            propertyMap.put("publisher", "Stripe");

            final JSONObject headerMappingObject = new JSONObject(propertyMap);
            headers.put("X-Stripe-Client-User-Agent", headerMappingObject.toString());
            headers.put("Stripe-Version", mApiVersion.getCode());

            if (options.getStripeAccount() != null) {
                headers.put("Stripe-Account", options.getStripeAccount());
            }

            if (options.getIdempotencyKey() != null) {
                headers.put("Idempotency-Key", options.getIdempotencyKey());
            }

            return headers;
        }
    }

    private static final class Parameter {
        @NonNull private final String key;
        @NonNull private final String value;

        Parameter(@NonNull String key, @NonNull String value) {
            this.key = key;
            this.value = value;
        }
    }
}
