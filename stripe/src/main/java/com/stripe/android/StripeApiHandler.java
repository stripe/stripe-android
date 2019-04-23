package com.stripe.android;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.PermissionException;
import com.stripe.android.exception.RateLimitException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Customer;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentIntentParams;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.Token;

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
import java.security.Security;
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
 * Handler for calls to the Stripe API.
 */
class StripeApiHandler {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            GET,
            POST,
            DELETE
    })
    @interface RestMethod { }
    static final String GET = "GET";
    static final String POST = "POST";
    static final String DELETE = "DELETE";

    private static final String LIVE_API_BASE = "https://api.stripe.com";
    private static final String LIVE_LOGGING_BASE = "https://q.stripe.com";
    private static final String LOGGING_ENDPOINT = "https://m.stripe.com/4";

    private static final String CHARSET = "UTF-8";
    private static final String CUSTOMERS = "customers";
    private static final String TOKENS = "tokens";
    private static final String SOURCES = "sources";
    private static final String PAYMENT_METHODS = "payment_methods";
    private static final String DNS_CACHE_TTL_PROPERTY_NAME = "networkaddress.cache.ttl";

    @NonNull private final LoggingUtils mLoggingUtils;
    @NonNull private final TelemetryClientUtil mTelemetryClientUtil;
    @NonNull private final StripeNetworkUtils mNetworkUtils;
    @NonNull private final ConnectionFactory mConnectionFactory;
    private final boolean mShouldLogRequest;

    StripeApiHandler(@NonNull Context context) {
        this(context, new ConnectionFactory(), true);
    }

    @VisibleForTesting
    StripeApiHandler(@NonNull Context context,
                     @NonNull ConnectionFactory connectionFactory,
                     boolean shouldLogRequest) {
        mShouldLogRequest = shouldLogRequest;
        mLoggingUtils = new LoggingUtils(context);
        mTelemetryClientUtil = new TelemetryClientUtil(context);
        mNetworkUtils = new StripeNetworkUtils(context);
        mConnectionFactory = connectionFactory;
    }

    /**
     * @return true if a request was made and it was successful
     */
    boolean logApiCall(
            @NonNull Map<String, Object> loggingMap,
            @NonNull RequestOptions options) {
        if (!mShouldLogRequest || options == null) {
            return false;
        }

        final String apiKey = options.getPublishableApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            // if there is no apiKey associated with the request, we don't need to react here.
            return false;
        }

        return fireAndForgetApiCall(loggingMap, LIVE_LOGGING_BASE, GET, options);
    }

    /**
     * Confirm a {@link PaymentIntent} using the provided {@link PaymentIntentParams}
     *
     * @param paymentIntentParams contains the confirmation params
     * @param publishableKey an API key
     * @param stripeAccount a connected Stripe Account ID
     * @return a {@link PaymentIntent} reflecting the updated state after applying the parameter
     * provided
     */
    @Nullable
    PaymentIntent confirmPaymentIntent(
            @NonNull PaymentIntentParams paymentIntentParams,
            @NonNull String publishableKey,
            @Nullable String stripeAccount)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        final Map<String, Object> paramMap = paymentIntentParams.toParamMap();
        mNetworkUtils.addUidParamsToPaymentIntent(paramMap);
        final RequestOptions options = RequestOptions.builder(publishableKey, stripeAccount,
                RequestOptions.TYPE_QUERY)
                .build();

        try {
            final String apiKey = options.getPublishableApiKey();
            if (StripeTextUtils.isBlank(apiKey)) {
                return null;
            }

            logTelemetryData();
            final SourceParams sourceParams = paymentIntentParams.getSourceParams();
            final String sourceType = sourceParams != null ? sourceParams.getType() : null;
            final Map<String, Object> loggingParams = mLoggingUtils
                    .getPaymentIntentConfirmationParams(null, apiKey, sourceType);
            RequestOptions loggingOptions = RequestOptions.builder(publishableKey).build();
            logApiCall(loggingParams, loggingOptions);
            final String paymentIntentId = PaymentIntent.parseIdFromClientSecret(
                    paymentIntentParams.getClientSecret());
            final StripeResponse response = requestData(
                    POST, confirmPaymentIntentUrl(paymentIntentId), paramMap, options);
            return PaymentIntent.fromString(response.getResponseBody());
        } catch (CardException unexpected) {
            // This particular kind of exception should not be possible from a PaymentI API endpoint
            throw new APIException(unexpected.getMessage(), unexpected.getRequestId(),
                    unexpected.getStatusCode(), null, unexpected);
        }
    }

    /**
     * Retrieve a {@link PaymentIntent} using the provided {@link PaymentIntentParams}
     *  @param paymentIntentParams contains the retrieval params
     * @param publishableKey an API key
     * @param stripeAccount a connected Stripe Account ID
     */
    @Nullable
    PaymentIntent retrievePaymentIntent(
            @NonNull PaymentIntentParams paymentIntentParams,
            @NonNull String publishableKey,
            @Nullable String stripeAccount)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        final Map<String, Object> paramMap = paymentIntentParams.toParamMap();
        final RequestOptions options = RequestOptions.builder(publishableKey, stripeAccount,
                RequestOptions.TYPE_QUERY).build();

        try {
            final String apiKey = options.getPublishableApiKey();
            if (StripeTextUtils.isBlank(apiKey)) {
                return null;
            }

            logTelemetryData();
            final Map<String, Object> loggingParams = mLoggingUtils
                    .getPaymentIntentRetrieveParams(null, apiKey);
            final RequestOptions loggingOptions = RequestOptions.builder(publishableKey).build();
            logApiCall(loggingParams, loggingOptions);
            final String paymentIntentId = PaymentIntent.parseIdFromClientSecret(
                    paymentIntentParams.getClientSecret());
            final StripeResponse response = requestData(GET,
                    retrievePaymentIntentUrl(paymentIntentId), paramMap, options);
            return PaymentIntent.fromString(response.getResponseBody());
        } catch (CardException unexpected) {
            // This particular kind of exception should not be possible from a PaymentI API endpoint
            throw new APIException(unexpected.getMessage(), unexpected.getRequestId(),
                    unexpected.getStatusCode(), null, unexpected);
        }
    }

    /**
     * Create a {@link Source} using the input {@link SourceParams}.
     *
     * @param sourceParams a {@link SourceParams} object with {@link Source} creation params
     * @param publishableKey an API key
     * @param stripeAccount a connected Stripe Account ID
     * @return a {@link Source} if one could be created from the input params,
     * or {@code null} if not
     * @throws AuthenticationException if there is a problem authenticating to the Stripe API
     * @throws InvalidRequestException if one or more of the parameters is incorrect
     * @throws APIConnectionException if there is a problem connecting to the Stripe API
     * @throws APIException for unknown Stripe API errors. These should be rare.
     */
    @Nullable
    Source createSource(
            @NonNull SourceParams sourceParams,
            @NonNull String publishableKey,
            @Nullable String stripeAccount)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        final Map<String, Object> paramMap = sourceParams.toParamMap();
        mNetworkUtils.addUidParams(paramMap);
        final RequestOptions options = RequestOptions.builder(publishableKey, stripeAccount,
                RequestOptions.TYPE_QUERY).build();

        try {
            final String apiKey = options.getPublishableApiKey();
            if (StripeTextUtils.isBlank(apiKey)) {
                return null;
            }

            logTelemetryData();
            final Map<String, Object> loggingParams = mLoggingUtils.getSourceCreationParams(
                    null,
                    apiKey,
                    sourceParams.getType());
            final RequestOptions loggingOptions = RequestOptions.builder(publishableKey).build();
            logApiCall(loggingParams, loggingOptions);
            final StripeResponse response = requestData(POST, getSourcesUrl(), paramMap, options);
            return Source.fromString(response.getResponseBody());
        } catch (CardException unexpected) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw new APIException(unexpected.getMessage(), unexpected.getRequestId(),
                    unexpected.getStatusCode(), null, unexpected);
        }
    }

    /**
     * Retrieve an existing {@link Source} object from the server.
     *
     * @param sourceId the {@link Source#mId} field for the Source to query
     * @param clientSecret the {@link Source#mClientSecret} field for the Source to query
     * @param publishableKey an API key
     * @param stripeAccount a connected Stripe Account ID
     * @return a {@link Source} if one could be retrieved for the input params, or {@code null} if
     * no such Source could be found.
     *
     * @throws AuthenticationException if there is a problem authenticating to the Stripe API
     * @throws InvalidRequestException if one or more of the parameters is incorrect
     * @throws APIConnectionException if there is a problem connecting to the Stripe API
     * @throws APIException for unknown Stripe API errors. These should be rare.
     */
    @Nullable
    Source retrieveSource(
           @NonNull String sourceId,
           @NonNull String clientSecret,
           @NonNull String publishableKey,
           @Nullable String stripeAccount)
           throws AuthenticationException,
           InvalidRequestException,
           APIConnectionException,
           APIException {
       final Map<String, Object> paramMap = SourceParams.createRetrieveSourceParams(clientSecret);
       final RequestOptions options;
       if (stripeAccount == null) {
           options = RequestOptions.builder(publishableKey).build();
       } else {
           options = RequestOptions.builder(publishableKey, stripeAccount,
                   RequestOptions.TYPE_QUERY).build();
       }
       try {
           final StripeResponse response =
                   requestData(GET, getRetrieveSourceApiUrl(sourceId), paramMap, options);
           return Source.fromString(response.getResponseBody());
       } catch (CardException unexpected) {
           // This particular kind of exception should not be possible from a Source API endpoint.
           throw new APIException(unexpected.getMessage(), unexpected.getRequestId(),
                   unexpected.getStatusCode(), null, unexpected);
       }
    }

    @Nullable
    PaymentMethod createPaymentMethod(
            @NonNull PaymentMethodCreateParams paymentMethodCreateParams,
            @NonNull String publishableKey,
            @Nullable String stripeAccount)

            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        final Map<String, Object> params = paymentMethodCreateParams.toParamMap();

        mNetworkUtils.addUidParams(params);
        final RequestOptions options = RequestOptions.builder(publishableKey, stripeAccount,
                RequestOptions.TYPE_QUERY).build();

        final String apiKey = options.getPublishableApiKey();
        if (StripeTextUtils.isBlank(apiKey)) {
            return null;
        }

        logTelemetryData();
        final Map<String, Object> loggingParams = mLoggingUtils.getPaymentMethodCreationParams(
                null, apiKey);
        final RequestOptions loggingOptions = RequestOptions.builder(publishableKey).build();
        logApiCall(loggingParams, loggingOptions);

        try {
            final StripeResponse response = requestData(POST, getPaymentMethodsUrl(), params,
                    options);
            return PaymentMethod.fromString(response.getResponseBody());
        } catch (CardException unexpected) {
            throw new APIException(unexpected.getMessage(), unexpected.getRequestId(),
                    unexpected.getStatusCode(), null, unexpected);
        }
    }

    /**
     * Create a {@link Token} using the input token parameters.
     *
     * @param tokenParams a mapped set of parameters representing the object for which this token
     *                   is being created
     * @param options a {@link RequestOptions} object that contains connection data like the api
     *                key, api version, etc
     * @param tokenType the {@link Token.TokenType} being created
     * @return a {@link Token} that can be used to perform other operations with this card
     * @throws AuthenticationException if there is a problem authenticating to the Stripe API
     * @throws InvalidRequestException if one or more of the parameters is incorrect
     * @throws APIConnectionException if there is a problem connecting to the Stripe API
     * @throws CardException if there is a problem with the card information
     * @throws APIException for unknown Stripe API errors. These should be rare.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    Token createToken(
            @NonNull Map<String, Object> tokenParams,
            @NonNull RequestOptions options,
            @NonNull @Token.TokenType String tokenType)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {

        try {
            final String apiKey = options.getPublishableApiKey();
            if (StripeTextUtils.isBlank(apiKey)) {
                return null;
            }

            final List<String> loggingTokens =
                    (List<String>) tokenParams.get(LoggingUtils.FIELD_PRODUCT_USAGE);
            tokenParams.remove(LoggingUtils.FIELD_PRODUCT_USAGE);

            logTelemetryData();

            final Map<String, Object> loggingParams =
                    mLoggingUtils.getTokenCreationParams(loggingTokens, apiKey, tokenType);
            logApiCall(loggingParams, options);
        } catch (ClassCastException classCastEx) {
            // This can only happen if someone puts a weird object in the map.
            tokenParams.remove(LoggingUtils.FIELD_PRODUCT_USAGE);
        }

        return requestToken(getTokensUrl(), tokenParams, options);
    }

    @Nullable
    Source addCustomerSource(
            @NonNull String customerId,
            @NonNull String publicKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @NonNull String secret)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("source", sourceId);

        final Map<String, Object> loggingParamsMap = mLoggingUtils.getAddSourceParams(
                productUsageTokens, publicKey, sourceType);

        // We use the public key to log, so we need different RequestOptions.
        final RequestOptions loggingOptions = RequestOptions.builder(publicKey).build();
        logApiCall(loggingParamsMap, loggingOptions);

        final StripeResponse response = getStripeResponse(
                POST,
                getAddCustomerSourceUrl(customerId),
                paramsMap,
                RequestOptions.builder(secret).build());
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Source.fromString(response.getResponseBody());
    }

    @Nullable
    Source deleteCustomerSource(
            @NonNull String customerId,
            @NonNull String publicKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull String secret)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final Map<String, Object> paramsMap = new HashMap<>();
        final Map<String, Object> loggingParamsMap =
                mLoggingUtils.getDeleteSourceParams(productUsageTokens, publicKey);

        // We use the public key to log, so we need different RequestOptions.
        final RequestOptions loggingOptions = RequestOptions.builder(publicKey).build();
        logApiCall(loggingParamsMap, loggingOptions);

        final StripeResponse response = getStripeResponse(
                DELETE,
                getDeleteCustomerSourceUrl(customerId, sourceId),
                paramsMap,
                RequestOptions.builder(secret).build());
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Source.fromString(response.getResponseBody());
    }

    @Nullable
    Customer setDefaultCustomerSource(
            @NonNull String customerId,
            @NonNull String publicKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @NonNull String secret)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("default_source", sourceId);

        final RequestOptions loggingOptions = RequestOptions.builder(publicKey).build();

        final Map<String, Object> loggingParameters = mLoggingUtils.getEventLoggingParams(
                productUsageTokens,
                sourceType,
                null,
                publicKey, LoggingUtils.EVENT_DEFAULT_SOURCE);

        logApiCall(loggingParameters, loggingOptions);

        final StripeResponse response = getStripeResponse(
                POST,
                getRetrieveCustomerUrl(customerId),
                paramsMap,
                RequestOptions.builder(secret).build());

        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Customer.fromString(response.getResponseBody());
    }

    @Nullable
    Customer setCustomerShippingInfo(
            @NonNull String customerId,
            @NonNull String publicKey,
            @NonNull List<String> productUsageTokens,
            @NonNull ShippingInformation shippingInformation,
            @NonNull String secret)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("shipping", shippingInformation.toMap());

        final RequestOptions loggingOptions = RequestOptions.builder(publicKey).build();

        final Map<String, Object> loggingParameters = mLoggingUtils.getEventLoggingParams(
                productUsageTokens,
                null,
                null,
                publicKey, LoggingUtils.EVENT_SET_SHIPPING_INFO);

        logApiCall(loggingParameters, loggingOptions);

        final StripeResponse response = getStripeResponse(
                POST,
                getRetrieveCustomerUrl(customerId),
                paramsMap,
                RequestOptions.builder(secret).build());
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Customer.fromString(response.getResponseBody());
    }


    @Nullable
    Customer retrieveCustomer(@NonNull String customerId, @NonNull String secret)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final StripeResponse response = getStripeResponse(
                GET,
                getRetrieveCustomerUrl(customerId),
                null,
                RequestOptions.builder(secret).build());
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Customer.fromString(response.getResponseBody());
    }

    @NonNull
    String createQuery(@Nullable Map<String, Object> params)
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

    @VisibleForTesting
    void start3ds2Auth(@NonNull Stripe3DS2AuthParams authParams,
                       @NonNull String publishableKey)
            throws InvalidRequestException, APIConnectionException, APIException, CardException,
            AuthenticationException {
        final StripeResponse response = getStripeResponse(POST,
                getApiUrl("3ds2/authenticate"),
                authParams.toParamMap(),
                RequestOptions.builder(publishableKey).build()
        );
        convertErrorsToExceptionsAndThrowIfNecessary(response);
    }

    @NonNull
    @VisibleForTesting
    String getTokensUrl() {
        return getApiUrl(TOKENS);
    }

    /**
     * @return https://api.stripe.com/v1/sources
     */
    @NonNull
    @VisibleForTesting
    String getSourcesUrl() {
        return getApiUrl(SOURCES);
    }

    /**
     * @return https://api.stripe.com/v1/payment_methods
     */
    @NonNull
    @VisibleForTesting
    String getPaymentMethodsUrl() {
        return getApiUrl(PAYMENT_METHODS);
    }

    @NonNull
    private static String getApiUrl(@NonNull String path) {
        return String.format(Locale.ENGLISH, "%s/v1/%s", LIVE_API_BASE, path);
    }

    @NonNull
    private String retrievePaymentIntentUrl(@NonNull String paymentIntentId) {
        return String.format(
                Locale.ENGLISH,
                "%s/v1/payment_intents/%s",
                LIVE_API_BASE, paymentIntentId);
    }

    @NonNull
    private String confirmPaymentIntentUrl(@NonNull String paymentIntentId) {
        return String.format(
                Locale.ENGLISH,
                "%s/v1/payment_intents/%s/confirm",
                LIVE_API_BASE,
                paymentIntentId);
    }

    @VisibleForTesting
    private String getCustomersUrl() {
        return getApiUrl(CUSTOMERS);
    }

    @VisibleForTesting
    String getAddCustomerSourceUrl(@NonNull String customerId) {
        return String.format(Locale.ENGLISH, "%s/%s",
                getRetrieveCustomerUrl(customerId), SOURCES);
    }

    @VisibleForTesting
    String getDeleteCustomerSourceUrl(@NonNull String customerId, @NonNull String sourceId) {
        return String.format(Locale.ENGLISH,
                "%s/%s", getAddCustomerSourceUrl(customerId), sourceId);
    }

    @VisibleForTesting
    String getRetrieveCustomerUrl(@NonNull String customerId) {
        return String.format(Locale.ENGLISH, "%s/%s", getCustomersUrl(), customerId);
    }

    @VisibleForTesting
    String getRetrieveSourceApiUrl(@NonNull String sourceId) {
        return String.format(Locale.ENGLISH, "%s/%s", getSourcesUrl(), sourceId);
    }

    @VisibleForTesting
    String getRetrieveTokenApiUrl(@NonNull String tokenId) {
        return String.format(Locale.ROOT, "%s/%s", getTokensUrl(), tokenId);
    }

    private void convertErrorsToExceptionsAndThrowIfNecessary(
            @NonNull StripeResponse response)
            throws InvalidRequestException, APIException, AuthenticationException, CardException {
        final int rCode = response.getResponseCode();
        final String rBody = response.getResponseBody();
        final Map<String, List<String>> headers = response.getResponseHeaders();
        final List<String> requestIdList = headers == null ? null : headers.get("Request-Id");

        final String requestId;
        if (requestIdList != null && requestIdList.size() > 0) {
            requestId = requestIdList.get(0);
        } else {
            requestId = null;
        }

        if (rCode < 200 || rCode >= 300) {
            handleAPIError(rBody, rCode, requestId);
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
                        // We don't include the item in the JSONObject if the keys are not Strings.
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
    private HttpURLConnection createDeleteConnection(
            @NonNull String url,
            @NonNull RequestOptions options) throws IOException {
        final HttpURLConnection conn = mConnectionFactory.create(url, options);
        conn.setRequestMethod(DELETE);
        return conn;
    }

    @NonNull
    private HttpURLConnection createGetConnection(
            @NonNull String url,
            @NonNull String query,
            @NonNull RequestOptions options) throws IOException {
        final HttpURLConnection conn = mConnectionFactory.create(formatURL(url, query), options);
        conn.setRequestMethod(GET);
        return conn;
    }

    @NonNull
    private HttpURLConnection createPostConnection(
            @NonNull String url,
            @Nullable Map<String, Object> params,
            @NonNull RequestOptions options) throws IOException, InvalidRequestException {
        final HttpURLConnection conn = mConnectionFactory.create(url, options);

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

    /**
     * @return true if a request was made and it was successful
     */
    private boolean fireAndForgetApiCall(
            @NonNull Map<String, Object> paramMap,
            @NonNull String url,
            @NonNull @RestMethod String method,
            @NonNull RequestOptions options) {
        String originalDNSCacheTTL = null;
        boolean allowedToSetTTL = true;

        try {
            originalDNSCacheTTL = Security.getProperty(DNS_CACHE_TTL_PROPERTY_NAME);
            // disable DNS cache
            Security.setProperty(DNS_CACHE_TTL_PROPERTY_NAME, "0");
        } catch (SecurityException se) {
            allowedToSetTTL = false;
        }

        boolean isSuccessful = false;
        try {
            final StripeResponse response = getStripeResponse(
                    method,
                    url,
                    paramMap,
                    options);
            isSuccessful = response.getResponseCode() == 200;
        } catch (StripeException ignore) {
            // We're just logging. No need to crash here or attempt to re-log things.
        } finally {
            if (allowedToSetTTL) {
                if (originalDNSCacheTTL == null) {
                    // value unspecified by implementation
                    // DNS_CACHE_TTL_PROPERTY_NAME of -1 = cache forever
                    Security.setProperty(DNS_CACHE_TTL_PROPERTY_NAME, "-1");
                } else {
                    Security.setProperty(DNS_CACHE_TTL_PROPERTY_NAME,
                            originalDNSCacheTTL);
                }
            }
        }

        return isSuccessful;
    }

    @NonNull
    private List<Parameter> flattenParams(@Nullable Map<String, Object> params)
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
    private List<Parameter> flattenParamsMap(@Nullable Map<String, Object> params,
                                             @Nullable String keyPrefix)
            throws InvalidRequestException {
        final List<Parameter> flatParams = new LinkedList<>();
        if (params == null) {
            return flatParams;
        }

        for (Map.Entry<String, Object> entry : params.entrySet()) {
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
    private List<Parameter> flattenParamsValue(@NonNull Object value,
                                               @Nullable String keyPrefix)
            throws InvalidRequestException {
        final List<Parameter> flatParams;
        if (value instanceof Map<?, ?>) {
            flatParams = flattenParamsMap((Map<String, Object>) value, keyPrefix);
        } else if (value instanceof List<?>) {
            flatParams = flattenParamsList((List<?>) value, keyPrefix);
        } else if ("".equals(value)) {
            throw new InvalidRequestException("You cannot set '" + keyPrefix + "' to an empty " +
                    "string. " + "We interpret empty strings as null in requests. " + "You may " +
                    "set '" + keyPrefix + "' to null to delete the property.", keyPrefix, null,
                    0, null, null, null, null);
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
    private String formatURL(@NonNull String url, @Nullable String query) {
        if (query == null || query.isEmpty()) {
            return url;
        } else {
            // In some cases, URL can already contain a question mark (eg, upcoming invoice lines)
            String separator = url.contains("?") ? "&" : "?";
            return String.format(Locale.ROOT, "%s%s%s", url, separator, query);
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
            @Nullable Map<String, Object> params,
            @NonNull RequestOptions options) throws InvalidRequestException {
        try {
            if (RequestOptions.TYPE_JSON.equals(options.getRequestType())) {
                JSONObject jsonData = mapToJsonObject(params);
                if (jsonData == null) {
                    throw new InvalidRequestException("Unable to create JSON data from parameters. "
                            + "Please contact support@stripe.com for assistance.",
                            null, null, 0, null, null, null, null);
                }
                return jsonData.toString().getBytes(CHARSET);
            } else {
                return createQuery(params).getBytes(CHARSET);
            }
        } catch (UnsupportedEncodingException e) {
            throw new InvalidRequestException("Unable to encode parameters to " + CHARSET + "." +
                    " Please contact support@stripe.com for assistance.",
                    null, null, 0, null, null, null, e);
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

    @NonNull
    private StripeResponse getStripeResponse(
            @RestMethod String method,
            @NonNull String url,
            @Nullable Map<String, Object> params,
            @NonNull RequestOptions options)
            throws InvalidRequestException, APIConnectionException {
        // HttpURLConnection verifies SSL cert by default
        HttpURLConnection conn = null;
        try {
            switch (method) {
                case GET:
                    conn = createGetConnection(url, createQuery(params), options);
                    break;
                case POST:
                    conn = createPostConnection(url, params, options);
                    break;
                case DELETE:
                    conn = createDeleteConnection(url, options);
                    break;
                default:
                    throw new APIConnectionException(
                            String.format(Locale.ENGLISH,
                                    "Unrecognized HTTP method %s. "
                                            + "This indicates a bug in the Stripe bindings. "
                                            + "Please contact support@stripe.com for assistance.",
                                    method));
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
                            getTokensUrl(), e.getMessage()), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void handleAPIError(@Nullable String responseBody, int responseCode,
                                @Nullable String requestId)
            throws InvalidRequestException, AuthenticationException, CardException, APIException {

        final StripeError stripeError = ErrorParser.parseError(responseBody);
        switch (responseCode) {
            case 400:
            case 404: {
                throw new InvalidRequestException(
                        stripeError.message,
                        stripeError.param,
                        requestId,
                        responseCode,
                        stripeError.code,
                        stripeError.declineCode,
                        stripeError,
                        null);
            }
            case 401: {
                throw new AuthenticationException(stripeError.message, requestId, responseCode,
                        stripeError);
            }
            case 402: {
                throw new CardException(
                        stripeError.message,
                        requestId,
                        stripeError.code,
                        stripeError.param,
                        stripeError.declineCode,
                        stripeError.charge,
                        responseCode,
                        stripeError
                );
            }
            case 403: {
                throw new PermissionException(stripeError.message, requestId, responseCode,
                        stripeError);
            }
            case 429: {
                throw new RateLimitException(stripeError.message, stripeError.param, requestId,
                        responseCode, stripeError);
            }
            default: {
                throw new APIException(stripeError.message, requestId, responseCode, stripeError,
                        null);
            }
        }
    }

    @NonNull
    @VisibleForTesting
    StripeResponse requestData(
            @RestMethod String method,
            @NonNull String url,
            @NonNull Map<String, Object> params,
            @NonNull RequestOptions options)
            throws AuthenticationException, InvalidRequestException,
            APIConnectionException, CardException, APIException {

        String originalDNSCacheTTL = null;
        boolean allowedToSetTTL = true;

        try {
            originalDNSCacheTTL = Security.getProperty(DNS_CACHE_TTL_PROPERTY_NAME);
            // disable DNS cache
            Security.setProperty(DNS_CACHE_TTL_PROPERTY_NAME, "0");
        } catch (SecurityException se) {
            allowedToSetTTL = false;
        }

        final String apiKey = options.getPublishableApiKey();
        if (StripeTextUtils.isBlank(apiKey)) {
            throw new AuthenticationException("No API key provided. (HINT: set your API key using" +
                    " 'Stripe.apiKey = <API-KEY>'. You can generate API keys from the Stripe" +
                    " web interface. See https://stripe.com/api for details or email " +
                    "support@stripe.com if you have questions.", null, 0, null);
        }

        final StripeResponse response = getStripeResponse(method, url, params, options);

        final int rCode = response.getResponseCode();
        final String rBody = response.getResponseBody();

        final Map<String, List<String>> headers = response.getResponseHeaders();
        final List<String> requestIdList = headers == null ? null : headers.get("Request-Id");
        final String requestId;
        if (requestIdList != null && requestIdList.size() > 0) {
            requestId = requestIdList.get(0);
        } else {
            requestId = null;
        }

        if (rCode < 200 || rCode >= 300) {
            handleAPIError(rBody, rCode, requestId);
        }

        if (allowedToSetTTL) {
            if (originalDNSCacheTTL == null) {
                // value unspecified by implementation
                // DNS_CACHE_TTL_PROPERTY_NAME of -1 = cache forever
                Security.setProperty(DNS_CACHE_TTL_PROPERTY_NAME, "-1");
            } else {
                Security.setProperty(DNS_CACHE_TTL_PROPERTY_NAME,
                        originalDNSCacheTTL);
            }
        }
        return response;
    }

    @Nullable
    private Token requestToken(
            @NonNull String url,
            @NonNull Map<String, Object> params,
            @NonNull RequestOptions options)
            throws AuthenticationException, InvalidRequestException,
            APIConnectionException, CardException, APIException {
        final StripeResponse response = requestData(StripeApiHandler.POST, url, params, options);
        return Token.fromString(response.getResponseBody());
    }

    private void logTelemetryData() {
        final Map<String, Object> telemetry = mTelemetryClientUtil.createTelemetryMap();
        StripeNetworkUtils.removeNullAndEmptyParams(telemetry);
        if (!mShouldLogRequest) {
            return;
        }

        final RequestOptions options =
                RequestOptions.builder(null, RequestOptions.TYPE_JSON)
                        .setGuid(mTelemetryClientUtil.getHashedId())
                        .build();
        fireAndForgetApiCall(telemetry, LOGGING_ENDPOINT, POST, options);
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

    private static final class Parameter {
        @NonNull private final String key;
        @NonNull private final String value;

        Parameter(@NonNull String key, @NonNull String value) {
            this.key = key;
            this.value = value;
        }
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
        private HttpURLConnection create(@NonNull String url, @NonNull RequestOptions options)
                throws IOException {
            final URL stripeURL = new URL(url);
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

            return conn;
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
}
