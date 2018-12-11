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
class StripeApiHandler {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            GET,
            POST
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
    private static final String DNS_CACHE_TTL_PROPERTY_NAME = "networkaddress.cache.ttl";
    private static final SSLSocketFactory SSL_SOCKET_FACTORY = new StripeSSLSocketFactory();

    static final String API_VERSION = "2017-06-05";

    static void logApiCall(
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
        if (apiKey == null || apiKey.trim().isEmpty()) {
            // if there is no apiKey associated with the request, we don't need to react here.
            return;
        }

        fireAndForgetApiCall(loggingMap, LIVE_LOGGING_BASE, GET, options, listener);
    }

    /**
     * Confirm a {@link PaymentIntent} using the provided {@link PaymentIntentParams}
     *
     * @param uidProvider a provider for UUID items in test
     * @param context a {@link Context} object for acquiring resources
     * @param paymentIntentParams contains the confirmation params
     * @param publishableKey an API key
     * @param stripeAccount a connected Stripe Account ID
     * @param loggingResponseListener a listener for logging responses
     * @return a {@link PaymentIntent} reflecting the updated state after applying the parameter
     * provided
     *
     * @throws AuthenticationException
     * @throws InvalidRequestException
     * @throws APIConnectionException
     * @throws APIException
     */
    @Nullable
    static PaymentIntent confirmPaymentIntent(
            @Nullable StripeNetworkUtils.UidProvider uidProvider,
            @NonNull Context context,
            @NonNull PaymentIntentParams paymentIntentParams,
            @NonNull String publishableKey,
            @Nullable String stripeAccount,
            @Nullable LoggingResponseListener loggingResponseListener)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        Map<String, Object> paramMap = paymentIntentParams.toParamMap();
        StripeNetworkUtils.addUidParamsToPaymentIntent(uidProvider, context, paramMap);
        RequestOptions options = RequestOptions.builder(
                publishableKey,
                stripeAccount,
                RequestOptions.TYPE_QUERY)
                .setApiVersion(API_VERSION)
                .build();

