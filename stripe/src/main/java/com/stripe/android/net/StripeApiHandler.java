package com.stripe.android.net;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.stripe.android.BuildConfig;
import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.PermissionException;
import com.stripe.android.exception.RateLimitException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.Token;
import com.stripe.android.util.LoggingUtils;
import com.stripe.android.util.StripeJsonUtils;
import com.stripe.android.util.StripeNetworkUtils;
import com.stripe.android.util.StripeTextUtils;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Handler for calls to the Stripe API.
 */
public class StripeApiHandler {

    public static final String LIVE_API_BASE = "https://api.stripe.com";
    public static final String LIVE_LOGGING_BASE = "https://q.stripe.com";
    public static final String CHARSET = "UTF-8";
    public static final String TOKENS = "tokens";
    public static final String SOURCES = "sources";

    private static final String LOGGING_ENDPOINT = "https://m.stripe.com/4";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            GET,
            POST
    })
    @interface RestMethod { }
    static final String GET = "GET";
    static final String POST = "POST";

    private static final String DNS_CACHE_TTL_PROPERTY_NAME = "networkaddress.cache.ttl";
    private static final SSLSocketFactory SSL_SOCKET_FACTORY = new StripeSSLSocketFactory();

    /**
     * Create a {@link Source} using the input {@link SourceParams}.
     *
     * @deprecated As of v4.0.3 - will not be carried into 5+ and beyond
     *
     * @param sourceParams a {@link SourceParams} object with {@link Source} creation params
     * @param publishableKey an API key
     * @return a {@link Source} if one could be created from the input params,
     * or {@code null} if not
     * @throws AuthenticationException if there is a problem authenticating to the Stripe API
     * @throws InvalidRequestException if one or more of the parameters is incorrect
     * @throws APIConnectionException if there is a problem connecting to the Stripe API
     * @throws APIException for unknown Stripe API errors. These should be rare.
     */
    @Deprecated
    @Nullable
    public static Source createSourceOnServer(
            @NonNull SourceParams sourceParams,
            @NonNull String publishableKey)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        return createSourceOnServer(sourceParams, publishableKey, null);
    }

    /**
     * Create a {@link Source} using the input {@link SourceParams}.
     *
     * @param context a {@link Context} object for aquiring resources
     * @param sourceParams a {@link SourceParams} object with {@link Source} creation params
     * @param publishableKey an API key
     * @return a {@link Source} if one could be created from the input params,
     * or {@code null} if not
     * @throws AuthenticationException if there is a problem authenticating to the Stripe API
     * @throws InvalidRequestException if one or more of the parameters is incorrect
     * @throws APIConnectionException if there is a problem connecting to the Stripe API
     * @throws APIException for unknown Stripe API errors. These should be rare.
     */
    @Nullable
    public static Source createSourceOnServer(
            @NonNull Context context,
            @NonNull SourceParams sourceParams,
            @NonNull String publishableKey)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        return createSourceOnServer(null, context, sourceParams, publishableKey, null);
    }

    @Nullable
    public static Source createSourceOnServer(
            @Nullable StripeNetworkUtils.UidProvider uidProvider,
            @NonNull Context context,
            @NonNull SourceParams sourceParams,
            @NonNull String publishableKey,
            @Nullable LoggingResponseListener loggingResponseListener)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        Map<String, Object> paramMap = sourceParams.toParamMap();
        StripeNetworkUtils.addUidParams(uidProvider, context, paramMap);
        RequestOptions options = RequestOptions.builder(publishableKey).build();

        try {
            String apiKey = options.getPublishableApiKey();
            if (StripeTextUtils.isBlank(apiKey)) {
                return null;
            }

            setTelemetryData(context, loggingResponseListener);
            Map<String, Object> loggingParams = LoggingUtils.getSourceCreationParams(
                    apiKey,
                    sourceParams.getType());
            RequestOptions loggingOptions = RequestOptions.builder(publishableKey).build();
            logApiCall(loggingParams, loggingOptions, loggingResponseListener);
            return Source.fromString(requestData(POST, getSourcesUrl(), paramMap, options));
        } catch (CardException unexpected) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw new APIException(
                    unexpected.getMessage(),
                    unexpected.getRequestId(),
                    unexpected.getStatusCode(),
                    unexpected);
        }
    }

    public static void setTelemetryData(@NonNull Context context,
                                        @Nullable LoggingResponseListener listener) {
        Map<String, Object> telemetry = TelemetryClientUtil.createTelemetryMap(context);
        StripeNetworkUtils.removeNullAndEmptyParams(telemetry);
        if (listener != null && !listener.shouldLogTest()) {
            return;
        }

        RequestOptions options =
                RequestOptions.builder(null, RequestOptions.TYPE_JSON)
                        .setGuid(TelemetryClientUtil.getHashedId(context))
                        .build();
        fireAndForgetApiCall(telemetry, LOGGING_ENDPOINT, POST, options, listener);
    }

    /**
     * Create a {@link Source} using the input {@link SourceParams}.
     *
     * @param sourceParams a {@link SourceParams} object with {@link Source} creation params
     * @param publishableKey an API key
     * @param loggingResponseListener a {@link LoggingResponseListener} to verify logging
     * @return a {@link Source} if one could be created from the input params,
     * or {@code null} if not
     * @throws AuthenticationException if there is a problem authenticating to the Stripe API
     * @throws InvalidRequestException if one or more of the parameters is incorrect
     * @throws APIConnectionException if there is a problem connecting to the Stripe API
     * @throws APIException for unknown Stripe API errors. These should be rare.
     */
    @Deprecated
    @Nullable
    static Source createSourceOnServer(
            @NonNull SourceParams sourceParams,
            @NonNull String publishableKey,
            @Nullable LoggingResponseListener loggingResponseListener)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        Map<String, Object> paramMap = sourceParams.toParamMap();
        RequestOptions options = RequestOptions.builder(publishableKey).build();

        try {
            String apiKey = options.getPublishableApiKey();
            if (StripeTextUtils.isBlank(apiKey)) {
                return null;
            }

            Map<String, Object> loggingParams = LoggingUtils.getSourceCreationParams(
                    apiKey,
                    sourceParams.getType());
            RequestOptions loggingOptions = RequestOptions.builder(publishableKey).build();
            logApiCall(loggingParams, loggingOptions, loggingResponseListener);
            return Source.fromString(requestData(POST, getSourcesUrl(), paramMap, options));
        } catch (CardException unexpected) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw new APIException(
                    unexpected.getMessage(),
                    unexpected.getRequestId(),
                    unexpected.getStatusCode(),
                    unexpected);
        }
    }

    /**
     * Retrieve an existing {@link Source} object from the server.
     *
     * @param sourceId the {@link Source#mId} field for the Source to query
     * @param clientSecret the {@link Source#mClientSecret} field for the Source to query
     * @param publishableKey an API key
     * @return a {@link Source} if one could be retrieved for the input params, or {@code null} if
     * no such Source could be found.
     *
     * @throws AuthenticationException if there is a problem authenticating to the Stripe API
     * @throws InvalidRequestException if one or more of the parameters is incorrect
     * @throws APIConnectionException if there is a problem connecting to the Stripe API
     * @throws APIException for unknown Stripe API errors. These should be rare.
     */
    public static Source retrieveSource(
            @NonNull String sourceId,
            @NonNull String clientSecret,
            @NonNull String publishableKey)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {

        Map<String, Object> paramMap = SourceParams.createRetrieveSourceParams(clientSecret);
        RequestOptions options = RequestOptions.builder(publishableKey).build();
        try {
            return Source.fromString(
                    requestData(GET, getRetrieveSourceApiUrl(sourceId), paramMap, options));
        } catch (CardException unexpected) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw new APIException(
                    unexpected.getMessage(),
                    unexpected.getRequestId(),
                    unexpected.getStatusCode(),
                    unexpected);
        }
    }

    /**
     * Poll for changes in a {@link Source} using a background thread with an exponential backoff.
     *
     * @param sourceId the {@link Source#mId} to check on
     * @param clientSecret the {@link Source#mClientSecret} to check on
     * @param publishableKey an API key
     * @param callback a {@link PollingResponseHandler} to use as a callback
     * @param timeoutMs the amount of time before the polling expires. If {@code null} is passed
     *                  in, 10000ms will be used.
     */
    public static void pollSource(
            @NonNull final String sourceId,
            @NonNull final String clientSecret,
            @NonNull final String publishableKey,
            @NonNull final PollingResponseHandler callback,
            @Nullable Integer timeoutMs) {

        PollingNetworkHandler networkHandler =
                new PollingNetworkHandler(
                        sourceId,
                        clientSecret,
                        publishableKey,
                        callback,
                        timeoutMs,
                        null,
                        PollingParameters.generateDefaultParameters());
        networkHandler.start();
    }

    /**
     * Polls for source updates synchronously. If called on the main thread,
     * this will crash the application.
     *
     * @param sourceId the {@link Source#mId ID} of the Source being polled
     * @param clientSecret the {@link Source#mClientSecret client_secret} of the Source
     * @param publishableKey a public API key
     * @param timeoutMs the amount of time before the polling expires. If {@code null} is passed
     *                  in, 10000ms will be used.
     * @return a {@link PollingResponse} that will indicate success or failure
     */
    public static PollingResponse pollSourceSynchronous(
            @NonNull final String sourceId,
            @NonNull final String clientSecret,
            @NonNull final String publishableKey,
            @Nullable Integer timeoutMs) {
        PollingSyncNetworkHandler pollingSyncNetworkHandler =
                new PollingSyncNetworkHandler(
                        sourceId,
                        clientSecret,
                        publishableKey,
                        timeoutMs,
                        null,
                        null,
                        PollingParameters.generateDefaultParameters());
        return pollingSyncNetworkHandler.pollForSourceUpdate();
    }

    /**
     * Create a {@link Token} using the input card parameters.
     *
     * @param cardParams a mapped set of parameters representing the object for which this token
     *                   is being created
     * @param options a {@link RequestOptions} object that contains connection data like the api
     *                key, api version, etc
     * @param listener a {@link LoggingResponseListener} useful for testing logging calls
     *
     * @return a {@link Token} that can be used to perform other operations with this card
     * @throws AuthenticationException if there is a problem authenticating to the Stripe API
     * @throws InvalidRequestException if one or more of the parameters is incorrect
     * @throws APIConnectionException if there is a problem connecting to the Stripe API
     * @throws CardException if there is a problem with the card information
     * @throws APIException for unknown Stripe API errors. These should be rare.
     */
    @Nullable
    @Deprecated
    @SuppressWarnings("unchecked")
    public static Token createTokenOnServer(
            @NonNull Map<String, Object> cardParams,
            @NonNull RequestOptions options,
            @NonNull @Token.TokenType String tokenType,
            @Nullable LoggingResponseListener listener)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {

        try {
            String apiKey = options.getPublishableApiKey();
            if (StripeTextUtils.isBlank(apiKey)) {
                return null;
            }

            List<String> loggingTokens =
                    (List<String>) cardParams.get(LoggingUtils.FIELD_PRODUCT_USAGE);
            cardParams.remove(LoggingUtils.FIELD_PRODUCT_USAGE);

            Map<String, Object> loggingParams =
                    LoggingUtils.getTokenCreationParams(loggingTokens, apiKey, tokenType);
            logApiCall(loggingParams, options, listener);
        } catch (ClassCastException classCastEx) {
            // This can only happen if someone puts a weird object in the map.
            cardParams.remove(LoggingUtils.FIELD_PRODUCT_USAGE);
        }

        return requestToken(POST, getApiUrl(), cardParams, options);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static Token createTokenOnServer(
            @NonNull Context context,
            @NonNull Map<String, Object> cardParams,
            @NonNull RequestOptions options,
            @NonNull @Token.TokenType String tokenType,
            @Nullable LoggingResponseListener listener)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {

        try {
            String apiKey = options.getPublishableApiKey();
            if (StripeTextUtils.isBlank(apiKey)) {
                return null;
            }

            List<String> loggingTokens =
                    (List<String>) cardParams.get(LoggingUtils.FIELD_PRODUCT_USAGE);
            cardParams.remove(LoggingUtils.FIELD_PRODUCT_USAGE);

            setTelemetryData(context, listener);

            Map<String, Object> loggingParams =
                    LoggingUtils.getTokenCreationParams(loggingTokens, apiKey, tokenType);
            logApiCall(loggingParams, options, listener);
        } catch (ClassCastException classCastEx) {
            // This can only happen if someone puts a weird object in the map.
            cardParams.remove(LoggingUtils.FIELD_PRODUCT_USAGE);
        }

        return requestToken(POST, getApiUrl(), cardParams, options);
    }

    /**
     * Retrieve a {@link Token} by its ID.
     *
     * @param options a {@link RequestOptions} object that contains
     * @param tokenId the id of the token that you're looking for
     * @return a {@link Token} if one exists and you have the permissions to access it
     * @throws AuthenticationException if there is a problem authenticating to the Stripe API
     * @throws InvalidRequestException if the token ID parameter is invalid
     * @throws APIConnectionException if there is a problem connecting to the Stripe API
     * @throws APIException for unknown Stripe API errors. These should be rare.
     */
    @Nullable
    public static Token retrieveTokenFromServer(
            @NonNull RequestOptions options,
            @NonNull String tokenId)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        try {
            return requestToken(GET, getRetrieveTokenApiUrl(tokenId), null, options);
        } catch (CardException cardException) {
            // It shouldn't be possible to throw a CardException from the retrieve token method.
            throw new APIException(
                    cardException.getMessage(),
                    cardException.getRequestId(),
                    cardException.getStatusCode(),
                    cardException);
        }
    }

    static String createQuery(Map<String, Object> params)
            throws UnsupportedEncodingException, InvalidRequestException {
        StringBuilder queryStringBuffer = new StringBuilder();
        List<Parameter> flatParams = flattenParams(params);
        Iterator<Parameter> it = flatParams.iterator();

        while (it.hasNext()) {
            if (queryStringBuffer.length() > 0) {
                queryStringBuffer.append("&");
            }
            Parameter param = it.next();
            queryStringBuffer.append(urlEncodePair(param.key, param.value));
        }

        return queryStringBuffer.toString();
    }

    static Map<String, String> getHeaders(RequestOptions options) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept-Charset", CHARSET);
        headers.put("Accept", "application/json");
        headers.put("User-Agent",
                String.format("Stripe/v1 AndroidBindings/%s", BuildConfig.VERSION_NAME));

        if (options != null) {
            headers.put("Authorization", String.format(Locale.ENGLISH,
                    "Bearer %s", options.getPublishableApiKey()));
        }

        // debug headers
        Map<String, String> propertyMap = new HashMap<>();

        final String systemPropertyName = "java.version";
        propertyMap.put(systemPropertyName, System.getProperty(systemPropertyName));
        propertyMap.put("os.name", "android");
        propertyMap.put("os.version", String.valueOf(Build.VERSION.SDK_INT));
        propertyMap.put("bindings.version", BuildConfig.VERSION_NAME);
        propertyMap.put("lang", "Java");
        propertyMap.put("publisher", "Stripe");
        JSONObject headerMappingObject = new JSONObject(propertyMap);
        headers.put("X-Stripe-Client-User-Agent", headerMappingObject.toString());

        if (options != null && options.getApiVersion() != null) {
            headers.put("Stripe-Version", options.getApiVersion());
        }

        if (options != null && options.getIdempotencyKey() != null) {
            headers.put("Idempotency-Key", options.getIdempotencyKey());
        }

        return headers;
    }

    @VisibleForTesting
    static String getApiUrl() {
        return String.format(Locale.ENGLISH, "%s/v1/%s", LIVE_API_BASE, TOKENS);
    }

    @VisibleForTesting
    static String getSourcesUrl() {
        return String.format(Locale.ENGLISH, "%s/v1/%s", LIVE_API_BASE, SOURCES);
    }

    @VisibleForTesting
    static String getRetrieveSourceApiUrl(@NonNull String sourceId) {
        return String.format(Locale.ENGLISH, "%s/%s", getSourcesUrl(), sourceId);
    }

    @VisibleForTesting
    static String getRetrieveTokenApiUrl(@NonNull String tokenId) {
        return String.format("%s/%s", getApiUrl(), tokenId);
    }

    private static String formatURL(String url, String query) {
        if (query == null || query.isEmpty()) {
            return url;
        } else {
            // In some cases, URL can already contain a question mark (eg, upcoming invoice lines)
            String separator = url.contains("?") ? "&" : "?";
            return String.format("%s%s%s", url, separator, query);
        }
    }

    private static java.net.HttpURLConnection createGetConnection(
            String url, String query, RequestOptions options) throws IOException {
        String getURL = formatURL(url, query);
        java.net.HttpURLConnection conn = createStripeConnection(getURL, options);
        conn.setRequestMethod(GET);

        return conn;
    }

    private static java.net.HttpURLConnection createPostConnection(
            @NonNull String url,
            @NonNull Map<String, Object> params,
            @NonNull RequestOptions options) throws IOException, InvalidRequestException {
        java.net.HttpURLConnection conn = createStripeConnection(url, options);

        conn.setDoOutput(true);
        conn.setRequestMethod(POST);
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
        return conn;
    }

    private static String getContentType(@NonNull RequestOptions options) {
        if (RequestOptions.TYPE_JSON.equals(options.getRequestType())) {
            return String.format(
                    "application/json; charset=%s", CHARSET);
        } else {
            return String.format(
                    "application/x-www-form-urlencoded;charset=%s", CHARSET);
        }
    }

    private static byte[] getOutputBytes(
            @NonNull Map<String, Object> params,
            @NonNull RequestOptions options) throws InvalidRequestException {
        try {
            if (RequestOptions.TYPE_JSON.equals(options.getRequestType())) {
                JSONObject jsonData = StripeJsonUtils.mapToJsonObject(params);
                if (jsonData == null) {
                    throw new InvalidRequestException("Unable to create JSON data from parameters. "
                            + "Please contact support@stripe.com for assistance.",
                            null, null, 0, null);
                }
                return jsonData.toString().getBytes(CHARSET);
            } else {
                String query = createQuery(params);
                return query.getBytes(CHARSET);
            }
        } catch (UnsupportedEncodingException e) {
            throw new InvalidRequestException("Unable to encode parameters to "
                    + CHARSET
                    + ". Please contact support@stripe.com for assistance.",
                    null, null, 0, e);
        }
    }

    private static java.net.HttpURLConnection createStripeConnection(
            String url,
            RequestOptions options)
            throws IOException {
        URL stripeURL;

        stripeURL = new URL(url);
        HttpURLConnection conn  = (HttpURLConnection) stripeURL.openConnection();
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

        return conn;
    }

    private static boolean urlNeedsHeaderData(@NonNull String url) {
        return url.startsWith(LIVE_API_BASE) || url.startsWith(LIVE_LOGGING_BASE);
    }

    private static boolean urlNeedsPseudoCookie(@NonNull String url) {
        return url.startsWith(LOGGING_ENDPOINT);
    }

    private static void attachPseudoCookie(
            @NonNull HttpURLConnection connection,
            @NonNull RequestOptions options) {
        if (options.getGuid() != null && !TextUtils.isEmpty(options.getGuid())) {
            connection.setRequestProperty("Cookie", "m=" + options.getGuid());
        }
    }

    public static void logApiCall(
            @NonNull Map<String, Object> loggingMap,
            @Nullable RequestOptions options,
            @Nullable LoggingResponseListener listener) {
        if (options == null) {
            return;
        }

        if (listener != null && !listener.shouldLogTest()) {
            return;
        }

        String apiKey = options.getPublishableApiKey();
        if (apiKey.trim().isEmpty()) {
            // if there is no apiKey associated with the request, we don't need to react here.
            return;
        }

        fireAndForgetApiCall(loggingMap, LIVE_LOGGING_BASE, GET, options, listener);
    }

    private static void fireAndForgetApiCall(
            @NonNull Map<String, Object> paramMap,
            @NonNull String url,
            @NonNull @RestMethod String method,
            @Nullable RequestOptions options,
            @Nullable LoggingResponseListener listener) {
        String originalDNSCacheTTL = null;
        Boolean allowedToSetTTL = true;

        try {
            originalDNSCacheTTL = java.security.Security
                    .getProperty(DNS_CACHE_TTL_PROPERTY_NAME);
            // disable DNS cache
            java.security.Security
                    .setProperty(DNS_CACHE_TTL_PROPERTY_NAME, "0");
        } catch (SecurityException se) {
            allowedToSetTTL = false;
        }

        try {
            StripeResponse response = getStripeResponse(
                    method,
                    url,
                    paramMap,
                    options);

            if (listener != null) {
                listener.onLoggingResponse(response);
            }
        } catch(StripeException stripeException) {
            if (listener != null) {
                listener.onStripeException(stripeException);
            }
            // We're just logging. No need to crash here or attempt to re-log things.
        } finally {
            if (allowedToSetTTL) {
                if (originalDNSCacheTTL == null) {
                    // value unspecified by implementation
                    // DNS_CACHE_TTL_PROPERTY_NAME of -1 = cache forever
                    java.security.Security.setProperty(
                            DNS_CACHE_TTL_PROPERTY_NAME, "-1");
                } else {
                    java.security.Security.setProperty(
                            DNS_CACHE_TTL_PROPERTY_NAME, originalDNSCacheTTL);
                }
            }
        }
    }

    private static String requestData(
            @RestMethod String method,
            String url,
            Map<String, Object> params,
            RequestOptions options)
            throws AuthenticationException, InvalidRequestException,
            APIConnectionException, CardException, APIException {

        if (options == null) {
            return null;
        }

        String originalDNSCacheTTL = null;
        Boolean allowedToSetTTL = true;

        try {
            originalDNSCacheTTL = java.security.Security
                    .getProperty(DNS_CACHE_TTL_PROPERTY_NAME);
            // disable DNS cache
            java.security.Security
                    .setProperty(DNS_CACHE_TTL_PROPERTY_NAME, "0");
        } catch (SecurityException se) {
            allowedToSetTTL = false;
        }

        String apiKey = options.getPublishableApiKey();
        if (apiKey.trim().isEmpty()) {
            throw new AuthenticationException(
                    "No API key provided. (HINT: set your API key using 'Stripe.apiKey = <API-KEY>'. "
                            + "You can generate API keys from the Stripe web interface. "
                            + "See https://stripe.com/api for details or email support@stripe.com if you have questions.",
                    null, 0);
        }

        StripeResponse response = getStripeResponse(method, url, params, options);

        int rCode = response.getResponseCode();
        String rBody = response.getResponseBody();

        String requestId = null;
        Map<String, List<String>> headers = response.getResponseHeaders();
        List<String> requestIdList = headers == null ? null : headers.get("Request-Id");
        if (requestIdList != null && requestIdList.size() > 0) {
            requestId = requestIdList.get(0);
        }

        if (rCode < 200 || rCode >= 300) {
            handleAPIError(rBody, rCode, requestId);
        }

        if (allowedToSetTTL) {
            if (originalDNSCacheTTL == null) {
                // value unspecified by implementation
                // DNS_CACHE_TTL_PROPERTY_NAME of -1 = cache forever
                java.security.Security.setProperty(
                        DNS_CACHE_TTL_PROPERTY_NAME, "-1");
            } else {
                java.security.Security.setProperty(
                        DNS_CACHE_TTL_PROPERTY_NAME, originalDNSCacheTTL);
            }
        }
        return rBody;
    }

    private static Token requestToken(
            @RestMethod String method,
            String url,
            Map<String, Object> params,
            RequestOptions options)
            throws AuthenticationException, InvalidRequestException,
            APIConnectionException, CardException, APIException {
        try {
            return TokenParser.parseToken(requestData(method, url, params, options));
        } catch (JSONException ignored) {
            return null;
        }
    }

    private static StripeResponse getStripeResponse(
            @RestMethod String method,
            String url,
            Map<String, Object> params,
            RequestOptions options)
            throws InvalidRequestException, APIConnectionException, APIException {
        // HTTPSURLConnection verifies SSL cert by default
        java.net.HttpURLConnection conn = null;
        try {
            switch (method) {
                case GET:
                    conn = createGetConnection(url, createQuery(params), options);
                    break;
                case POST:
                    conn = createPostConnection(url, params, options);
                    break;
                default:
                    throw new APIConnectionException(
                            String.format(
                                    "Unrecognized HTTP method %s. "
                                            + "This indicates a bug in the Stripe bindings. "
                                            + "Please contact support@stripe.com for assistance.",
                                    method));
            }
            // trigger the request
            int rCode = conn.getResponseCode();
            String rBody;
            Map<String, List<String>> headers;

            if (rCode >= 200 && rCode < 300) {
                rBody = getResponseBody(conn.getInputStream());
            } else {
                rBody = getResponseBody(conn.getErrorStream());
            }
            headers = conn.getHeaderFields();
            return new StripeResponse(rCode, rBody, headers);

        } catch (IOException e) {
            throw new APIConnectionException(
                    String.format(
                            "IOException during API request to Stripe (%s): %s "
                                    + "Please check your internet connection and try again. "
                                    + "If this problem persists, you should check Stripe's "
                                    + "service status at https://twitter.com/stripestatus, "
                                    + "or let us know at support@stripe.com.",
                            getApiUrl(), e.getMessage()), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static List<Parameter> flattenParams(Map<String, Object> params)
            throws InvalidRequestException {
        return flattenParamsMap(params, null);
    }

    private static List<Parameter> flattenParamsList(List<Object> params, String keyPrefix)
            throws InvalidRequestException {
        List<Parameter> flatParams = new LinkedList<Parameter>();
        Iterator<?> it = ((List<?>)params).iterator();
        String newPrefix = String.format("%s[]", keyPrefix);

        // Because application/x-www-form-urlencoded cannot represent an empty
        // list, convention is to take the list parameter and just set it to an
        // empty string. (e.g. A regular list might look like `a[]=1&b[]=2`.
        // Emptying it would look like `a=`.)
        if (params.isEmpty()) {
            flatParams.add(new Parameter(keyPrefix, ""));
        } else {
            while (it.hasNext()) {
                flatParams.addAll(flattenParamsValue(it.next(), newPrefix));
            }
        }

        return flatParams;
    }

    private static List<Parameter> flattenParamsMap(Map<String, Object> params, String keyPrefix)
            throws InvalidRequestException {
        List<Parameter> flatParams = new LinkedList<Parameter>();
        if (params == null) {
            return flatParams;
        }

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            String newPrefix = key;
            if (keyPrefix != null) {
                newPrefix = String.format("%s[%s]", keyPrefix, key);
            }

            flatParams.addAll(flattenParamsValue(value, newPrefix));
        }

        return flatParams;
    }

    private static List<Parameter> flattenParamsValue(Object value, String keyPrefix)
            throws InvalidRequestException {
        List<Parameter> flatParams;

        if (value instanceof Map<?, ?>) {
            flatParams = flattenParamsMap((Map<String, Object>) value, keyPrefix);
        } else if (value instanceof List<?>) {
            flatParams = flattenParamsList((List<Object>) value, keyPrefix);
        } else if ("".equals(value)) {
            throw new InvalidRequestException("You cannot set '"+keyPrefix+"' to an empty string. "+
                    "We interpret empty strings as null in requests. "+
                    "You may set '"+keyPrefix+"' to null to delete the property.",
                    keyPrefix, null, 0, null);
        } else if (value == null) {
            flatParams = new LinkedList<>();
            flatParams.add(new Parameter(keyPrefix, ""));
        } else {
            flatParams = new LinkedList<>();
            flatParams.add(new Parameter(keyPrefix, value.toString()));
        }

        return flatParams;
    }

    private static void handleAPIError(String rBody, int rCode, String requestId)
            throws InvalidRequestException, AuthenticationException,
            CardException, APIException {

        ErrorParser.StripeError stripeError = ErrorParser.parseError(rBody);
        switch (rCode) {
            case 400:
                throw new InvalidRequestException(
                        stripeError.message,
                        stripeError.param,
                        requestId,
                        rCode,
                        null);
            case 404:
                throw new InvalidRequestException(
                        stripeError.message,
                        stripeError.param,
                        requestId,
                        rCode,
                        null);
            case 401:
                throw new AuthenticationException(stripeError.message, requestId, rCode);
            case 402:
                throw new CardException(
                        stripeError.message,
                        requestId,
                        stripeError.code,
                        stripeError.param,
                        stripeError.decline_code,
                        stripeError.charge,
                        rCode,
                        null);
            case 403:
                throw new PermissionException(stripeError.message, requestId, rCode);
            case 429:
                throw new RateLimitException(stripeError.message, stripeError.param, requestId, rCode, null);
            default:
                throw new APIException(stripeError.message, requestId, rCode, null);
        }
    }

    private static String urlEncodePair(String k, String v)
            throws UnsupportedEncodingException {
        return String.format("%s=%s", urlEncode(k), urlEncode(v));
    }

    private static String urlEncode(String str) throws UnsupportedEncodingException {
        // Preserve original behavior that passing null for an object id will lead
        // to us actually making a request to /v1/foo/null
        if (str == null) {
            return null;
        }
        else {
            return URLEncoder.encode(str, CHARSET);
        }
    }

    private static String getResponseBody(InputStream responseStream)
            throws IOException {
        //\A is the beginning of
        // the stream boundary
        String rBody = new Scanner(responseStream, CHARSET)
                .useDelimiter("\\A")
                .next(); //

        responseStream.close();
        return rBody;
    }

    public interface LoggingResponseListener {
        boolean shouldLogTest();
        void onLoggingResponse(StripeResponse response);
        void onStripeException(StripeException exception);
    }

    private static final class Parameter {
        public final String key;
        public final String value;

        public Parameter(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
