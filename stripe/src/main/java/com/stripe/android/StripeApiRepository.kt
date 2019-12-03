package com.stripe.android

import android.content.Context
import android.util.Pair
import androidx.annotation.VisibleForTesting
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
import com.stripe.android.model.FpxBankStatuses
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.android.model.Stripe3ds2AuthResult
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.Token
import java.io.IOException
import java.net.HttpURLConnection
import java.security.Security
import java.util.Locale
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * An implementation of [StripeRepository] that makes network requests to the Stripe API.
 */
internal class StripeApiRepository @JvmOverloads internal constructor(
    context: Context,
    private val appInfo: AppInfo? = null,
    private val logger: Logger = Logger.noop(),
    private val stripeApiRequestExecutor: ApiRequestExecutor = StripeApiRequestExecutor(logger),
    private val fireAndForgetRequestExecutor: FireAndForgetRequestExecutor =
        StripeFireAndForgetRequestExecutor(logger),
    private val fingerprintRequestFactory: FingerprintRequestFactory =
        FingerprintRequestFactory(context),
    private val uidParamsFactory: UidParamsFactory = UidParamsFactory.create(context),
    private val analyticsDataFactory: AnalyticsDataFactory = AnalyticsDataFactory.create(context),
    private val networkUtils: StripeNetworkUtils = StripeNetworkUtils(context)
) : StripeRepository {
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
        val params = networkUtils.paramsWithUid(confirmPaymentIntentParams.toParamMap())
        val paymentIntentId =
            PaymentIntent.parseIdFromClientSecret(confirmPaymentIntentParams.clientSecret)
        val apiUrl = getConfirmPaymentIntentUrl(paymentIntentId)

        try {
            fireFingerprintRequest()

            val paymentMethodType =
                confirmPaymentIntentParams.paymentMethodCreateParams?.typeCode
                    ?: confirmPaymentIntentParams.sourceParams?.type

            fireAnalyticsRequest(
                analyticsDataFactory.getPaymentIntentConfirmationParams(
                    null,
                    options.apiKey,
                    paymentMethodType
                ),
                options.apiKey
            )
            val response = makeApiRequest(
                ApiRequest.createPost(
                    apiUrl, options, params, appInfo
                )
            )
            return PaymentIntent.fromJson(response.responseJson)
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException.create(unexpected)
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
        val paymentIntentId = PaymentIntent.parseIdFromClientSecret(clientSecret)
        val apiUrl = getRetrievePaymentIntentUrl(paymentIntentId)

        try {
            fireFingerprintRequest()
            fireAnalyticsRequest(
                analyticsDataFactory.getPaymentIntentRetrieveParams(null, options.apiKey),
                options.apiKey)
            val response = makeApiRequest(
                ApiRequest.createGet(
                    apiUrl,
                    options,
                    createClientSecretParam(clientSecret),
                    appInfo
                )
            )
            return PaymentIntent.fromJson(response.responseJson)
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException.create(unexpected)
        }
    }

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    override fun cancelPaymentIntentSource(
        paymentIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): PaymentIntent? {
        val apiUrl = getCancelPaymentIntentSourceUrl(paymentIntentId)
        try {
            fireFingerprintRequest()
            val response = makeApiRequest(
                ApiRequest.createPost(
                    apiUrl,
                    options,
                    mapOf("source" to sourceId),
                    appInfo
                )
            )
            return PaymentIntent.fromJson(response.responseJson)
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException.create(unexpected)
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
        val params = networkUtils.paramsWithUid(confirmSetupIntentParams.toParamMap())

        val setupIntentId =
            SetupIntent.parseIdFromClientSecret(confirmSetupIntentParams.clientSecret)
        val apiUrl = getConfirmSetupIntentUrl(setupIntentId)

        try {
            fireFingerprintRequest()
            val response = makeApiRequest(
                ApiRequest.createPost(
                    apiUrl,
                    options,
                    params,
                    appInfo
                )
            )
            val setupIntent = SetupIntent.fromJson(response.responseJson)

            fireAnalyticsRequest(
                analyticsDataFactory.getSetupIntentConfirmationParams(
                    options.apiKey,
                    confirmSetupIntentParams.paymentMethodCreateParams?.typeCode
                ),
                options.apiKey
            )
            return setupIntent
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException.create(unexpected)
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
        val setupIntentId = SetupIntent.parseIdFromClientSecret(clientSecret)
        val apiUrl = getRetrieveSetupIntentUrl(setupIntentId)

        try {
            fireFingerprintRequest()
            fireAnalyticsRequest(
                analyticsDataFactory.getSetupIntentRetrieveParams(options.apiKey),
                options.apiKey
            )

            val response = makeApiRequest(
                ApiRequest.createGet(
                    apiUrl,
                    options,
                    createClientSecretParam(clientSecret),
                    appInfo
                )
            )
            return SetupIntent.fromJson(response.responseJson)
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException.create(unexpected)
        }
    }

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    override fun cancelSetupIntentSource(
        setupIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): SetupIntent? {
        val apiUrl = getCancelSetupIntentSourceUrl(setupIntentId)

        try {
            val response = makeApiRequest(
                ApiRequest.createPost(
                    apiUrl,
                    options,
                    mapOf("source" to sourceId),
                    appInfo
                )
            )
            return SetupIntent.fromJson(response.responseJson)
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException.create(unexpected)
        }
    }

    override fun retrieveIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        callback: ApiResultCallback<StripeIntent>
    ) {
        RetrieveIntentTask(this, clientSecret, options, callback)
            .execute()
    }

    override fun cancelIntent(
        stripeIntent: StripeIntent,
        sourceId: String,
        options: ApiRequest.Options,
        callback: ApiResultCallback<StripeIntent>
    ) {
        CancelIntentTask(this, stripeIntent, sourceId, options, callback)
            .execute()
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
        val apiUrl = sourcesUrl

        try {
            fireFingerprintRequest()
            fireAnalyticsRequest(
                analyticsDataFactory.getSourceCreationParams(options.apiKey, sourceParams.type),
                options.apiKey
            )
            val response = makeApiRequest(
                ApiRequest.createPost(
                    apiUrl,
                    options,
                    sourceParams.toParamMap()
                        .plus(uidParamsFactory.createParams()),
                    appInfo
                )
            )
            return Source.fromJson(response.responseJson)
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException.create(unexpected)
        }
    }

    /**
     * Retrieve an existing [Source] object from the server.
     *
     * @param sourceId the [Source.id] field for the Source to query
     * @param clientSecret the [Source.clientSecret] field for the Source to query
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
        val apiUrl = getRetrieveSourceApiUrl(sourceId)

        try {
            val response = makeApiRequest(
                ApiRequest.createGet(
                    apiUrl,
                    options,
                    SourceParams.createRetrieveSourceParams(clientSecret),
                    appInfo
                )
            )
            return Source.fromJson(response.responseJson)
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException.create(unexpected)
        }
    }

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    override fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: ApiRequest.Options
    ): PaymentMethod? {
        val apiUrl = paymentMethodsUrl

        fireFingerprintRequest()

        try {
            val response = makeApiRequest(
                ApiRequest.createPost(
                    apiUrl,
                    options,
                    paymentMethodCreateParams.toParamMap()
                        .plus(uidParamsFactory.createParams()),
                    appInfo
                )
            )
            val paymentMethod = PaymentMethod.fromJson(response.responseJson)

            fireAnalyticsRequest(
                analyticsDataFactory.createPaymentMethodCreationParams(
                    options.apiKey,
                    paymentMethod?.id),
                options.apiKey)

            return paymentMethod
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException.create(unexpected)
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
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options
    ): Source? {
        fireAnalyticsRequest(
            analyticsDataFactory
                .getAddSourceParams(productUsageTokens.toList(), publishableKey, sourceType),
            // We use the public key to log, so we need different Options.
            publishableKey
        )

        val response = makeApiRequest(
            ApiRequest.createPost(
                getAddCustomerSourceUrl(customerId),
                requestOptions,
                mapOf("source" to sourceId),
                appInfo
            )
        )
        return Source.fromJson(response.responseJson)
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun deleteCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Source? {
        fireAnalyticsRequest(
            analyticsDataFactory.getDeleteSourceParams(productUsageTokens.toList(), publishableKey),
            // We use the public key to log, so we need different Options.
            publishableKey
        )

        val response = makeApiRequest(
            ApiRequest.createDelete(
                getDeleteCustomerSourceUrl(customerId, sourceId),
                requestOptions, appInfo)
        )

        return Source.fromJson(response.responseJson)
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun attachPaymentMethod(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): PaymentMethod? {
        fireAnalyticsRequest(
            analyticsDataFactory
                .getAttachPaymentMethodParams(productUsageTokens.toList(), publishableKey),
            // We use the public key to log, so we need different Options.
            publishableKey
        )

        val response = makeApiRequest(
            ApiRequest.createPost(
                getAttachPaymentMethodUrl(paymentMethodId),
                requestOptions,
                mapOf("customer" to customerId), appInfo
            )
        )
        return PaymentMethod.fromJson(response.responseJson)
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun detachPaymentMethod(
        publishableKey: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): PaymentMethod? {
        fireAnalyticsRequest(
            analyticsDataFactory
                .getDetachPaymentMethodParams(productUsageTokens.toList(), publishableKey),
            // We use the public key to log, so we need different Options.
            publishableKey
        )

        val response = makeApiRequest(
            ApiRequest.createPost(
                getDetachPaymentMethodUrl(paymentMethodId),
                requestOptions,
                appInfo = appInfo
            )
        )
        return PaymentMethod.fromJson(response.responseJson)
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
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options
    ): List<PaymentMethod> {
        val queryParams = mapOf(
            "customer" to customerId,
            "type" to paymentMethodType
        )

        fireAnalyticsRequest(
            analyticsDataFactory
                .getDetachPaymentMethodParams(productUsageTokens.toList(), publishableKey),
            // We use the public key to log, so we need different Options.
            publishableKey
        )

        val response = makeApiRequest(
            ApiRequest.createGet(
                paymentMethodsUrl,
                requestOptions,
                queryParams, appInfo)
        )

        return try {
            val data = response.responseJson.optJSONArray("data") ?: JSONArray()
            (0 until data.length()).mapNotNull {
                PaymentMethod.fromJson(data.optJSONObject(it))
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun setDefaultCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options
    ): Customer? {
        fireAnalyticsRequest(
            analyticsDataFactory.getEventLoggingParams(
                eventName = AnalyticsDataFactory.EventName.DEFAULT_SOURCE,
                publishableKey = publishableKey,
                productUsageTokens = productUsageTokens.toList(),
                sourceType = sourceType
            ),
            publishableKey
        )

        val response = makeApiRequest(
            ApiRequest.createPost(
                getRetrieveCustomerUrl(customerId),
                requestOptions,
                mapOf("default_source" to sourceId),
                appInfo
            )
        )

        return Customer.fromJson(response.responseJson)
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun setCustomerShippingInfo(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        shippingInformation: ShippingInformation,
        requestOptions: ApiRequest.Options
    ): Customer? {
        fireAnalyticsRequest(
            analyticsDataFactory.getEventLoggingParams(
                AnalyticsDataFactory.EventName.SET_SHIPPING_INFO,
                publishableKey,
                productUsageTokens = productUsageTokens.toList()
            ),
            publishableKey
        )

        val response = makeApiRequest(
            ApiRequest.createPost(
                getRetrieveCustomerUrl(customerId),
                requestOptions,
                mapOf("shipping" to shippingInformation.toParamMap()), appInfo)
        )
        return Customer.fromJson(response.responseJson)
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun retrieveCustomer(
        customerId: String,
        requestOptions: ApiRequest.Options
    ): Customer? {
        val response = makeApiRequest(
            ApiRequest.createGet(
                getRetrieveCustomerUrl(customerId),
                requestOptions,
                appInfo = appInfo
            )
        )
        return Customer.fromJson(response.responseJson)
    }

    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class, JSONException::class)
    override fun retrieveIssuingCardPin(
        cardId: String,
        verificationId: String,
        userOneTimeCode: String,
        ephemeralKeySecret: String
    ): String {
        val response = makeApiRequest(
            ApiRequest.createGet(
                getIssuingCardPinUrl(cardId),
                ApiRequest.Options(ephemeralKeySecret),
                mapOf("verification" to createVerificationParam(verificationId, userOneTimeCode)),
                appInfo
            )
        )
        return response.responseJson.getString("pin")
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
        makeApiRequest(
            ApiRequest.createPost(
                getIssuingCardPinUrl(cardId),
                ApiRequest.Options(ephemeralKeySecret),
                mapOf(
                    "verification" to createVerificationParam(verificationId, userOneTimeCode),
                    "pin" to newPin
                ),
                appInfo
            )
        )
    }

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class, CardException::class)
    override fun getFpxBankStatus(options: ApiRequest.Options): FpxBankStatuses {
        val response = makeApiRequest(
            ApiRequest.createGet(
                getApiUrl("fpx/bank_statuses"),
                options,
                mapOf("account_holder_type" to "individual"),
                appInfo
            )
        )
        return FpxBankStatuses.fromJson(response.responseJson)
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

        val response = makeApiRequest(
            ApiRequest.createPost(
                getApiUrl("3ds2/authenticate"),
                requestOptions,
                authParams.toParamMap(),
                appInfo
            )
        )
        return Stripe3ds2AuthResult.fromJson(response.responseJson)
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
        val response = makeApiRequest(
            ApiRequest.createPost(
                getApiUrl("3ds2/challenge_complete"),
                requestOptions,
                mapOf("source" to sourceId),
                appInfo
            )
        )
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

    @Throws(InvalidRequestException::class, AuthenticationException::class, CardException::class,
        APIException::class)
    private fun handleApiError(response: StripeResponse) {
        val requestId = response.requestId
        val responseCode = response.responseCode
        val stripeError = ErrorParser.parseError(response.responseJson)
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
    fun makeApiRequest(apiRequest: ApiRequest): StripeResponse {
        val dnsCacheData = disableDnsCache()

        val response = try {
            stripeApiRequestExecutor.execute(apiRequest)
        } catch (ex: IOException) {
            throw APIConnectionException.create(ex, apiRequest.baseUrl)
        }

        if (response.hasErrorCode()) {
            handleApiError(response)
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
     * @param dnsCacheData first object - flag to reset [DNS_CACHE_TTL_PROPERTY_NAME]
     * second object - the original DNS cache TTL value
     */
    private fun resetDnsCacheTtl(dnsCacheData: Pair<Boolean, String>) {
        if (dnsCacheData.first) {
            // value unspecified by implementation
            // DNS_CACHE_TTL_PROPERTY_NAME of -1 = cache forever
            Security.setProperty(DNS_CACHE_TTL_PROPERTY_NAME,
                dnsCacheData.second ?: "-1")
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
            ApiRequest.createPost(url, options, params, appInfo)
        )
        return Token.fromJson(response.responseJson)
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
                ApiRequest.Options(publishableKey),
                appInfo
            )
        )
    }

    private fun createClientSecretParam(clientSecret: String): Map<String, String> {
        return mapOf("client_secret" to clientSecret)
    }

    private class Start3ds2AuthTask constructor(
        private val stripeApiRepository: StripeApiRepository,
        private val params: Stripe3ds2AuthParams,
        private val stripeIntentId: String,
        private val requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<Stripe3ds2AuthResult>
    ) : ApiOperation<Stripe3ds2AuthResult>(callback = callback) {
        @Throws(StripeException::class, JSONException::class)
        override suspend fun getResult(): Stripe3ds2AuthResult {
            return stripeApiRepository.start3ds2Auth(params, stripeIntentId, requestOptions)
        }
    }

    private class Complete3ds2AuthTask constructor(
        private val stripeApiRepository: StripeApiRepository,
        private val sourceId: String,
        private val requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<Boolean>
    ) : ApiOperation<Boolean>(callback = callback) {
        @Throws(StripeException::class)
        override suspend fun getResult(): Boolean {
            return stripeApiRepository.complete3ds2Auth(sourceId, requestOptions)
        }
    }

    private class RetrieveIntentTask constructor(
        private val stripeRepository: StripeRepository,
        private val clientSecret: String,
        private val requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<StripeIntent>
    ) : ApiOperation<StripeIntent>(callback = callback) {

        @Throws(StripeException::class)
        override suspend fun getResult(): StripeIntent? {
            return when {
                clientSecret.startsWith("pi_") ->
                    stripeRepository.retrievePaymentIntent(clientSecret, requestOptions)
                clientSecret.startsWith("seti_") ->
                    stripeRepository.retrieveSetupIntent(clientSecret, requestOptions)
                else -> null
            }
        }
    }

    private class CancelIntentTask constructor(
        private val stripeRepository: StripeRepository,
        private val stripeIntent: StripeIntent,
        private val sourceId: String,
        private val requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<StripeIntent>
    ) : ApiOperation<StripeIntent>(callback = callback) {

        @Throws(StripeException::class)
        override suspend fun getResult(): StripeIntent? {
            return when (stripeIntent) {
                is PaymentIntent ->
                    stripeRepository.cancelPaymentIntentSource(
                        stripeIntent.id.orEmpty(), sourceId, requestOptions
                    )
                is SetupIntent ->
                    stripeRepository.cancelSetupIntentSource(
                        stripeIntent.id.orEmpty(), sourceId, requestOptions
                    )
                else -> null
            }
        }
    }

    internal companion object {
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
        internal val tokensUrl: String
            @JvmSynthetic
            get() = getApiUrl("tokens")

        /**
         * @return `https://api.stripe.com/v1/sources`
         */
        internal val sourcesUrl: String
            @JvmSynthetic
            get() = getApiUrl("sources")

        /**
         * @return `https://api.stripe.com/v1/payment_methods`
         */
        internal val paymentMethodsUrl: String
            @JvmSynthetic
            get() = getApiUrl("payment_methods")

        /**
         * @return `https://api.stripe.com/v1/payment_intents/:id`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getRetrievePaymentIntentUrl(paymentIntentId: String): String {
            return getApiUrl("payment_intents/%s", paymentIntentId)
        }

        /**
         * @return `https://api.stripe.com/v1/payment_intents/:id/confirm`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getConfirmPaymentIntentUrl(paymentIntentId: String): String {
            return getApiUrl("payment_intents/%s/confirm", paymentIntentId)
        }

        /**
         * @return `https://api.stripe.com/v1/payment_intents/:id/source_cancel`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getCancelPaymentIntentSourceUrl(paymentIntentId: String): String {
            return getApiUrl("payment_intents/%s/source_cancel", paymentIntentId)
        }

        /**
         * @return `https://api.stripe.com/v1/setup_intents/:id`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getRetrieveSetupIntentUrl(setupIntentId: String): String {
            return getApiUrl("setup_intents/%s", setupIntentId)
        }

        /**
         * @return `https://api.stripe.com/v1/setup_intents/:id/confirm`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getConfirmSetupIntentUrl(setupIntentId: String): String {
            return getApiUrl("setup_intents/%s/confirm", setupIntentId)
        }

        /**
         * @return `https://api.stripe.com/v1/setup_intents/:id/source_cancel`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getCancelSetupIntentSourceUrl(setupIntentId: String): String {
            return getApiUrl("setup_intents/%s/source_cancel", setupIntentId)
        }

        /**
         * @return `https://api.stripe.com/v1/customers/:customer_id/sources`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getAddCustomerSourceUrl(customerId: String): String {
            return getApiUrl("customers/%s/sources", customerId)
        }

        /**
         * @return `https://api.stripe.com/v1/customers/:customer_id/sources/:source_id`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getDeleteCustomerSourceUrl(customerId: String, sourceId: String): String {
            return getApiUrl("customers/%s/sources/%s", customerId, sourceId)
        }

        /**
         * @return `https://api.stripe.com/v1/payment_methods/:id/attach`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getAttachPaymentMethodUrl(paymentMethodId: String): String {
            return getApiUrl("payment_methods/%s/attach", paymentMethodId)
        }

        /**
         * @return `https://api.stripe.com/v1/customers/:id`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getRetrieveCustomerUrl(customerId: String): String {
            return getApiUrl("customers/%s", customerId)
        }

        /**
         * @return `https://api.stripe.com/v1/sources/:id`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getRetrieveSourceApiUrl(sourceId: String): String {
            return getApiUrl("sources/%s", sourceId)
        }

        /**
         * @return `https://api.stripe.com/v1/tokens/:id`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getRetrieveTokenApiUrl(tokenId: String): String {
            return getApiUrl("tokens/%s", tokenId)
        }

        /**
         * @return `https://api.stripe.com/v1/issuing/cards/:id/pin`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getIssuingCardPinUrl(cardId: String): String {
            return getApiUrl("issuing/cards/%s/pin", cardId)
        }

        private fun getApiUrl(path: String, vararg args: Any): String {
            return getApiUrl(String.format(Locale.ENGLISH, path, *args))
        }

        private fun getApiUrl(path: String): String {
            return "${ApiRequest.API_HOST}/v1/$path"
        }
    }
}
