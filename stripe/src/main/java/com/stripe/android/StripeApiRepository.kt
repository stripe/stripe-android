package com.stripe.android

import android.content.Context
import android.support.annotation.VisibleForTesting
import android.util.Pair
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.CardException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.exception.PermissionException
import com.stripe.android.exception.RateLimitException
import com.stripe.android.exception.StripeException
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.android.model.Stripe3ds2AuthResult
import com.stripe.android.model.Token
import com.stripe.android.utils.ObjectUtils
import java.net.HttpURLConnection
import java.security.Security
import java.util.Locale
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * An implementation of [StripeRepository] that makes network requests to the Stripe API.
 */
internal class StripeApiRepository @VisibleForTesting @JvmOverloads constructor(
    context: Context,
    private val appInfo: AppInfo? = null,
    private val stripeApiRequestExecutor: ApiRequestExecutor = StripeApiRequestExecutor(),
    private val fireAndForgetRequestExecutor: FireAndForgetRequestExecutor =
        StripeFireAndForgetRequestExecutor(),
    private val fingerprintRequestFactory: FingerprintRequestFactory =
        FingerprintRequestFactory(context)
) : StripeRepository {

    private val analyticsDataFactory: AnalyticsDataFactory = AnalyticsDataFactory.create(context)
    private val networkUtils: StripeNetworkUtils = StripeNetworkUtils(context)

    /**
     * Confirm a [PaymentIntent] using the provided [ConfirmPaymentIntentParams]
     *
     * @param confirmPaymentIntentParams contains the confirmation params
     * @return a [PaymentIntent] reflecting the updated state after applying the parameter
     * provided
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    override fun confirmPaymentIntent(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        options: ApiRequest.Options
    ): PaymentIntent? {
        val paramMap = confirmPaymentIntentParams.toParamMap()
        networkUtils.addUidToConfirmPaymentIntentParams(paramMap)

        try {
            fireFingerprintRequest()
            val sourceParams = confirmPaymentIntentParams.sourceParams
            val sourceType = sourceParams?.type

            fireAnalyticsRequest(
                analyticsDataFactory.getPaymentIntentConfirmationParams(null,
                    options.apiKey, sourceType),
                options.apiKey
            )
            val paymentIntentId = PaymentIntent.parseIdFromClientSecret(
                confirmPaymentIntentParams.clientSecret)
            val response = makeApiRequest(ApiRequest.createPost(
                getConfirmPaymentIntentUrl(paymentIntentId), paramMap, options, appInfo))
            return PaymentIntent.fromString(response.responseBody)
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a PaymentI API endpoint
            throw APIException(unexpected.message, unexpected.requestId,
                unexpected.statusCode, null, unexpected)
        }
    }

    /**
     * Retrieve a [PaymentIntent] using its client_secret
     *
     * @param clientSecret client_secret of the PaymentIntent to retrieve
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    override fun retrievePaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options
    ): PaymentIntent? {
        try {
            fireFingerprintRequest()
            fireAnalyticsRequest(
                analyticsDataFactory.getPaymentIntentRetrieveParams(null, options.apiKey),
                options.apiKey)
            val paymentIntentId = PaymentIntent.parseIdFromClientSecret(clientSecret)
            val response = makeApiRequest(
                ApiRequest.createGet(getRetrievePaymentIntentUrl(paymentIntentId),
                    createClientSecretParam(clientSecret),
                    options,
                    appInfo))
            return PaymentIntent.fromString(response.responseBody)
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a PaymentI API endpoint
            throw APIException(unexpected.message, unexpected.requestId,
                unexpected.statusCode, null, unexpected)
        }
    }

    /**
     * Confirm a [SetupIntent] using the provided [ConfirmSetupIntentParams]
     *
     * @param confirmSetupIntentParams contains the confirmation params
     * @return a [SetupIntent] reflecting the updated state after applying the parameter
     * provided
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    override fun confirmSetupIntent(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        options: ApiRequest.Options
    ): SetupIntent? {
        val paramMap = confirmSetupIntentParams.toParamMap()
        networkUtils.addUidToConfirmPaymentIntentParams(paramMap)

        try {
            fireFingerprintRequest()
            val setupIntentId = SetupIntent.parseIdFromClientSecret(
                confirmSetupIntentParams.clientSecret)
            val response = makeApiRequest(
                ApiRequest.createPost(
                    getConfirmSetupIntentUrl(setupIntentId),
                    paramMap,
                    options,
                    appInfo
                )
            )
            val setupIntent = SetupIntent.fromString(response.responseBody)

            fireAnalyticsRequest(
                analyticsDataFactory.getSetupIntentConfirmationParams(
                    options.apiKey,
                    confirmSetupIntentParams.paymentMethodCreateParams?.typeCode
                ),
                options.apiKey
            )
            return setupIntent
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a PaymentI API endpoint
            throw APIException(unexpected.message, unexpected.requestId,
                unexpected.statusCode, null, unexpected)
        }
    }

    /**
     * Retrieve a [SetupIntent] using its client_secret
     *
     * @param clientSecret client_secret of the SetupIntent to retrieve
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    override fun retrieveSetupIntent(
        clientSecret: String,
        options: ApiRequest.Options
    ): SetupIntent? {
        try {
            fireFingerprintRequest()
            fireAnalyticsRequest(
                analyticsDataFactory.getSetupIntentRetrieveParams(options.apiKey),
                options.apiKey
            )
            val setupIntentId = SetupIntent.parseIdFromClientSecret(clientSecret)
            val response = makeApiRequest(
                ApiRequest.createGet(
                    getRetrieveSetupIntentUrl(setupIntentId),
                    createClientSecretParam(clientSecret),
                    options,
                    appInfo
                )
            )
            return SetupIntent.fromString(response.responseBody)
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a PaymentI API endpoint
            throw APIException(unexpected.message, unexpected.requestId,
                unexpected.statusCode, null, unexpected)
        }
    }

    /**
     * Create a [Source] using the input [SourceParams].
     *
     * @param sourceParams a [SourceParams] object with [Source] creation params
     * @return a [Source] if one could be created from the input params,
     * or `null` if not
     * @throws AuthenticationException if there is a problem authenticating to the Stripe API
     * @throws InvalidRequestException if one or more of the parameters is incorrect
     * @throws APIConnectionException if there is a problem connecting to the Stripe API
     * @throws APIException for unknown Stripe API errors. These should be rare.
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    override fun createSource(
        sourceParams: SourceParams,
        options: ApiRequest.Options
    ): Source? {
        val requestParams = sourceParams.toParamMap()
        requestParams.putAll(networkUtils.createUidParams())

        try {
            fireFingerprintRequest()
            fireAnalyticsRequest(
                analyticsDataFactory.getSourceCreationParams(null, options.apiKey,
                    sourceParams.type),
                options.apiKey)
            val response = makeApiRequest(
                ApiRequest.createPost(sourcesUrl, requestParams, options, appInfo))
            return Source.fromString(response.responseBody)
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException(unexpected.message, unexpected.requestId,
                unexpected.statusCode, null, unexpected)
        }
    }

    /**
     * Retrieve an existing [Source] object from the server.
     *
     * @param sourceId the [Source.getId] field for the Source to query
     * @param clientSecret the [Source.getClientSecret] field for the Source to query
     * @return a [Source] if one could be retrieved for the input params, or `null` if
     * no such Source could be found.
     *
     * @throws AuthenticationException if there is a problem authenticating to the Stripe API
     * @throws InvalidRequestException if one or more of the parameters is incorrect
     * @throws APIConnectionException if there is a problem connecting to the Stripe API
     * @throws APIException for unknown Stripe API errors. These should be rare.
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    override fun retrieveSource(
        sourceId: String,
        clientSecret: String,
        options: ApiRequest.Options
    ): Source? {
        try {
            val response = makeApiRequest(
                ApiRequest.createGet(
                    getRetrieveSourceApiUrl(sourceId),
                    SourceParams.createRetrieveSourceParams(clientSecret),
                    options,
                    appInfo
                )
            )
            return Source.fromString(response.responseBody)
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException(unexpected.message, unexpected.requestId,
                unexpected.statusCode, null, unexpected)
        }
    }

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    override fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: ApiRequest.Options
    ): PaymentMethod? {
        fireFingerprintRequest()

        try {
            val response = makeApiRequest(
                ApiRequest.createPost(
                    paymentMethodsUrl,
                    paymentMethodCreateParams.toParamMap()
                        .plus(networkUtils.createUidParams()),
                    options,
                    appInfo
                )
            )
            val paymentMethod = PaymentMethod.fromString(response.responseBody)

            fireAnalyticsRequest(
                analyticsDataFactory.createPaymentMethodCreationParams(
                    options.apiKey,
                    paymentMethod?.id),
                options.apiKey)

            return paymentMethod
        } catch (unexpected: CardException) {
            throw APIException(unexpected.message, unexpected.requestId,
                unexpected.statusCode, null, unexpected)
        }
    }

    /**
     * Create a [Token] using the input token parameters.
     *
     * @param tokenParams a mapped set of parameters representing the object for which this token
     * is being created
     * @param options a [ApiRequest.Options] object that contains connection data like the api
     * key, api version, etc
     * @param tokenType the [Token.TokenType] being created
     * @return a [Token] that can be used to perform other operations with this card
     * @throws AuthenticationException if there is a problem authenticating to the Stripe API
     * @throws InvalidRequestException if one or more of the parameters is incorrect
     * @throws APIConnectionException if there is a problem connecting to the Stripe API
     * @throws CardException if there is a problem with the card information
     * @throws APIException for unknown Stripe API errors. These should be rare.
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, CardException::class, APIException::class)
    override fun createToken(
        tokenParams: Map<String, *>,
        options: ApiRequest.Options,
        @Token.TokenType tokenType: String
    ): Token? {
        try {
            fireFingerprintRequest()

            fireAnalyticsRequest(
                analyticsDataFactory.getTokenCreationParams(
                    tokenParams[AnalyticsDataFactory.FIELD_PRODUCT_USAGE] as List<String>?,
                    options.apiKey,
                    tokenType
                ),
                options.apiKey
            )
        } catch (classCastEx: ClassCastException) {
            // This can only happen if someone puts a weird object in the map.
        }

        return requestToken(
            tokensUrl,
            tokenParams.minus(AnalyticsDataFactory.FIELD_PRODUCT_USAGE),
            options
        )
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class, AuthenticationException::class, CardException::class)
    override fun addCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: List<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options
    ): Source? {
        fireAnalyticsRequest(
            analyticsDataFactory
                .getAddSourceParams(productUsageTokens, publishableKey, sourceType),
            // We use the public key to log, so we need different Options.
            publishableKey
        )

        val response = fireStripeApiRequest(
            ApiRequest.createPost(
                getAddCustomerSourceUrl(customerId),
                mapOf("source" to sourceId),
                requestOptions,
                appInfo
            )
        )
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response)
        return Source.fromString(response.responseBody)
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun deleteCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: List<String>,
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Source? {
        fireAnalyticsRequest(
            analyticsDataFactory.getDeleteSourceParams(productUsageTokens, publishableKey),
            // We use the public key to log, so we need different Options.
            publishableKey
        )

        val response = fireStripeApiRequest(
            ApiRequest.createDelete(
                getDeleteCustomerSourceUrl(customerId, sourceId),
                requestOptions, appInfo)
        )

        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response)
        return Source.fromString(response.responseBody)
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun attachPaymentMethod(
        customerId: String,
        publishableKey: String,
        productUsageTokens: List<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): PaymentMethod? {
        fireAnalyticsRequest(
            analyticsDataFactory
                .getAttachPaymentMethodParams(productUsageTokens, publishableKey),
            // We use the public key to log, so we need different Options.
            publishableKey
        )

        val response = fireStripeApiRequest(
            ApiRequest.createPost(
                getAttachPaymentMethodUrl(paymentMethodId),
                mapOf("customer" to customerId),
                requestOptions, appInfo
            )
        )
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response)
        return PaymentMethod.fromString(response.responseBody)
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun detachPaymentMethod(
        publishableKey: String,
        productUsageTokens: List<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): PaymentMethod? {
        fireAnalyticsRequest(
            analyticsDataFactory
                .getDetachPaymentMethodParams(productUsageTokens, publishableKey),
            // We use the public key to log, so we need different Options.
            publishableKey
        )

        val response = fireStripeApiRequest(
            ApiRequest.createPost(
                getDetachPaymentMethodUrl(paymentMethodId),
                requestOptions, appInfo)
        )
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response)
        return PaymentMethod.fromString(response.responseBody)
    }

    /**
     * Retrieve a Customer's [PaymentMethod]s
     */
    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun getPaymentMethods(
        customerId: String,
        paymentMethodType: String,
        publishableKey: String,
        productUsageTokens: List<String>,
        requestOptions: ApiRequest.Options
    ): List<PaymentMethod> {
        val queryParams = mapOf(
            "customer" to customerId,
            "type" to paymentMethodType
        )

        fireAnalyticsRequest(
            analyticsDataFactory
                .getDetachPaymentMethodParams(productUsageTokens, publishableKey),
            // We use the public key to log, so we need different Options.
            publishableKey
        )

        val response = fireStripeApiRequest(
            ApiRequest.createGet(
                paymentMethodsUrl,
                queryParams,
                requestOptions, appInfo)
        )
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response)

        val data: JSONArray
        try {
            data = JSONObject(response.responseBody).optJSONArray("data")
        } catch (e: JSONException) {
            return emptyList()
        }

        return (0 until data.length()).mapNotNull {
            PaymentMethod.fromJson(data.optJSONObject(it))
        }
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun setDefaultCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: List<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options
    ): Customer? {
        fireAnalyticsRequest(
            analyticsDataFactory.getEventLoggingParams(
                eventName = AnalyticsDataFactory.EventName.DEFAULT_SOURCE,
                publishableKey = publishableKey,
                productUsageTokens = productUsageTokens,
                sourceType = sourceType
            ),
            publishableKey
        )

        val response = fireStripeApiRequest(
            ApiRequest.createPost(
                getRetrieveCustomerUrl(customerId),
                mapOf("default_source" to sourceId),
                requestOptions,
                appInfo
            )
        )

        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response)
        return Customer.fromString(response.responseBody)
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun setCustomerShippingInfo(
        customerId: String,
        publishableKey: String,
        productUsageTokens: List<String>,
        shippingInformation: ShippingInformation,
        requestOptions: ApiRequest.Options
    ): Customer? {
        fireAnalyticsRequest(
            analyticsDataFactory.getEventLoggingParams(
                AnalyticsDataFactory.EventName.SET_SHIPPING_INFO,
                publishableKey,
                productUsageTokens = productUsageTokens
            ),
            publishableKey
        )

        val response = fireStripeApiRequest(
            ApiRequest.createPost(
                getRetrieveCustomerUrl(customerId),
                mapOf("shipping" to shippingInformation.toParamMap()),
                requestOptions, appInfo)
        )
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response)
        return Customer.fromString(response.responseBody)
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun retrieveCustomer(
        customerId: String,
        requestOptions: ApiRequest.Options
    ): Customer? {
        val response = fireStripeApiRequest(
            ApiRequest.createGet(getRetrieveCustomerUrl(customerId),
                requestOptions, appInfo)
        )
        convertErrorsToExceptionsAndThrowIfNecessary(response)
        return Customer.fromString(response.responseBody)
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class, JSONException::class)
    override fun retrieveIssuingCardPin(
        cardId: String,
        verificationId: String,
        userOneTimeCode: String,
        ephemeralKeySecret: String
    ): String {
        val response = fireStripeApiRequest(
            ApiRequest.createGet(
                getIssuingCardPinUrl(cardId),
                mapOf("verification" to createVerificationParam(verificationId, userOneTimeCode)),
                ApiRequest.Options.create(ephemeralKeySecret),
                appInfo
            )
        )
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response)
        val jsonResponse = JSONObject(response.responseBody)
        return jsonResponse.getString("pin")
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun updateIssuingCardPin(
        cardId: String,
        newPin: String,
        verificationId: String,
        userOneTimeCode: String,
        ephemeralKeySecret: String
    ) {
        val response = fireStripeApiRequest(
            ApiRequest.createPost(
                getIssuingCardPinUrl(cardId),
                mapOf(
                    "verification" to createVerificationParam(verificationId, userOneTimeCode),
                    "pin" to newPin
                ),
                ApiRequest.Options.create(ephemeralKeySecret), appInfo)
        )
        // Method throws if errors are found, so no return value occurs.
        convertErrorsToExceptionsAndThrowIfNecessary(response)
    }

    @VisibleForTesting
    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        CardException::class, AuthenticationException::class, JSONException::class)
    fun start3ds2Auth(
        authParams: Stripe3ds2AuthParams,
        stripeIntentId: String,
        requestOptions: ApiRequest.Options
    ): Stripe3ds2AuthResult {
        fireAnalyticsRequest(
            analyticsDataFactory.createAuthParams(
                AnalyticsDataFactory.EventName.AUTH_3DS2_START,
                stripeIntentId, requestOptions.apiKey),
            requestOptions.apiKey
        )

        val response = fireStripeApiRequest(
            ApiRequest.createPost(
                getApiUrl("3ds2/authenticate"),
                authParams.toParamMap(),
                requestOptions,
                appInfo
            )
        )
        convertErrorsToExceptionsAndThrowIfNecessary(response)
        return Stripe3ds2AuthResult.fromJson(JSONObject(response.responseBody))
    }

    override fun start3ds2Auth(
        authParams: Stripe3ds2AuthParams,
        stripeIntentId: String,
        requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<Stripe3ds2AuthResult>
    ) {
        Start3ds2AuthTask(this, authParams, stripeIntentId, requestOptions, callback)
            .execute()
    }

    @VisibleForTesting
    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        CardException::class, AuthenticationException::class)
    fun complete3ds2Auth(sourceId: String, requestOptions: ApiRequest.Options): Boolean {
        val response = fireStripeApiRequest(
            ApiRequest.createPost(
                getApiUrl("3ds2/challenge_complete"),
                mapOf("source" to sourceId),
                requestOptions,
                appInfo
            )
        )
        convertErrorsToExceptionsAndThrowIfNecessary(response)
        return response.isOk
    }

    override fun complete3ds2Auth(
        sourceId: String,
        requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<Boolean>
    ) {
        Complete3ds2AuthTask(this, sourceId, requestOptions, callback)
            .execute()
    }

    /**
     * @return `https://api.stripe.com/v1/payment_methods/:id/detach`
     */
    @VisibleForTesting
    fun getDetachPaymentMethodUrl(paymentMethodId: String): String {
        return getApiUrl("payment_methods/%s/detach", paymentMethodId)
    }

    @Throws(InvalidRequestException::class, APIException::class, AuthenticationException::class,
        CardException::class)
    private fun convertErrorsToExceptionsAndThrowIfNecessary(response: StripeResponse) {
        val rCode = response.responseCode
        val rBody = response.responseBody
        val requestId = response.responseHeaders?.get("Request-Id")?.firstOrNull()

        if (rCode < 200 || rCode >= 300) {
            handleAPIError(rBody, rCode, requestId)
        }
    }

    /**
     * Converts a string-keyed [Map] into a [JSONObject]. This will cause a
     * [ClassCastException] if any sub-map has keys that are not [Strings][String].
     *
     * @param mapObject the [Map] that you'd like in JSON form
     * @return a [JSONObject] representing the input map, or `null` if the input
     * object is `null`
     */
    private fun mapToJsonObject(mapObject: Map<String, *>?): JSONObject? {
        if (mapObject == null) {
            return null
        }
        val jsonObject = JSONObject()
        for (key in mapObject.keys) {
            val value = mapObject[key] ?: continue

            try {
                if (value is Map<*, *>) {
                    try {
                        val mapValue = value as Map<String, Any>
                        jsonObject.put(key, mapToJsonObject(mapValue))
                    } catch (classCastException: ClassCastException) {
                        // We don't include the item in the JSONObject if the keys are not Strings.
                    }
                } else if (value is List<*>) {
                    jsonObject.put(key, listToJsonArray(value as List<Any>))
                } else if (value is Number || value is Boolean) {
                    jsonObject.put(key, value)
                } else {
                    jsonObject.put(key, value.toString())
                }
            } catch (jsonException: JSONException) {
                // Simply skip this value
            }
        }
        return jsonObject
    }

    /**
     * Converts a [List] into a [JSONArray]. A [ClassCastException] will be
     * thrown if any object in the list (or any sub-list or sub-map) is a [Map] whose keys
     * are not [Strings][String].
     *
     * @param values a [List] of values to be put in a [JSONArray]
     * @return a [JSONArray], or `null` if the input was `null`
     */
    private fun listToJsonArray(values: List<*>?): JSONArray? {
        if (values == null) {
            return null
        }

        val jsonArray = JSONArray()
        for (`object` in values) {
            if (`object` is Map<*, *>) {
                // We are ignoring type erasure here and crashing on bad input.
                // Now that this method is not public, we have more control on what is
                // passed to it.
                val mapObject = `object` as Map<String, Any>
                jsonArray.put(mapToJsonObject(mapObject))
            } else if (`object` is List<*>) {
                jsonArray.put(listToJsonArray(`object`))
            } else if (`object` is Number || `object` is Boolean) {
                jsonArray.put(`object`)
            } else {
                jsonArray.put(`object`.toString())
            }
        }
        return jsonArray
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class)
    private fun fireStripeApiRequest(apiRequest: ApiRequest): StripeResponse {
        return stripeApiRequestExecutor.execute(apiRequest)
    }

    @Throws(InvalidRequestException::class, AuthenticationException::class, CardException::class,
        APIException::class)
    private fun handleAPIError(responseBody: String?, responseCode: Int, requestId: String?) {
        val stripeError = ErrorParser.parseError(responseBody)
        when (responseCode) {
            HttpURLConnection.HTTP_BAD_REQUEST, HttpURLConnection.HTTP_NOT_FOUND -> {
                throw InvalidRequestException(
                    stripeError.message,
                    stripeError.param,
                    requestId,
                    responseCode,
                    stripeError.code,
                    stripeError.declineCode,
                    stripeError, null)
            }
            HttpURLConnection.HTTP_UNAUTHORIZED -> {
                throw AuthenticationException(stripeError.message, requestId, stripeError)
            }
            HttpURLConnection.HTTP_PAYMENT_REQUIRED -> {
                throw CardException(
                    stripeError.message,
                    requestId,
                    stripeError.code,
                    stripeError.param,
                    stripeError.declineCode,
                    stripeError.charge,
                    stripeError
                )
            }
            HttpURLConnection.HTTP_FORBIDDEN -> {
                throw PermissionException(stripeError.message, requestId, stripeError)
            }
            429 -> {
                throw RateLimitException(stripeError.message, stripeError.param, requestId,
                    stripeError)
            }
            else -> {
                throw APIException(stripeError.message, requestId, responseCode, stripeError, null)
            }
        }
    }

    @VisibleForTesting
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, CardException::class, APIException::class)
    fun makeApiRequest(request: ApiRequest): StripeResponse {
        val dnsCacheData = disableDnsCache()

        val response = fireStripeApiRequest(request)
        if (response.hasErrorCode()) {
            handleAPIError(response.responseBody, response.responseCode,
                response.requestId)
        }

        resetDnsCacheTtl(dnsCacheData)

        return response
    }

    private fun makeFireAndForgetRequest(request: StripeRequest) {
        fireAndForgetRequestExecutor.executeAsync(request)
    }

    private fun disableDnsCache(): Pair<Boolean, String> {
        return try {
            val originalDNSCacheTtl = Security.getProperty(DNS_CACHE_TTL_PROPERTY_NAME)
            // disable DNS cache
            Security.setProperty(DNS_CACHE_TTL_PROPERTY_NAME, "0")
            Pair.create(true, originalDNSCacheTtl)
        } catch (se: SecurityException) {
            Pair.create(false, null)
        }
    }

    /**
     * @param dnsCacheData first object - flag to reset [.DNS_CACHE_TTL_PROPERTY_NAME]
     * second object - the original DNS cache TTL value
     */
    private fun resetDnsCacheTtl(dnsCacheData: Pair<Boolean, String>) {
        if (dnsCacheData.first) {
            // value unspecified by implementation
            // DNS_CACHE_TTL_PROPERTY_NAME of -1 = cache forever
            Security.setProperty(DNS_CACHE_TTL_PROPERTY_NAME,
                ObjectUtils.getOrDefault(dnsCacheData.second, "-1"))
        }
    }

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, CardException::class, APIException::class)
    private fun requestToken(
        url: String,
        params: Map<String, *>,
        options: ApiRequest.Options
    ): Token? {
        val response = makeApiRequest(
            ApiRequest.createPost(url, params, options, appInfo)
        )
        return Token.fromString(response.responseBody)
    }

    private fun fireFingerprintRequest() {
        makeFireAndForgetRequest(fingerprintRequestFactory.create())
    }

    @VisibleForTesting
    fun fireAnalyticsRequest(
        loggingMap: Map<String, Any>,
        publishableKey: String
    ) {
        makeFireAndForgetRequest(
            AnalyticsRequest.create(
                loggingMap,
                ApiRequest.Options.create(publishableKey),
                appInfo
            )
        )
    }

    private fun createClientSecretParam(clientSecret: String): Map<String, String> {
        return mapOf("client_secret" to clientSecret)
    }

    private class Start3ds2AuthTask constructor(
        private val mStripeApiRepository: StripeApiRepository,
        private val mParams: Stripe3ds2AuthParams,
        private val mStripeIntentId: String,
        private val mRequestOptions: ApiRequest.Options,
        callback: ApiResultCallback<Stripe3ds2AuthResult>
    ) : ApiOperation<Stripe3ds2AuthResult>(callback) {
        @Throws(StripeException::class, JSONException::class)
        internal override fun getResult(): Stripe3ds2AuthResult {
            return mStripeApiRepository.start3ds2Auth(mParams, mStripeIntentId, mRequestOptions)
        }
    }

    private class Complete3ds2AuthTask constructor(
        private val mStripeApiRepository: StripeApiRepository,
        private val mSourceId: String,
        private val mRequestOptions: ApiRequest.Options,
        callback: ApiResultCallback<Boolean>
    ) : ApiOperation<Boolean>(callback) {
        @Throws(StripeException::class)
        internal override fun getResult(): Boolean {
            return mStripeApiRepository.complete3ds2Auth(mSourceId, mRequestOptions)
        }
    }

    companion object {

        private const val DNS_CACHE_TTL_PROPERTY_NAME = "networkaddress.cache.ttl"

        private fun createVerificationParam(
            verificationId: String,
            userOneTimeCode: String
        ): Map<String, String> {
            return mapOf(
                "id" to verificationId,
                "one_time_code" to userOneTimeCode
            )
        }

        /**
         * @return `https://api.stripe.com/v1/tokens`
         */
        @JvmStatic
        val tokensUrl: String
            get() = getApiUrl("tokens")

        /**
         * @return `https://api.stripe.com/v1/sources`
         */
        @JvmStatic
        val sourcesUrl: String
            @VisibleForTesting
            get() = getApiUrl("sources")

        /**
         * @return `https://api.stripe.com/v1/payment_methods`
         */
        @JvmStatic
        val paymentMethodsUrl: String
            @VisibleForTesting
            get() = getApiUrl("payment_methods")

        /**
         * @return `https://api.stripe.com/v1/payment_intents/:id`
         */
        @VisibleForTesting
        @JvmStatic
        fun getRetrievePaymentIntentUrl(paymentIntentId: String): String {
            return getApiUrl("payment_intents/%s", paymentIntentId)
        }

        /**
         * @return `https://api.stripe.com/v1/payment_intents/:id/confirm`
         */
        @VisibleForTesting
        @JvmStatic
        fun getConfirmPaymentIntentUrl(paymentIntentId: String): String {
            return getApiUrl("payment_intents/%s/confirm", paymentIntentId)
        }

        /**
         * @return `https://api.stripe.com/v1/payment_intents/:id`
         */
        @VisibleForTesting
        @JvmStatic
        fun getRetrieveSetupIntentUrl(setupIntentId: String): String {
            return getApiUrl("setup_intents/%s", setupIntentId)
        }

        /**
         * @return `https://api.stripe.com/v1/payment_intents/:id/confirm`
         */
        @VisibleForTesting
        @JvmStatic
        fun getConfirmSetupIntentUrl(setupIntentId: String): String {
            return getApiUrl("setup_intents/%s/confirm", setupIntentId)
        }

        /**
         * @return `https://api.stripe.com/v1/customers/:customer_id/sources`
         */
        @VisibleForTesting
        @JvmStatic
        fun getAddCustomerSourceUrl(customerId: String): String {
            return getApiUrl("customers/%s/sources", customerId)
        }

        /**
         * @return `https://api.stripe.com/v1/customers/:customer_id/sources/:source_id`
         */
        @VisibleForTesting
        @JvmStatic
        fun getDeleteCustomerSourceUrl(customerId: String, sourceId: String): String {
            return getApiUrl("customers/%s/sources/%s", customerId, sourceId)
        }

        /**
         * @return `https://api.stripe.com/v1/payment_methods/:id/attach`
         */
        @VisibleForTesting
        @JvmStatic
        fun getAttachPaymentMethodUrl(paymentMethodId: String): String {
            return getApiUrl("payment_methods/%s/attach", paymentMethodId)
        }

        /**
         * @return `https://api.stripe.com/v1/customers/:id`
         */
        @VisibleForTesting
        @JvmStatic
        fun getRetrieveCustomerUrl(customerId: String): String {
            return getApiUrl("customers/%s", customerId)
        }

        /**
         * @return `https://api.stripe.com/v1/sources/:id`
         */
        @VisibleForTesting
        @JvmStatic
        fun getRetrieveSourceApiUrl(sourceId: String): String {
            return getApiUrl("sources/%s", sourceId)
        }

        /**
         * @return `https://api.stripe.com/v1/tokens/:id`
         */
        @VisibleForTesting
        @JvmStatic
        fun getRetrieveTokenApiUrl(tokenId: String): String {
            return getApiUrl("tokens/%s", tokenId)
        }

        /**
         * @return `https://api.stripe.com/v1/issuing/cards/:id/pin`
         */
        @VisibleForTesting
        @JvmStatic
        fun getIssuingCardPinUrl(cardId: String): String {
            return getApiUrl("issuing/cards/%s/pin", cardId)
        }

        private fun getApiUrl(path: String, vararg args: Any): String {
            return getApiUrl(String.format(Locale.ENGLISH, path, *args))
        }

        private fun getApiUrl(path: String): String {
            return String.format(Locale.ENGLISH, "%s/v1/%s", ApiRequest.API_HOST, path)
        }
    }
}
