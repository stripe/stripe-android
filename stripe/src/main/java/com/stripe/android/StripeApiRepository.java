package com.stripe.android;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Pair;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.exception.PermissionException;
import com.stripe.android.exception.RateLimitException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.ConfirmSetupIntentParams;
import com.stripe.android.model.Customer;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.model.SetupIntent;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.Stripe3ds2AuthResult;
import com.stripe.android.model.Token;
import com.stripe.android.utils.ObjectUtils;

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
 * An implementation of {@link StripeRepository} that makes network requests to the Stripe API.
 */
final class StripeApiRepository implements StripeRepository {

    private static final String DNS_CACHE_TTL_PROPERTY_NAME = "networkaddress.cache.ttl";

    @NonNull private final AnalyticsDataFactory mAnalyticsDataFactory;
    @NonNull private final FingerprintRequestFactory mFingerprintRequestFactory;
    @NonNull private final StripeNetworkUtils mNetworkUtils;
    @NonNull private final ApiRequestExecutor mStripeApiRequestExecutor;
    @NonNull private final FireAndForgetRequestExecutor mFireAndForgetRequestExecutor;
    @Nullable private final AppInfo mAppInfo;

    StripeApiRepository(@NonNull Context context, @Nullable AppInfo appInfo) {
        this(context.getApplicationContext(), new StripeApiRequestExecutor(),
                new StripeFireAndForgetRequestExecutor(), appInfo);
    }

    @VisibleForTesting
    StripeApiRepository(@NonNull Context context,
                        @NonNull ApiRequestExecutor stripeApiRequestExecutor,
                        @NonNull FireAndForgetRequestExecutor fireAndForgetRequestExecutor,
                        @Nullable AppInfo appInfo) {
        this(context, stripeApiRequestExecutor, fireAndForgetRequestExecutor, appInfo,
                new FingerprintRequestFactory(context));
    }

    @VisibleForTesting
    StripeApiRepository(@NonNull Context context,
                        @NonNull ApiRequestExecutor stripeApiRequestExecutor,
                        @NonNull FireAndForgetRequestExecutor fireAndForgetRequestExecutor,
                        @Nullable AppInfo appInfo,
                        @NonNull FingerprintRequestFactory fingerprintRequestFactory) {
        mStripeApiRequestExecutor = stripeApiRequestExecutor;
        mFireAndForgetRequestExecutor = fireAndForgetRequestExecutor;
        mAnalyticsDataFactory = new AnalyticsDataFactory(context);
        mFingerprintRequestFactory = fingerprintRequestFactory;
        mNetworkUtils = new StripeNetworkUtils(context);
        mAppInfo = appInfo;
    }

    /**
     * Confirm a {@link PaymentIntent} using the provided {@link ConfirmPaymentIntentParams}
     *
     * @param confirmPaymentIntentParams contains the confirmation params
     * @return a {@link PaymentIntent} reflecting the updated state after applying the parameter
     * provided
     */
    @Nullable
    @Override
    public PaymentIntent confirmPaymentIntent(
            @NonNull ConfirmPaymentIntentParams confirmPaymentIntentParams,
            @NonNull ApiRequest.Options options)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        final Map<String, ?> paramMap = confirmPaymentIntentParams.toParamMap();
        mNetworkUtils.addUidToConfirmPaymentIntentParams(paramMap);

