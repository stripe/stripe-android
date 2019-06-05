package com.stripe.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

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
import com.stripe.android.model.Stripe3ds2AuthResult;
import com.stripe.android.model.Token;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Handler for calls to the Stripe API.
 */
class StripeApiHandler {

    private static final String DNS_CACHE_TTL_PROPERTY_NAME = "networkaddress.cache.ttl";

    @NonNull private final LoggingUtils mLoggingUtils;
    @NonNull private final TelemetryClientUtil mTelemetryClientUtil;
    @NonNull private final StripeNetworkUtils mNetworkUtils;
    @NonNull private final RequestExecutor mRequestExecutor;
    private final boolean mShouldLogRequest;

    StripeApiHandler(@NonNull Context context) {
        this(context.getApplicationContext(), new RequestExecutor(), true);
    }

    @VisibleForTesting
    StripeApiHandler(@NonNull Context context,
                     @NonNull RequestExecutor requestExecutor,
                     boolean shouldLogRequest) {
        mRequestExecutor = requestExecutor;
        mShouldLogRequest = shouldLogRequest;
        mLoggingUtils = new LoggingUtils(context);
        mTelemetryClientUtil = new TelemetryClientUtil(context);
        mNetworkUtils = new StripeNetworkUtils(context);
    }

    /**
     * @return true if a request was made and it was successful
     */
    boolean logApiCall(
            @NonNull Map<String, Object> loggingMap,
            @NonNull String publishableKey) {
        if (!mShouldLogRequest) {
            return false;
        }

        return fireAndForgetApiCall(StripeRequest.createGet(RequestExecutor.ANALYTICS_HOST,
                loggingMap, RequestOptions.createForApi(publishableKey)));
    }

    /**
     * Confirm a {@link PaymentIntent} using the provided {@link PaymentIntentParams}
     *
     * @param paymentIntentParams contains the confirmation params
     * @return a {@link PaymentIntent} reflecting the updated state after applying the parameter
     * provided
     */
    @Nullable
    PaymentIntent confirmPaymentIntent(
            @NonNull PaymentIntentParams paymentIntentParams,
            @NonNull RequestOptions requestOptions)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        final Map<String, Object> paramMap = paymentIntentParams.toParamMap();
        mNetworkUtils.addUidParamsToPaymentIntent(paramMap);