        try {
            String apiKey = options.getPublishableApiKey();
            if (StripeTextUtils.isBlank(apiKey)) {
                return null;
            }

            setTelemetryData(context, loggingResponseListener);
            SourceParams sourceParams = paymentIntentParams.getSourceParams();
            String sourceType = sourceParams != null ? sourceParams.getType() : null;
            Map<String, Object> loggingParams = LoggingUtils.getPaymentIntentConfirmationParams(
                    context,
                    null,
                    apiKey,
                    sourceType);
            RequestOptions loggingOptions = RequestOptions.builder(publishableKey).build();
            logApiCall(loggingParams, loggingOptions, loggingResponseListener);
            String paymentIntentId = PaymentIntent.parseIdFromClientSecret(
                    paymentIntentParams.getClientSecret());
            StripeResponse response = requestData(
                    POST, confirmPaymentIntentUrl(paymentIntentId), paramMap, options);
            return PaymentIntent.fromString(response.getResponseBody());
        } catch (CardException unexpected) {
            // This particular kind of exception should not be possible from a PaymentI API endpoint
            throw new APIException(
                    unexpected.getMessage(),
                    unexpected.getRequestId(),
                    unexpected.getStatusCode(),
                    unexpected);
        }
    }

    /**
     * Retrive a {@link PaymentIntent} using the provided {@link PaymentIntentParams}
     *
     * @param uidProvider a provider for UUID items in test
     * @param context a {@link Context} object for acquiring resources
     * @param paymentIntentParams contains the retrieval params
     * @param publishableKey an API key
     * @param stripeAccount a connected Stripe Account ID
     * @param loggingResponseListener a listener for logging responses
     * @return
     * @throws AuthenticationException
     * @throws InvalidRequestException
     * @throws APIConnectionException
     * @throws APIException
     */
    @Nullable
    static PaymentIntent retrievePaymentIntent(
            @Nullable StripeNetworkUtils.UidProvider uidProvider,
            @NonNull Context context,
            @NonNull PaymentIntentParams paymentIntentParams,
            @NonNull String publishableKey,
            @Nullable String stripeAccount,
            @Nullable LoggingResponseListener loggingResponseListener)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        Map<String, Object> paramMap = paymentIntentParams.toParamMap();
        RequestOptions options = RequestOptions.builder(
                publishableKey,
                stripeAccount,
                RequestOptions.TYPE_QUERY)
                .setApiVersion(API_VERSION)
                .build();

        try {
            String apiKey = options.getPublishableApiKey();
            if (StripeTextUtils.isBlank(apiKey)) {
                return null;
            }

            setTelemetryData(context, loggingResponseListener);
            Map<String, Object> loggingParams = LoggingUtils.getPaymentIntentRetrieveParams(
                    context,
                    null,
                    apiKey);
            RequestOptions loggingOptions = RequestOptions.builder(publishableKey).build();
            logApiCall(loggingParams, loggingOptions, loggingResponseListener);
            String paymentIntentId = PaymentIntent.parseIdFromClientSecret(
                    paymentIntentParams.getClientSecret());
            StripeResponse response = requestData(GET,
                    retrievePaymentIntentUrl(paymentIntentId), paramMap, options);
            return PaymentIntent.fromString(response.getResponseBody());
        } catch (CardException unexpected) {
            // This particular kind of exception should not be possible from a PaymentI API endpoint
            throw new APIException(
                    unexpected.getMessage(),
                    unexpected.getRequestId(),
                    unexpected.getStatusCode(),
                    unexpected);
        }
    }

    /**
     * Create a {@link Source} using the input {@link SourceParams}.
     *
     * @param uidProvider a provider for UUID items in test
     * @param context a {@link Context} object for acquiring resources
     * @param sourceParams a {@link SourceParams} object with {@link Source} creation params
     * @param publishableKey an API key
     * @param stripeAccount a connected Stripe Account ID
     * @param loggingResponseListener a listener for logging responses
     * @return a {@link Source} if one could be created from the input params,
     * or {@code null} if not
     * @throws AuthenticationException if there is a problem authenticating to the Stripe API
     * @throws InvalidRequestException if one or more of the parameters is incorrect
     * @throws APIConnectionException if there is a problem connecting to the Stripe API
     * @throws APIException for unknown Stripe API errors. These should be rare.
     */
    @Nullable
    static Source createSource(
            @Nullable StripeNetworkUtils.UidProvider uidProvider,
            @NonNull Context context,
            @NonNull SourceParams sourceParams,
            @NonNull String publishableKey,
            @Nullable String stripeAccount,
            @Nullable LoggingResponseListener loggingResponseListener)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        return createSource(
                uidProvider,
                context,
                sourceParams,
                publishableKey,
                stripeAccount,
                loggingResponseListener,
                null);
    }

    @VisibleForTesting
    @Nullable
    static Source createSource(
            @Nullable StripeNetworkUtils.UidProvider uidProvider,
            @NonNull Context context,
            @NonNull SourceParams sourceParams,
            @NonNull String publishableKey,
            @Nullable String stripeAccount,
            @Nullable LoggingResponseListener loggingResponseListener,
            @Nullable StripeResponseListener stripeResponseListener)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        Map<String, Object> paramMap = sourceParams.toParamMap();
        StripeNetworkUtils.addUidParams(uidProvider, context, paramMap);
        RequestOptions options = RequestOptions.builder(
                publishableKey,
                stripeAccount,
                RequestOptions.TYPE_QUERY).build();

        try {
            String apiKey = options.getPublishableApiKey();
            if (StripeTextUtils.isBlank(apiKey)) {
                return null;
            }

            setTelemetryData(context, loggingResponseListener);
            Map<String, Object> loggingParams = LoggingUtils.getSourceCreationParams(
                    context,
                    null,
                    apiKey,
                    sourceParams.getType());
            RequestOptions loggingOptions = RequestOptions.builder(publishableKey).build();
            logApiCall(loggingParams, loggingOptions, loggingResponseListener);
            StripeResponse response = requestData(POST, getSourcesUrl(), paramMap, options);
            if (stripeResponseListener != null) {
                stripeResponseListener.onStripeResponse(response);
            }
            return Source.fromString(response.getResponseBody());
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
    @Nullable
    static Source retrieveSource(
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
           StripeResponse response =
                   requestData(GET, getRetrieveSourceApiUrl(sourceId), paramMap, options);
           return Source.fromString(response.getResponseBody());
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
     * Create a {@link Token} using the input token parameters.
     *
     * @param context the {@link Context} in which this method is working
     * @param tokenParams a mapped set of parameters representing the object for which this token
     *                   is being created
     * @param options a {@link RequestOptions} object that contains connection data like the api
     *                key, api version, etc
     * @param tokenType the {@link com.stripe.android.model.Token.TokenType} being created
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
    @SuppressWarnings("unchecked")
    static Token createToken(
            @NonNull Context context,
            @NonNull Map<String, Object> tokenParams,
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
                    (List<String>) tokenParams.get(LoggingUtils.FIELD_PRODUCT_USAGE);
            tokenParams.remove(LoggingUtils.FIELD_PRODUCT_USAGE);

            setTelemetryData(context, listener);

            Map<String, Object> loggingParams =
                    LoggingUtils.getTokenCreationParams(context, loggingTokens, apiKey, tokenType);
            logApiCall(loggingParams, options, listener);
        } catch (ClassCastException classCastEx) {
            // This can only happen if someone puts a weird object in the map.
            tokenParams.remove(LoggingUtils.FIELD_PRODUCT_USAGE);
        }

        return requestToken(POST, getApiUrl(), tokenParams, options);
    }

    @Nullable
    static Source addCustomerSource(
            @Nullable Context context,
            @NonNull String customerId,
            @NonNull String publicKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @NonNull String secret,
            @Nullable LoggingResponseListener listener)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("source", sourceId);

        if (context != null) {
            Map<String, Object> loggingParamsMap =
                    LoggingUtils.getAddSourceParams(
                            context,
                            productUsageTokens,
                            publicKey,
                            sourceType);

            // We use the public key to log, so we need different RequestOptions.
            RequestOptions loggingOptions =
                    RequestOptions.builder(publicKey).setApiVersion(API_VERSION).build();
            logApiCall(loggingParamsMap, loggingOptions, listener);
        }

        StripeResponse response = getStripeResponse(
                POST,
                getAddCustomerSourceUrl(customerId),
                paramsMap,
                RequestOptions.builder(secret).setApiVersion(API_VERSION).build());
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Source.fromString(response.getResponseBody());
    }

    @Nullable
    static Source deleteCustomerSource(
            @Nullable Context context,
            @NonNull String customerId,
            @NonNull String publicKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull String secret,
            @Nullable LoggingResponseListener listener)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        Map<String, Object> paramsMap = new HashMap<>();

        if (context != null) {
            Map<String, Object> loggingParamsMap =
                    LoggingUtils.getDeleteSourceParams(
                            context,
                            productUsageTokens,
                            publicKey);

            // We use the public key to log, so we need different RequestOptions.
            RequestOptions loggingOptions =
                    RequestOptions.builder(publicKey).setApiVersion(API_VERSION).build();
            logApiCall(loggingParamsMap, loggingOptions, listener);
        }

        StripeResponse response = getStripeResponse(
                DELETE,
                getDeleteCustomerSourceUrl(customerId, sourceId),
                paramsMap,
                RequestOptions.builder(secret).setApiVersion(API_VERSION).build());
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Source.fromString(response.getResponseBody());
    }

    @Nullable
    static Customer setDefaultCustomerSource(
            @Nullable Context context,
            @NonNull String customerId,
            @NonNull String publicKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @NonNull String secret,
            @Nullable LoggingResponseListener listener)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("default_source", sourceId);

        // Context can be nullable because this action is performed with only a weak reference
        if (context != null) {
            RequestOptions loggingOptions = RequestOptions.builder(publicKey)
                    .setApiVersion(API_VERSION)
                    .build();

            Map<String, Object> loggingParameters = LoggingUtils.getEventLoggingParams(
                    context,
                    productUsageTokens,
                    sourceType,
                    null,
                    publicKey,
                    LoggingUtils.EVENT_DEFAULT_SOURCE);

            logApiCall(loggingParameters, loggingOptions, listener);
        }

        StripeResponse response = getStripeResponse(
                POST,
                getRetrieveCustomerUrl(customerId),
                paramsMap,
                RequestOptions.builder(secret).setApiVersion(API_VERSION).build());

        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Customer.fromString(response.getResponseBody());
    }

    @Nullable
    static Customer setCustomerShippingInfo(
            @Nullable Context context,
            @NonNull String customerId,
            @NonNull String publicKey,
            @NonNull List<String> productUsageTokens,
            @NonNull ShippingInformation shippingInformation,
            @NonNull String secret,
            @Nullable LoggingResponseListener listener)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("shipping", shippingInformation.toMap());

        // Context can be nullable because this action is performed with only a weak reference
        if (context != null) {
            RequestOptions loggingOptions = RequestOptions.builder(publicKey)
                    .setApiVersion(API_VERSION)
                    .build();

            Map<String, Object> loggingParameters = LoggingUtils.getEventLoggingParams(
                    context,
                    productUsageTokens,
                    null,
                    null,
                    publicKey,
                    LoggingUtils.EVENT_SET_SHIPPING_INFO);

            logApiCall(loggingParameters, loggingOptions, listener);
        }

        StripeResponse response = getStripeResponse(
                POST,
                getRetrieveCustomerUrl(customerId),
                paramsMap,
                RequestOptions.builder(secret).setApiVersion(API_VERSION).build());
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Customer.fromString(response.getResponseBody());
    }


    @Nullable
    static Customer retrieveCustomer(@NonNull String customerId, @NonNull String secret)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        StripeResponse response = getStripeResponse(
                GET,
                getRetrieveCustomerUrl(customerId),
                null,
                RequestOptions.builder(secret).setApiVersion(API_VERSION).build());
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Customer.fromString(response.getResponseBody());
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

        if (options != null && options.getStripeAccount() != null) {
            headers.put("Stripe-Account", options.getStripeAccount());
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
        //https://api.stripe.com/v1/
        return String.format(Locale.ENGLISH, "%s/v1/%s", LIVE_API_BASE, SOURCES);
    }

    private static String createPaymentIntentUrl() {
        return String.format(Locale.ENGLISH, "%s/v1/payment_intents", LIVE_API_BASE);
    }

    static String retrievePaymentIntentUrl(String paymentIntentId) {
        return String.format(
                Locale.ENGLISH,
                "%s/v1/payment_intents/%s",
                LIVE_API_BASE, paymentIntentId);
    }

    static String confirmPaymentIntentUrl(String paymentIntentId) {
        return String.format(
                Locale.ENGLISH,
                "%s/v1/payment_intents/%s/confirm",
                LIVE_API_BASE,
                paymentIntentId);
    }

    @VisibleForTesting
    static String getCustomersUrl() {
        return String.format(Locale.ENGLISH, "%s/v1/%s", LIVE_API_BASE, CUSTOMERS);
    }

    @VisibleForTesting
    static String getAddCustomerSourceUrl(@NonNull String customerId) {
        return String.format(Locale.ENGLISH, "%s/%s", getRetrieveCustomerUrl(customerId), SOURCES);
    }

    @VisibleForTesting
    static String getDeleteCustomerSourceUrl(@NonNull String customerId, @NonNull String sourceId) {
        return String.format(Locale.ENGLISH,
                "%s/%s", getAddCustomerSourceUrl(customerId), sourceId);
    }

    @VisibleForTesting
    static String getRetrieveCustomerUrl(@NonNull String customerId) {
        return String.format(Locale.ENGLISH, "%s/%s", getCustomersUrl(), customerId);
    }

    @VisibleForTesting
    static String getRetrieveSourceApiUrl(@NonNull String sourceId) {
        return String.format(Locale.ENGLISH, "%s/%s", getSourcesUrl(), sourceId);
    }

    @VisibleForTesting
    static String getRetrieveTokenApiUrl(@NonNull String tokenId) {
        return String.format("%s/%s", getApiUrl(), tokenId);
    }

    static void convertErrorsToExceptionsAndThrowIfNecessary(StripeResponse response) throws
            InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
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
    private static JSONObject mapToJsonObject(@Nullable Map<String, ? extends Object> mapObject) {
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
    private static JSONArray listToJsonArray(@Nullable List values) {
        if (values == null) {
            return null;
        }

        JSONArray jsonArray = new JSONArray();
        for (Object object : values) {
            if (object instanceof Map<?, ?>) {
                // We are ignoring type erasure here and crashing on bad input.
                // Now that this method is not public, we have more control on what is
                // passed to it.
                Map<String, Object> mapObject = (Map<String, Object>) object;
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

    private static void attachPseudoCookie(
            @NonNull HttpURLConnection connection,
            @NonNull RequestOptions options) {
        if (options.getGuid() != null && !TextUtils.isEmpty(options.getGuid())) {
            connection.setRequestProperty("Cookie", "m=" + options.getGuid());
        }
    }

    private static java.net.HttpURLConnection createDeleteConnection(
            @NonNull String url,
            @NonNull RequestOptions options) throws IOException {
        java.net.HttpURLConnection conn = createStripeConnection(url, options);
        conn.setRequestMethod(DELETE);

        return conn;
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
        } catch (StripeException stripeException) {
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

    private static List<Parameter> flattenParams(Map<String, Object> params)
            throws InvalidRequestException {
        return flattenParamsMap(params, null);
    }

    private static List<Parameter> flattenParamsList(List<Object> params, String keyPrefix)
            throws InvalidRequestException {
        List<Parameter> flatParams = new LinkedList<Parameter>();
        Iterator<?> it = ((List<?>) params).iterator();
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
            throw new InvalidRequestException("You cannot set '" + keyPrefix + "' to an empty " +
                    "string. " + "We interpret empty strings as null in requests. " + "You may " +
                    "set '" + keyPrefix + "' to null to delete the property.", keyPrefix, null,
                    0, null);
        } else if (value == null) {
            flatParams = new LinkedList<>();
            flatParams.add(new Parameter(keyPrefix, ""));
        } else {
            flatParams = new LinkedList<>();
            flatParams.add(new Parameter(keyPrefix, value.toString()));
        }

        return flatParams;
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
                JSONObject jsonData = mapToJsonObject(params);
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

    private static String getResponseBody(InputStream responseStream)
            throws IOException {
        //\A is the beginning of
        // the stream boundary
        Scanner scanner = new Scanner(responseStream, CHARSET).useDelimiter("\\A");
        String rBody = scanner.hasNext() ? scanner.next() : null;
        responseStream.close();
        return rBody;
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
                case DELETE:
                    conn = createDeleteConnection(url, options);
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
                throw new RateLimitException(stripeError.message, stripeError.param, requestId,
                        rCode, null);
            default:
                throw new APIException(stripeError.message, requestId, rCode, null);
        }
    }

    private static StripeResponse requestData(
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
            throw new AuthenticationException("No API key provided. (HINT: set your API key using" +
                    " 'Stripe.apiKey = <API-KEY>'. You can generate API keys from the Stripe" +
                    " web interface. See https://stripe.com/api for details or email " +
                    "support@stripe.com if you have questions.", null, 0);
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
        return response;
    }

    @Nullable
    private static Token requestToken(
            @RestMethod String method,
            String url,
            Map<String, Object> params,
            RequestOptions options)
            throws AuthenticationException, InvalidRequestException,
            APIConnectionException, CardException, APIException {
        StripeResponse response = requestData(method, url, params, options);
        return Token.fromString(response.getResponseBody());
    }

    private static void setTelemetryData(@NonNull Context context,
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

    private static boolean urlNeedsHeaderData(@NonNull String url) {
        return url.startsWith(LIVE_API_BASE) || url.startsWith(LIVE_LOGGING_BASE);
    }

    private static boolean urlNeedsPseudoCookie(@NonNull String url) {
        return url.startsWith(LOGGING_ENDPOINT);
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
        } else {
            return URLEncoder.encode(str, CHARSET);
        }
    }

    interface LoggingResponseListener {
        boolean shouldLogTest();

        void onLoggingResponse(StripeResponse response);

        void onStripeException(StripeException exception);
    }

    interface StripeResponseListener {
        void onStripeResponse(StripeResponse response);
    }

    private static final class Parameter {
        public final String key;
        public final String value;

        Parameter(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