        try {
            fireFingerprintRequest();
            final SourceParams sourceParams = confirmPaymentIntentParams.getSourceParams();
            final String sourceType = sourceParams != null ? sourceParams.getType() : null;

            fireAnalyticsRequest(
                    mAnalyticsDataFactory.getPaymentIntentConfirmationParams(null,
                            options.apiKey, sourceType),
                    options.apiKey
            );
            final String paymentIntentId = PaymentIntent.parseIdFromClientSecret(
                    confirmPaymentIntentParams.getClientSecret());
            final StripeResponse response = makeApiRequest(ApiRequest.createPost(
                    getConfirmPaymentIntentUrl(paymentIntentId), paramMap, options, mAppInfo));
            return PaymentIntent.fromString(response.getResponseBody());
        } catch (CardException unexpected) {
            // This particular kind of exception should not be possible from a PaymentI API endpoint
            throw new APIException(unexpected.getMessage(), unexpected.getRequestId(),
                    unexpected.getStatusCode(), null, unexpected);
        }
    }

    /**
     * Retrieve a {@link PaymentIntent} using its client_secret
     *
     * @param clientSecret client_secret of the PaymentIntent to retrieve
     */
    @Nullable
    @Override
    public PaymentIntent retrievePaymentIntent(
            @NonNull String clientSecret,
            @NonNull ApiRequest.Options options)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        try {
            fireFingerprintRequest();
            fireAnalyticsRequest(
                    mAnalyticsDataFactory.getPaymentIntentRetrieveParams(null, options.apiKey),
                    options.apiKey);
            final String paymentIntentId = PaymentIntent.parseIdFromClientSecret(clientSecret);
            final StripeResponse response = makeApiRequest(
                    ApiRequest.createGet(getRetrievePaymentIntentUrl(paymentIntentId),
                            createClientSecretParam(clientSecret),
                            options,
                            mAppInfo));
            return PaymentIntent.fromString(response.getResponseBody());
        } catch (CardException unexpected) {
            // This particular kind of exception should not be possible from a PaymentI API endpoint
            throw new APIException(unexpected.getMessage(), unexpected.getRequestId(),
                    unexpected.getStatusCode(), null, unexpected);
        }
    }

    /**
     * Confirm a {@link SetupIntent} using the provided {@link ConfirmSetupIntentParams}
     *
     * @param confirmSetupIntentParams contains the confirmation params
     * @return a {@link SetupIntent} reflecting the updated state after applying the parameter
     * provided
     */
    @Nullable
    @Override
    public SetupIntent confirmSetupIntent(
            @NonNull ConfirmSetupIntentParams confirmSetupIntentParams,
            @NonNull ApiRequest.Options options)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        final Map<String, ?> paramMap = confirmSetupIntentParams.toParamMap();
        mNetworkUtils.addUidToConfirmPaymentIntentParams(paramMap);

        try {
            fireFingerprintRequest();
            final String setupIntentId = SetupIntent.parseIdFromClientSecret(
                    confirmSetupIntentParams.getClientSecret());
            final StripeResponse response = makeApiRequest(ApiRequest.createPost(
                    getConfirmSetupIntentUrl(setupIntentId), paramMap, options, mAppInfo));
            final SetupIntent setupIntent = SetupIntent.fromString(response.getResponseBody());

            final String paymentMethodType;
            final PaymentMethodCreateParams paymentMethodCreateParams =
                    confirmSetupIntentParams.getPaymentMethodCreateParams();
            if (paymentMethodCreateParams != null) {
                paymentMethodType = paymentMethodCreateParams.getTypeCode();
            } else {
                paymentMethodType = null;
            }
            fireAnalyticsRequest(
                    mAnalyticsDataFactory.getSetupIntentConfirmationParams(
                            options.apiKey, paymentMethodType),
                    options.apiKey
            );
            return setupIntent;
        } catch (CardException unexpected) {
            // This particular kind of exception should not be possible from a PaymentI API endpoint
            throw new APIException(unexpected.getMessage(), unexpected.getRequestId(),
                    unexpected.getStatusCode(), null, unexpected);
        }
    }

    /**
     * Retrieve a {@link SetupIntent} using its client_secret
     *
     * @param clientSecret client_secret of the SetupIntent to retrieve
     */
    @Nullable
    @Override
    public SetupIntent retrieveSetupIntent(
            @NonNull String clientSecret,
            @NonNull ApiRequest.Options options)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        try {
            fireFingerprintRequest();
            fireAnalyticsRequest(mAnalyticsDataFactory.getSetupIntentRetrieveParams(options.apiKey),
                    options.apiKey);
            final String setupIntentId = SetupIntent.parseIdFromClientSecret(
                    Objects.requireNonNull(clientSecret));
            final StripeResponse response = makeApiRequest(
                    ApiRequest.createGet(
                            getRetrieveSetupIntentUrl(setupIntentId),
                            createClientSecretParam(clientSecret),
                            options,
                            mAppInfo));
            return SetupIntent.fromString(response.getResponseBody());
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
    @Override
    public Source createSource(
            @NonNull SourceParams sourceParams,
            @NonNull ApiRequest.Options options)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        final Map<String, Object> requestParams = sourceParams.toParamMap();
        requestParams.putAll(mNetworkUtils.createUidParams());

        try {
            fireFingerprintRequest();
            fireAnalyticsRequest(
                    mAnalyticsDataFactory.getSourceCreationParams(null, options.apiKey,
                            sourceParams.getType()),
                    options.apiKey);
            final StripeResponse response = makeApiRequest(
                    ApiRequest.createPost(getSourcesUrl(), requestParams, options, mAppInfo));
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
    @Override
    public Source retrieveSource(
            @NonNull String sourceId,
            @NonNull String clientSecret,
            @NonNull ApiRequest.Options options)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        final Map<String, String> paramMap = SourceParams.createRetrieveSourceParams(clientSecret);
        try {
            final StripeResponse response = makeApiRequest(
                    ApiRequest.createGet(getRetrieveSourceApiUrl(sourceId), paramMap, options,
                            mAppInfo));
            return Source.fromString(response.getResponseBody());
        } catch (CardException unexpected) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw new APIException(unexpected.getMessage(), unexpected.getRequestId(),
                    unexpected.getStatusCode(), null, unexpected);
        }
    }

    @Nullable
    @Override
    public PaymentMethod createPaymentMethod(
            @NonNull PaymentMethodCreateParams paymentMethodCreateParams,
            @NonNull ApiRequest.Options options)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            APIException {
        final Map<String, Object> params = paymentMethodCreateParams.toParamMap();
        params.putAll(mNetworkUtils.createUidParams());
        fireFingerprintRequest();

        try {
            final StripeResponse response = makeApiRequest(
                    ApiRequest.createPost(getPaymentMethodsUrl(), params, options, mAppInfo));
            final PaymentMethod paymentMethod =
                    PaymentMethod.fromString(response.getResponseBody());

            fireAnalyticsRequest(
                    mAnalyticsDataFactory.createPaymentMethodCreationParams(
                            options.apiKey,
                            paymentMethod != null ? paymentMethod.id : null),
                    options.apiKey);

            return paymentMethod;
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
     * @param options a {@link ApiRequest.Options} object that contains connection data like the api
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
    @Override
    public Token createToken(
            @NonNull Map<String, ?> tokenParams,
            @NonNull ApiRequest.Options options,
            @NonNull @Token.TokenType String tokenType)
            throws AuthenticationException,
            InvalidRequestException,
            APIConnectionException,
            CardException,
            APIException {

        try {
            final List<String> loggingTokens =
                    (List<String>) tokenParams.get(AnalyticsDataFactory.FIELD_PRODUCT_USAGE);
            tokenParams.remove(AnalyticsDataFactory.FIELD_PRODUCT_USAGE);

            fireFingerprintRequest();

            fireAnalyticsRequest(
                    mAnalyticsDataFactory
                            .getTokenCreationParams(loggingTokens, options.apiKey, tokenType),
                    options.apiKey
            );
        } catch (ClassCastException classCastEx) {
            // This can only happen if someone puts a weird object in the map.
            tokenParams.remove(AnalyticsDataFactory.FIELD_PRODUCT_USAGE);
        }

        return requestToken(getTokensUrl(), tokenParams, options);
    }

    @Nullable
    @Override
    public Source addCustomerSource(
            @NonNull String customerId,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @NonNull ApiRequest.Options requestOptions)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final Map<String, Object> params = new HashMap<>();
        params.put("source", sourceId);

        fireAnalyticsRequest(
                mAnalyticsDataFactory
                        .getAddSourceParams(productUsageTokens, publishableKey, sourceType),
                // We use the public key to log, so we need different Options.
                publishableKey
        );

        final StripeResponse response = fireStripeApiRequest(
                ApiRequest.createPost(getAddCustomerSourceUrl(customerId),
                        params, requestOptions, mAppInfo)
        );
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Source.fromString(response.getResponseBody());
    }

    @Nullable
    @Override
    public Source deleteCustomerSource(
            @NonNull String customerId,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull ApiRequest.Options requestOptions)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        fireAnalyticsRequest(
                mAnalyticsDataFactory.getDeleteSourceParams(productUsageTokens, publishableKey),
                // We use the public key to log, so we need different Options.
                publishableKey
        );

        final StripeResponse response = fireStripeApiRequest(
                ApiRequest.createDelete(
                        getDeleteCustomerSourceUrl(customerId, sourceId),
                        requestOptions, mAppInfo)
        );

        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Source.fromString(response.getResponseBody());
    }

    @Nullable
    @Override
    public PaymentMethod attachPaymentMethod(
            @NonNull String customerId,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String paymentMethodId,
            @NonNull ApiRequest.Options requestOptions)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final Map<String, Object> params = new HashMap<>();
        params.put("customer", customerId);

        fireAnalyticsRequest(
                mAnalyticsDataFactory
                        .getAttachPaymentMethodParams(productUsageTokens, publishableKey),
                // We use the public key to log, so we need different Options.
                publishableKey
        );

        final StripeResponse response = fireStripeApiRequest(
                ApiRequest.createPost(
                        getAttachPaymentMethodUrl(paymentMethodId),
                        params,
                        requestOptions, mAppInfo)
        );
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return PaymentMethod.fromString(response.getResponseBody());
    }

    @Nullable
    @Override
    public PaymentMethod detachPaymentMethod(
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String paymentMethodId,
            @NonNull ApiRequest.Options requestOptions)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        fireAnalyticsRequest(
                mAnalyticsDataFactory
                        .getDetachPaymentMethodParams(productUsageTokens, publishableKey),
                // We use the public key to log, so we need different Options.
                publishableKey
        );

        final StripeResponse response = fireStripeApiRequest(
                ApiRequest.createPost(
                        getDetachPaymentMethodUrl(paymentMethodId),
                        requestOptions, mAppInfo)
        );
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return PaymentMethod.fromString(response.getResponseBody());
    }

    /**
     * Retrieve a Customer's {@link PaymentMethod}s
     */
    @NonNull
    @Override
    public List<PaymentMethod> getPaymentMethods(
            @NonNull String customerId,
            @NonNull String paymentMethodType,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull ApiRequest.Options requestOptions)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final Map<String, String> queryParams = new HashMap<>(2);
        queryParams.put("customer", customerId);
        queryParams.put("type", paymentMethodType);

        fireAnalyticsRequest(
                mAnalyticsDataFactory
                        .getDetachPaymentMethodParams(productUsageTokens, publishableKey),
                // We use the public key to log, so we need different Options.
                publishableKey
        );

        final StripeResponse response = fireStripeApiRequest(
                ApiRequest.createGet(
                        getPaymentMethodsUrl(),
                        queryParams,
                        requestOptions, mAppInfo)
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
    @Override
    public Customer setDefaultCustomerSource(
            @NonNull String customerId,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull String sourceId,
            @NonNull @Source.SourceType String sourceType,
            @NonNull ApiRequest.Options requestOptions)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final Map<String, Object> params = new HashMap<>();
        params.put("default_source", sourceId);

        fireAnalyticsRequest(
                mAnalyticsDataFactory.getEventLoggingParams(productUsageTokens, sourceType,
                        null, publishableKey, AnalyticsDataFactory.EventName.DEFAULT_SOURCE),
                publishableKey
        );

        final StripeResponse response = fireStripeApiRequest(
                ApiRequest.createPost(
                        getRetrieveCustomerUrl(customerId),
                        params,
                        requestOptions, mAppInfo)
        );

        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Customer.fromString(response.getResponseBody());
    }

    @Nullable
    @Override
    public Customer setCustomerShippingInfo(
            @NonNull String customerId,
            @NonNull String publishableKey,
            @NonNull List<String> productUsageTokens,
            @NonNull ShippingInformation shippingInformation,
            @NonNull ApiRequest.Options requestOptions)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final Map<String, Object> params = new HashMap<>();
        params.put("shipping", shippingInformation.toParamMap());

        fireAnalyticsRequest(
                mAnalyticsDataFactory.getEventLoggingParams(productUsageTokens,
                        publishableKey, AnalyticsDataFactory.EventName.SET_SHIPPING_INFO),
                publishableKey
        );

        final StripeResponse response = fireStripeApiRequest(
                ApiRequest.createPost(
                        getRetrieveCustomerUrl(customerId),
                        params,
                        requestOptions, mAppInfo)
        );
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Customer.fromString(response.getResponseBody());
    }

    @Nullable
    @Override
    public Customer retrieveCustomer(@NonNull String customerId,
                              @NonNull ApiRequest.Options requestOptions)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException {
        final StripeResponse response = fireStripeApiRequest(
                ApiRequest.createGet(getRetrieveCustomerUrl(customerId),
                        requestOptions, mAppInfo)
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
    @Override
    public String retrieveIssuingCardPin(
            @NonNull String cardId,
            @NonNull String verificationId,
            @NonNull String userOneTimeCode,
            @NonNull String ephemeralKeySecret)
            throws InvalidRequestException,
            APIConnectionException,
            APIException,
            AuthenticationException,
            CardException,
            JSONException {
        final Map<String, Map<String, String>> params = new HashMap<>();
        params.put("verification", createVerificationParam(verificationId, userOneTimeCode));

        final StripeResponse response = fireStripeApiRequest(
                ApiRequest.createGet(getIssuingCardPinUrl(cardId), params,
                        ApiRequest.Options.create(ephemeralKeySecret), mAppInfo)
        );
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        JSONObject jsonResponse = new JSONObject(response.getResponseBody());
        return jsonResponse.getString("pin");
    }

    @Override
    public void updateIssuingCardPin(
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

        final StripeResponse response = fireStripeApiRequest(
                ApiRequest.createPost(
                getIssuingCardPinUrl(cardId),
                params,
                ApiRequest.Options.create(ephemeralKeySecret), mAppInfo)
        );
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response);
    }

    @NonNull
    @VisibleForTesting
    Stripe3ds2AuthResult start3ds2Auth(@NonNull Stripe3ds2AuthParams authParams,
                                       @NonNull String stripeIntentId,
                                       @NonNull ApiRequest.Options requestOptions)
            throws InvalidRequestException, APIConnectionException, APIException, CardException,
            AuthenticationException, JSONException {
        fireAnalyticsRequest(
                mAnalyticsDataFactory.createAuthParams(
                        AnalyticsDataFactory.EventName.AUTH_3DS2_START,
                        stripeIntentId, requestOptions.apiKey),
                requestOptions.apiKey
        );

        final StripeResponse response = fireStripeApiRequest(
                ApiRequest.createPost(
                        getApiUrl("3ds2/authenticate"),
                        authParams.toParamMap(),
                        requestOptions,
                        mAppInfo
                )
        );
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return Stripe3ds2AuthResult.fromJson(new JSONObject(response.getResponseBody()));
    }

    @Override
    public void start3ds2Auth(@NonNull Stripe3ds2AuthParams authParams,
                       @NonNull String stripeIntentId,
                       @NonNull ApiRequest.Options requestOptions,
                       @NonNull ApiResultCallback<Stripe3ds2AuthResult> callback) {
        new Start3ds2AuthTask(this, authParams, stripeIntentId, requestOptions, callback)
                .execute();
    }

    @VisibleForTesting
    boolean complete3ds2Auth(@NonNull String sourceId,
                             @NonNull ApiRequest.Options requestOptions)
            throws InvalidRequestException, APIConnectionException, APIException, CardException,
            AuthenticationException {
        final Map<String, String> params = new HashMap<>();
        params.put("source", sourceId);

        final StripeResponse response = fireStripeApiRequest(
                ApiRequest.createPost(
                        getApiUrl("3ds2/challenge_complete"),
                        params,
                        requestOptions, mAppInfo)
        );
        convertErrorsToExceptionsAndThrowIfNecessary(response);
        return response.isOk();
    }

    @Override
    public void complete3ds2Auth(@NonNull String sourceId,
                          @NonNull ApiRequest.Options requestOptions,
                          @NonNull ApiResultCallback<Boolean> callback) {
        new Complete3ds2AuthTask(this, sourceId, requestOptions, callback)
                .execute();
    }

    /**
     * @return <code>https://api.stripe.com/v1/tokens</code>
     */
    @NonNull
    static String getTokensUrl() {
        return getApiUrl("tokens");
    }

    /**
     * @return <code>https://api.stripe.com/v1/sources</code>
     */
    @NonNull
    @VisibleForTesting
    static String getSourcesUrl() {
        return getApiUrl("sources");
    }

    /**
     * @return <code>https://api.stripe.com/v1/payment_methods</code>
     */
    @VisibleForTesting
    @NonNull
    static String getPaymentMethodsUrl() {
        return getApiUrl("payment_methods");
    }

    /**
     * @return <code>https://api.stripe.com/v1/payment_intents/:id</code>
     */
    @VisibleForTesting
    @NonNull
    static String getRetrievePaymentIntentUrl(@NonNull String paymentIntentId) {
        return getApiUrl("payment_intents/%s", paymentIntentId);
    }

    /**
     * @return <code>https://api.stripe.com/v1/payment_intents/:id/confirm</code>
     */
    @VisibleForTesting
    @NonNull
    static String getConfirmPaymentIntentUrl(@NonNull String paymentIntentId) {
        return getApiUrl("payment_intents/%s/confirm", paymentIntentId);
    }

    /**
     * @return <code>https://api.stripe.com/v1/payment_intents/:id</code>
     */
    @VisibleForTesting
    @NonNull
    static String getRetrieveSetupIntentUrl(@NonNull String setupIntentId) {
        return getApiUrl("setup_intents/%s", setupIntentId);
    }

    /**
     * @return <code>https://api.stripe.com/v1/payment_intents/:id/confirm</code>
     */
    @VisibleForTesting
    @NonNull
    static String getConfirmSetupIntentUrl(@NonNull String setupIntentId) {
        return getApiUrl("setup_intents/%s/confirm", setupIntentId);
    }

    /**
     * @return <code>https://api.stripe.com/v1/customers/:customer_id/sources</code>
     */
    @VisibleForTesting
    @NonNull
    static String getAddCustomerSourceUrl(@NonNull String customerId) {
        return getApiUrl("customers/%s/sources", customerId);
    }

    /**
     * @return <code>https://api.stripe.com/v1/customers/:customer_id/sources/:source_id</code>
     */
    @VisibleForTesting
    @NonNull
    static String getDeleteCustomerSourceUrl(@NonNull String customerId, @NonNull String sourceId) {
        return getApiUrl("customers/%s/sources/%s", customerId, sourceId);
    }

    /**
     * @return <code>https://api.stripe.com/v1/payment_methods/:id/attach</code>
     */
    @VisibleForTesting
    @NonNull
    static String getAttachPaymentMethodUrl(@NonNull String paymentMethodId) {
        return getApiUrl("payment_methods/%s/attach", paymentMethodId);
    }

    /**
     * @return <code>https://api.stripe.com/v1/payment_methods/:id/detach</code>
     */
    @VisibleForTesting
    @NonNull
    String getDetachPaymentMethodUrl(@NonNull String paymentMethodId) {
        return getApiUrl("payment_methods/%s/detach", paymentMethodId);
    }

    /**
     * @return <code>https://api.stripe.com/v1/customers/:id</code>
     */
    @VisibleForTesting
    @NonNull
    static String getRetrieveCustomerUrl(@NonNull String customerId) {
        return getApiUrl("customers/%s", customerId);
    }

    /**
     * @return <code>https://api.stripe.com/v1/sources/:id</code>
     */
    @VisibleForTesting
    @NonNull
    static String getRetrieveSourceApiUrl(@NonNull String sourceId) {
        return getApiUrl("sources/%s", sourceId);
    }

    /**
     * @return <code>https://api.stripe.com/v1/tokens/:id</code>
     */
    @VisibleForTesting
    @NonNull
    static String getRetrieveTokenApiUrl(@NonNull String tokenId) {
        return getApiUrl("tokens/%s", tokenId);
    }

    /**
     * @return <code>https://api.stripe.com/v1/issuing/cards/:id/pin</code>
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
        return String.format(Locale.ENGLISH, "%s/v1/%s", ApiRequest.API_HOST, path);
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
    private StripeResponse fireStripeApiRequest(@NonNull ApiRequest apiRequest)
            throws InvalidRequestException, APIConnectionException {
        return mStripeApiRequestExecutor.execute(apiRequest);
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
    StripeResponse makeApiRequest(@NonNull ApiRequest request)
            throws AuthenticationException, InvalidRequestException,
            APIConnectionException, CardException, APIException {
        final Pair<Boolean, String> dnsCacheData = disableDnsCache();

        final StripeResponse response = fireStripeApiRequest(request);
        if (response.hasErrorCode()) {
            handleAPIError(response.getResponseBody(), response.getResponseCode(),
                    response.getRequestId());
        }

        resetDnsCacheTtl(dnsCacheData);

        return response;
    }

    private void makeFireAndForgetRequest(@NonNull StripeRequest request) {
        mFireAndForgetRequestExecutor.executeAsync(request);
    }

    @NonNull
    private Pair<Boolean, String> disableDnsCache() {
        try {
            final String originalDNSCacheTtl = Security.getProperty(DNS_CACHE_TTL_PROPERTY_NAME);
            // disable DNS cache
            Security.setProperty(DNS_CACHE_TTL_PROPERTY_NAME, "0");
            return Pair.create(true, originalDNSCacheTtl);
        } catch (SecurityException se) {
            return Pair.create(false, null);
        }
    }

    /**
     * @param dnsCacheData first object - flag to reset {@link #DNS_CACHE_TTL_PROPERTY_NAME}
     *                     second object - the original DNS cache TTL value
     */
    private void resetDnsCacheTtl(@NonNull Pair<Boolean, String> dnsCacheData) {
        if (dnsCacheData.first) {
            // value unspecified by implementation
            // DNS_CACHE_TTL_PROPERTY_NAME of -1 = cache forever
            Security.setProperty(DNS_CACHE_TTL_PROPERTY_NAME,
                    ObjectUtils.getOrDefault(dnsCacheData.second, "-1"));
        }
    }

    @Nullable
    private Token requestToken(
            @NonNull String url,
            @NonNull Map<String, ?> params,
            @NonNull ApiRequest.Options options)
            throws AuthenticationException, InvalidRequestException,
            APIConnectionException, CardException, APIException {
        final StripeResponse response = makeApiRequest(ApiRequest.createPost(url, params, options,
                mAppInfo));
        return Token.fromString(response.getResponseBody());
    }

    private void fireFingerprintRequest() {
        makeFireAndForgetRequest(mFingerprintRequestFactory.create());
    }

    @VisibleForTesting
    void fireAnalyticsRequest(
            @NonNull Map<String, Object> loggingMap,
            @NonNull String publishableKey) {
        makeFireAndForgetRequest(
                AnalyticsRequest.create(
                        loggingMap,
                        ApiRequest.Options.create(publishableKey),
                        mAppInfo)
        );
    }

    @NonNull
    private Map<String, String> createClientSecretParam(@NonNull String clientSecret) {
        final Map<String, String> paramMap = new HashMap<>();
        paramMap.put("client_secret", clientSecret);
        return paramMap;
    }

    private static final class Start3ds2AuthTask extends ApiOperation<Stripe3ds2AuthResult> {
        @NonNull private final StripeApiRepository mStripeApiRepository;
        @NonNull private final Stripe3ds2AuthParams mParams;
        @NonNull private final String mStripeIntentId;
        @NonNull private final ApiRequest.Options mRequestOptions;

        private Start3ds2AuthTask(@NonNull StripeApiRepository stripeApiRepository,
                                  @NonNull Stripe3ds2AuthParams params,
                                  @NonNull String stripeIntentId,
                                  @NonNull ApiRequest.Options requestOptions,
                                  @NonNull ApiResultCallback<Stripe3ds2AuthResult> callback) {
            super(callback);
            mStripeApiRepository = stripeApiRepository;
            mParams = params;
            mStripeIntentId = stripeIntentId;
            mRequestOptions = requestOptions;
        }

        @NonNull
        @Override
        Stripe3ds2AuthResult getResult() throws StripeException, JSONException {
            return mStripeApiRepository.start3ds2Auth(mParams, mStripeIntentId, mRequestOptions);
        }
    }

    private static final class Complete3ds2AuthTask extends ApiOperation<Boolean> {
        @NonNull private final StripeApiRepository mStripeApiRepository;
        @NonNull private final String mSourceId;
        @NonNull private final ApiRequest.Options mRequestOptions;

        private Complete3ds2AuthTask(@NonNull StripeApiRepository stripeApiRepository,
                                     @NonNull String sourceId,
                                     @NonNull ApiRequest.Options requestOptions,
                                     @NonNull ApiResultCallback<Boolean> callback) {
            super(callback);
            mStripeApiRepository = stripeApiRepository;
            mSourceId = sourceId;
            mRequestOptions = requestOptions;
        }

        @NonNull
        @Override
        Boolean getResult() throws StripeException {
            return mStripeApiRepository.complete3ds2Auth(mSourceId, mRequestOptions);
        }
    }
}