        try {
            logTelemetryData();
            final SourceParams sourceParams = paymentIntentParams.getSourceParams();
            final String sourceType = sourceParams != null ? sourceParams.getType() : null;

            final String apiKey = Objects.requireNonNull(requestOptions.getApiKey());
            logApiCall(
                    mLoggingUtils.getPaymentIntentConfirmationParams(null,
                            Objects.requireNonNull(apiKey), sourceType),
                    apiKey
            );
            final String paymentIntentId = PaymentIntent.parseIdFromClientSecret(
                    Objects.requireNonNull(paymentIntentParams.getClientSecret()));
            final StripeResponse response = requestData(StripeRequest.createPost(
                    getConfirmPaymentIntentUrl(paymentIntentId), paramMap, requestOptions));
            return PaymentIntent.fromString(response.getResponseBody());
        } catch (CardException unexpected) {
            // This particular kind of exception should not be possible from a PaymentI API endpoint
            throw new APIException(unexpected.getMessage(), unexpected.getRequestId(),
                    unexpected.getStatusCode(), null, unexpected);
        }
    }

    /**
     * Retrieve a {@link PaymentIntent} using the provided {@link PaymentIntentParams}
     * @param paymentIntentParams contains the retrieval params
     */
    @Nullable
    PaymentIntent retrievePaymentIntent(
            @NonNull PaymentIntentParams paymentIntentParams,
            @NonNull RequestOptions requestOptions)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        final Map<String, Object> paramMap = paymentIntentParams.toParamMap();

        try {
            logTelemetryData();
            final String apiKey = Objects.requireNonNull(requestOptions.getApiKey());
            logApiCall(
                    mLoggingUtils.getPaymentIntentRetrieveParams(null, apiKey),
                    apiKey);
            final String paymentIntentId = PaymentIntent.parseIdFromClientSecret(
                    Objects.requireNonNull(paymentIntentParams.getClientSecret()));
            final StripeResponse response = requestData(StripeRequest.createGet(
                    getRetrievePaymentIntentUrl(paymentIntentId), paramMap, requestOptions));
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
            @NonNull RequestOptions requestOptions)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        final Map<String, Object> paramMap = sourceParams.toParamMap();
        mNetworkUtils.addUidParams(paramMap);

        try {
            logTelemetryData();
            final String apiKey = Objects.requireNonNull(requestOptions.getApiKey());
            logApiCall(
                    mLoggingUtils.getSourceCreationParams(null,
                            Objects.requireNonNull(apiKey), sourceParams.getType()),
                    apiKey);
            final StripeResponse response = requestData(
                    StripeRequest.createPost(getSourcesUrl(), paramMap, requestOptions));
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
     * @param sourceId the {@link Source#getId()} field for the Source to query
     * @param clientSecret the {@link Source#getClientSecret()} field for the Source to query
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
            @NonNull RequestOptions requestOptions)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        final Map<String, String> paramMap = SourceParams.createRetrieveSourceParams(clientSecret);
        try {
            final StripeResponse response = requestData(
                    StripeRequest.createGet(getRetrieveSourceApiUrl(sourceId), paramMap,
                            requestOptions));
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
            @NonNull RequestOptions requestOptions)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        final Map<String, Object> params = paymentMethodCreateParams.toParamMap();

        mNetworkUtils.addUidParams(params);
        logTelemetryData();

        final String apiKey = Objects.requireNonNull(requestOptions.getApiKey());
        logApiCall(
                mLoggingUtils.getPaymentMethodCreationParams(null, apiKey),
                apiKey);

        try {
            final StripeResponse response = requestData(
                    StripeRequest.createPost(getPaymentMethodsUrl(), params, requestOptions));
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
            final List<String> loggingTokens =
                    (List<String>) tokenParams.get(LoggingUtils.FIELD_PRODUCT_USAGE);
            tokenParams.remove(LoggingUtils.FIELD_PRODUCT_USAGE);

            logTelemetryData();

            final String apiKey = Objects.requireNonNull(options.getApiKey());
            logApiCall(
                    mLoggingUtils.getTokenCreationParams(loggingTokens,
                            apiKey, tokenType),
                    apiKey
            );
        } catch (ClassCastException classCastEx) {
            // This can only happen if someone puts a weird object in the map.
            tokenParams.remove(LoggingUtils.FIELD_PRODUCT_USAGE);
        }

        return requestToken(getTokensUrl(), tokenParams, options);
    }

    @Nullable
    Source addCustomerSource(
            @NonNull String customerId,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @NonNull String ephemeralKey)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final Map<String, Object> params = new HashMap<>();
        params.put("source", sourceId);

        logApiCall(
                mLoggingUtils.getAddSourceParams(productUsageTokens, publishableKey, sourceType),
                // We use the public key to log, so we need different RequestOptions.
                publishableKey
        );

        final StripeResponse response = getStripeResponse(
                StripeRequest.createPost(
                        getAddCustomerSourceUrl(customerId),
                        params,
                        RequestOptions.createForApi(ephemeralKey))
        );
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Source.fromString(response.getResponseBody());
    }

    @Nullable
    Source deleteCustomerSource(
            @NonNull String customerId,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull String ephemeralKey)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        logApiCall(
                mLoggingUtils.getDeleteSourceParams(productUsageTokens, publishableKey),
                // We use the public key to log, so we need different RequestOptions.
                publishableKey
        );

        final StripeResponse response = getStripeResponse(
                StripeRequest.createDelete(
                        getDeleteCustomerSourceUrl(customerId, sourceId),
                        RequestOptions.createForApi(ephemeralKey))
        );

        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Source.fromString(response.getResponseBody());
    }

    @Nullable
    PaymentMethod attachPaymentMethod(
            @NonNull String customerId,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String paymentMethodId,
            @NonNull String ephemeralKey)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);

        logApiCall(
                mLoggingUtils.getAttachPaymentMethodParams(productUsageTokens, publishableKey),
                // We use the public key to log, so we need different RequestOptions.
                publishableKey
        );

        final StripeResponse response = getStripeResponse(
                StripeRequest.createPost(
                        getAttachPaymentMethodUrl(paymentMethodId),
                        params,
                        RequestOptions.createForApi(ephemeralKey))
        );
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return PaymentMethod.fromString(response.getResponseBody());
    }

    @Nullable
    PaymentMethod detachPaymentMethod(
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String paymentMethodId,
            @NonNull String ephemeralKey)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        logApiCall(
                mLoggingUtils.getDetachPaymentMethodParams(productUsageTokens, publishableKey),
                // We use the public key to log, so we need different RequestOptions.
                publishableKey
        );

        final StripeResponse response = getStripeResponse(
                StripeRequest.createPost(
                        getDetachPaymentMethodUrl(paymentMethodId),
                        RequestOptions.createForApi(ephemeralKey))
        );
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return PaymentMethod.fromString(response.getResponseBody());
    }

    /**
     * Retrieve a Customer's {@link PaymentMethod}s
     */
    @NonNull
    List<PaymentMethod> getPaymentMethods(
            @NonNull String customerId,
            @NonNull String paymentMethodType,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String ephemeralKey)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final Map<String, String> queryParams = new HashMap<>(2);
        queryParams.put("customer", customerId);
        queryParams.put("type", paymentMethodType);

        logApiCall(
                mLoggingUtils.getDetachPaymentMethodParams(productUsageTokens, publishableKey),
                // We use the public key to log, so we need different RequestOptions.
                publishableKey
        );

        final StripeResponse response = getStripeResponse(
                StripeRequest.createGet(
                        getPaymentMethodsUrl(),
                        queryParams,
                        RequestOptions.createForApi(ephemeralKey))
        );
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);

        final JSONArray data;
        try {
            data = new JSONObject(response.getResponseBody()).optJSONArray("data");
        } catch (JSONException e) {
            return new ArrayList<>();
        }

        final List<PaymentMethod> paymentMethods = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            paymentMethods.add(PaymentMethod.fromJson(data.optJSONObject(i)));
        }
        return paymentMethods;
    }

    @Nullable
    Customer setDefaultCustomerSource(
            @NonNull String customerId,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @NonNull String ephemeralKey)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final Map<String, Object> params = new HashMap<>();
        params.put("default_source", sourceId);

        logApiCall(
                mLoggingUtils.getEventLoggingParams(productUsageTokens, sourceType, null,
                        publishableKey, LoggingUtils.EVENT_DEFAULT_SOURCE),
                ephemeralKey
        );

        final StripeResponse response = getStripeResponse(
                StripeRequest.createPost(
                        getRetrieveCustomerUrl(customerId),
                        params,
                        RequestOptions.createForApi(ephemeralKey))
        );

        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Customer.fromString(response.getResponseBody());
    }

    @Nullable
    Customer setCustomerShippingInfo(
            @NonNull String customerId,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull ShippingInformation shippingInformation,
            @NonNull String ephemeralKey)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final Map<String, Object> params = new HashMap<>();
        params.put("shipping", shippingInformation.toMap());

        logApiCall(
                mLoggingUtils.getEventLoggingParams(productUsageTokens, null, null,
                        publishableKey, LoggingUtils.EVENT_SET_SHIPPING_INFO),
                publishableKey
        );

        final StripeResponse response = getStripeResponse(
                StripeRequest.createPost(
                        getRetrieveCustomerUrl(customerId),
                        params,
                        RequestOptions.createForApi(ephemeralKey))
        );
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Customer.fromString(response.getResponseBody());
    }


    @Nullable
    Customer retrieveCustomer(@NonNull String customerId, @NonNull String ephemeralKey)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final StripeResponse response = getStripeResponse(
                StripeRequest.createGet(
                        getRetrieveCustomerUrl(customerId),
                        RequestOptions.createForApi(ephemeralKey))
        );
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Customer.fromString(response.getResponseBody());
    }

    @NonNull
    private static Map<String, String> createVerificationParam(@NonNull String verificationId,
                                                               @NonNull String userOneTimeCode) {
        final Map<String, String> verificationMap = new HashMap<>();
        verificationMap.put("id", verificationId);
        verificationMap.put("one_time_code", userOneTimeCode);
        return verificationMap;
    }

    @NonNull
    String retrieveIssuingCardPin(
            @NonNull String cardId,
            @NonNull String verificationId,
            @NonNull String userOneTimeCode,
            @NonNull String ephemeralKeySecret)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException, JSONException {
        final Map<String, Map<String, String>> params = new HashMap<>();
        params.put("verification", createVerificationParam(verificationId, userOneTimeCode));

        StripeResponse response = getStripeResponse(
                StripeRequest.createGet(
                        getIssuingCardPinUrl(cardId),
                        params,
                        RequestOptions.createForApi(ephemeralKeySecret))
        );
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        JSONObject jsonResponse = new JSONObject(response.getResponseBody());
        return jsonResponse.getString("pin");
    }

    void updateIssuingCardPin(
            @NonNull String cardId,
            @NonNull String newPin,
            @NonNull String verificationId,
            @NonNull String userOneTimeCode,
            @NonNull String ephemeralKeySecret)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final Map<String, Object> params = new HashMap<>();
        params.put("verification", createVerificationParam(verificationId, userOneTimeCode));
        params.put("pin", newPin);

        StripeResponse response = getStripeResponse(
                StripeRequest.createPost(
                getIssuingCardPinUrl(cardId),
                params,
                RequestOptions.createForApi(ephemeralKeySecret))
        );
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
    }

    @NonNull
    @VisibleForTesting
    Stripe3ds2AuthResult start3ds2Auth(@NonNull Stripe3ds2AuthParams authParams,
                                       @NonNull String publishableKey)
            throws InvalidRequestException, APIConnectionException, APIException, CardException,
            AuthenticationException, JSONException {
        final StripeResponse response = getStripeResponse(
                StripeRequest.createPost(
                        getApiUrl("3ds2/authenticate"),
                        authParams.toParamMap(),
                        RequestOptions.createForApi(publishableKey))
        );
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Stripe3ds2AuthResult.fromJson(new JSONObject(response.getResponseBody()));
    }

    void start3ds2Auth(@NonNull Stripe3ds2AuthParams authParams,
                       @NonNull String publishableKey,
                       @NonNull ApiResultCallback<Stripe3ds2AuthResult> callback) {
        new Start3ds2AuthTask(this, authParams, publishableKey, callback)
                .execute();
    }

    @VisibleForTesting
    boolean complete3ds2Auth(@NonNull String sourceId,
                             @NonNull String publishableKey)
            throws InvalidRequestException, APIConnectionException, APIException, CardException,
            AuthenticationException {
        final Map<String, String> params = new HashMap<>();
        params.put("source", sourceId);

        final StripeResponse response = getStripeResponse(
                StripeRequest.createPost(
                        getApiUrl("3ds2/challenge_complete"),
                        params,
                        RequestOptions.createForApi(publishableKey))
        );
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return response.isSuccessful();
    }

    void complete3ds2Auth(@NonNull String sourceId,
                          @NonNull String publishableKey,
                          @NonNull ApiResultCallback<Boolean> callback) {
        new Complete3ds2AuthTask(this, sourceId, publishableKey, callback)
                .execute();
    }

    /**
     * @return https://api.stripe.com/v1/tokens
     */
    @NonNull
    static String getTokensUrl() {
        return getApiUrl("tokens");
    }

    /**
     * @return https://api.stripe.com/v1/sources
     */
    @NonNull
    @VisibleForTesting
    static String getSourcesUrl() {
        return getApiUrl("sources");
    }

    /**
     * @return https://api.stripe.com/v1/payment_methods
     */
    @VisibleForTesting
    @NonNull
    static String getPaymentMethodsUrl() {
        return getApiUrl("payment_methods");
    }

    /**
     * @return https://api.stripe.com/v1/payment_intents/:id
     */
    @VisibleForTesting
    @NonNull
    static String getRetrievePaymentIntentUrl(@NonNull String paymentIntentId) {
        return getApiUrl("payment_intents/%s", paymentIntentId);
    }

    /**
     * @return https://api.stripe.com/v1/payment_intents/:id/confirm
     */
    @VisibleForTesting
    @NonNull
    static String getConfirmPaymentIntentUrl(@NonNull String paymentIntentId) {
        return getApiUrl("payment_intents/%s/confirm", paymentIntentId);
    }

    /**
     * @return https://api.stripe.com/v1/customers/:customer_id/sources
     */
    @VisibleForTesting
    @NonNull
    static String getAddCustomerSourceUrl(@NonNull String customerId) {
        return getApiUrl("customers/%s/sources", customerId);
    }

    /**
     * @return https://api.stripe.com/v1/customers/:customer_id/sources/:source_id
     */
    @VisibleForTesting
    @NonNull
    static String getDeleteCustomerSourceUrl(@NonNull String customerId, @NonNull String sourceId) {
        return getApiUrl("customers/%s/sources/%s", customerId, sourceId);
    }

    /**
     * @return https://api.stripe.com/v1/payment_methods/:id/attach
     */
    @VisibleForTesting
    @NonNull
    static String getAttachPaymentMethodUrl(@NonNull String paymentMethodId) {
        return getApiUrl("payment_methods/%s/attach", paymentMethodId);
    }

    /**
     * @return https://api.stripe.com/v1/payment_methods/:id/detach
     */
    @VisibleForTesting
    @NonNull
    String getDetachPaymentMethodUrl(@NonNull String paymentMethodId) {
        return getApiUrl("payment_methods/%s/detach", paymentMethodId);
    }

    /**
     * @return https://api.stripe.com/v1/customers/:id
     */
    @VisibleForTesting
    @NonNull
    static String getRetrieveCustomerUrl(@NonNull String customerId) {
        return getApiUrl("customers/%s", customerId);
    }

    /**
     * @return https://api.stripe.com/v1/sources/:id
     */
    @VisibleForTesting
    @NonNull
    static String getRetrieveSourceApiUrl(@NonNull String sourceId) {
        return getApiUrl("sources/%s", sourceId);
    }

    /**
     * @return https://api.stripe.com/v1/tokens/:id
     */
    @VisibleForTesting
    @NonNull
    static String getRetrieveTokenApiUrl(@NonNull String tokenId) {
        return getApiUrl("tokens/%s", tokenId);
    }

    /**
     * @return https://api.stripe.com/v1/issuing/cards/:id/pin
     */
    @VisibleForTesting
    @NonNull
    static String getIssuingCardPinUrl(@NonNull String cardId) {
        return getApiUrl("issuing/cards/%s/pin", cardId);
    }

    @NonNull
    private static String getApiUrl(@NonNull String path, @NonNull Object... args) {
        return getApiUrl(String.format(Locale.ENGLISH, path, args));
    }

    @NonNull
    private static String getApiUrl(@NonNull String path) {
        return String.format(Locale.ENGLISH, "%s/v1/%s", RequestExecutor.API_HOST, path);
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

    /**
     * @return true if a request was made and it was successful
     */
    private boolean fireAndForgetApiCall(@NonNull StripeRequest request) {
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
            final StripeResponse response = getStripeResponse(request);
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
    private StripeResponse getStripeResponse(@NonNull StripeRequest request)
            throws InvalidRequestException, APIConnectionException {
        return mRequestExecutor.execute(request);
    }

    private void handleAPIError(@Nullable String responseBody, int responseCode,
                                @Nullable String requestId)
            throws InvalidRequestException, AuthenticationException, CardException, APIException {
        final StripeError stripeError = ErrorParser.parseError(responseBody);
        switch (responseCode) {
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_NOT_FOUND: {
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
            case HttpURLConnection.HTTP_UNAUTHORIZED: {
                throw new AuthenticationException(stripeError.message, requestId, stripeError);
            }
            case HttpURLConnection.HTTP_PAYMENT_REQUIRED: {
                throw new CardException(
                        stripeError.message,
                        requestId,
                        stripeError.code,
                        stripeError.param,
                        stripeError.declineCode,
                        stripeError.charge,
                        stripeError
                );
            }
            case HttpURLConnection.HTTP_FORBIDDEN: {
                throw new PermissionException(stripeError.message, requestId, stripeError);
            }
            case 429: {
                throw new RateLimitException(stripeError.message, stripeError.param, requestId,
                        stripeError);
            }
            default: {
                throw new APIException(stripeError.message, requestId, responseCode, stripeError,
                        null);
            }
        }
    }

    @NonNull
    @VisibleForTesting
    StripeResponse requestData(@NonNull StripeRequest request)
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

        final String apiKey = request.options.getApiKey();
        if (StripeTextUtils.isBlank(apiKey)) {
            throw new AuthenticationException("No API key provided. (HINT: set your API key using" +
                    " 'Stripe.apiKey = <API-KEY>'. You can generate API keys from the Stripe" +
                    " web interface. See https://stripe.com/api for details or email " +
                    "support@stripe.com if you have questions.", null, null);
        }

        final StripeResponse response = getStripeResponse(request);
        if (response.hasErrorCode()) {
            handleAPIError(response.getResponseBody(), response.getResponseCode(),
                    response.getRequestId());
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
        final StripeResponse response = requestData(StripeRequest.createPost(url, params, options));
        return Token.fromString(response.getResponseBody());
    }

    private void logTelemetryData() {
        final Map<String, Object> params = mTelemetryClientUtil.createTelemetryMap();
        StripeNetworkUtils.removeNullAndEmptyParams(params);
        if (!mShouldLogRequest) {
            return;
        }

        final String guid = mTelemetryClientUtil.getHashedId();
        fireAndForgetApiCall(
                StripeRequest.createPost(RequestExecutor.FINGERPRINTING_ENDPOINT, params,
                        RequestOptions.createForFingerprinting(guid)));
    }

    private static final class Start3ds2AuthTask extends ApiOperation<Stripe3ds2AuthResult> {
        @NonNull private final StripeApiHandler mApiHandler;
        @NonNull private final Stripe3ds2AuthParams mParams;
        @NonNull private final String mPublishableKey;

        private Start3ds2AuthTask(@NonNull StripeApiHandler apiHandler,
                                  @NonNull Stripe3ds2AuthParams params,
                                  @NonNull String publishableKey,
                                  @NonNull ApiResultCallback<Stripe3ds2AuthResult> callback) {
            super(callback);
            mApiHandler = apiHandler;
            mParams = params;
            mPublishableKey = publishableKey;
        }

        @NonNull
        @Override
        Stripe3ds2AuthResult getResult() throws StripeException, JSONException {
            return mApiHandler.start3ds2Auth(mParams, mPublishableKey);
        }
    }

    private static final class Complete3ds2AuthTask extends ApiOperation<Boolean> {
        @NonNull private final StripeApiHandler mApiHandler;
        @NonNull private final String mSourceId;
        @NonNull private final String mPublishableKey;

        private Complete3ds2AuthTask(@NonNull StripeApiHandler apiHandler,
                                     @NonNull String sourceId,
                                     @NonNull String publishableKey,
                                     @NonNull ApiResultCallback<Boolean> callback) {
            super(callback);
            mApiHandler = apiHandler;
            mSourceId = sourceId;
            mPublishableKey = publishableKey;
        }

        @NonNull
        @Override
        Boolean getResult() throws StripeException {
            return mApiHandler.complete3ds2Auth(mSourceId, mPublishableKey);
        }
    }
}
