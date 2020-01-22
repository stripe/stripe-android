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
import com.stripe.android.model.StripeFile
import com.stripe.android.model.StripeFileParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeModel
import com.stripe.android.model.Token
import com.stripe.android.model.parsers.CustomerJsonParser
import com.stripe.android.model.parsers.ModelJsonParser
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.model.parsers.SetupIntentJsonParser
import com.stripe.android.model.parsers.SourceJsonParser
import com.stripe.android.model.parsers.Stripe3ds2AuthResultJsonParser
import com.stripe.android.model.parsers.StripeFileJsonParser
import com.stripe.android.model.parsers.TokenJsonParser
import java.io.IOException
import java.net.HttpURLConnection
import java.security.Security
import java.util.Locale
import org.json.JSONArray
import org.json.JSONException

/**
 * An implementation of [StripeRepository] that makes network requests to the Stripe API.
 */
internal class StripeApiRepository @JvmOverloads internal constructor(
    context: Context,
    private val appInfo: AppInfo? = null,
    private val logger: Logger = Logger.noop(),
    private val stripeApiRequestExecutor: ApiRequestExecutor = ApiRequestExecutor.Default(logger),
    private val fireAndForgetRequestExecutor: FireAndForgetRequestExecutor =
        StripeFireAndForgetRequestExecutor(logger),
    private val fingerprintRequestFactory: FingerprintRequestFactory =
        FingerprintRequestFactory(context),
    private val uidParamsFactory: UidParamsFactory = UidParamsFactory.create(context),
    private val analyticsDataFactory: AnalyticsDataFactory = AnalyticsDataFactory(context),
    private val networkUtils: StripeNetworkUtils = StripeNetworkUtils(context),
    apiVersion: String = ApiVersion.get().code,
    sdkVersion: String = Stripe.VERSION
) : StripeRepository {
    private val apiRequestFactory = ApiRequest.Factory(
        appInfo = appInfo,
        apiVersion = apiVersion,
        sdkVersion = sdkVersion
    )

    /**
     * Confirm a [PaymentIntent] using the provided [ConfirmPaymentIntentParams]
     *
     * Analytics event: [AnalyticsEvent.PaymentIntentConfirm]
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
        val apiUrl = getConfirmPaymentIntentUrl(
            PaymentIntent.ClientSecret(confirmPaymentIntentParams.clientSecret).paymentIntentId
        )

        try {
            fireFingerprintRequest()

            val paymentMethodType =
                confirmPaymentIntentParams.paymentMethodCreateParams?.typeCode
                    ?: confirmPaymentIntentParams.sourceParams?.type

            fireAnalyticsRequest(
                analyticsDataFactory.createPaymentIntentConfirmationParams(
                    options.apiKey,
                    paymentMethodType
                ),
                options.apiKey
            )

            return fetchStripeModel(
                apiRequestFactory.createPost(apiUrl, options, params),
                PaymentIntentJsonParser()
            )
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException.create(unexpected)
        }
    }

    /**
     * Retrieve a [PaymentIntent] using its client_secret
     *
     * Analytics event: [AnalyticsEvent.PaymentIntentRetrieve]
     *
     * @param clientSecret client_secret of the PaymentIntent to retrieve
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    override fun retrievePaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options
    ): PaymentIntent? {
        val paymentIntentId = PaymentIntent.ClientSecret(clientSecret).paymentIntentId

        fireFingerprintRequest()
        fireAnalyticsRequest(
            analyticsDataFactory.createPaymentIntentRetrieveParams(
                options.apiKey,
                paymentIntentId
            ),
            options.apiKey
        )

        try {
            return fetchStripeModel(
                apiRequestFactory.createGet(
                    getRetrievePaymentIntentUrl(paymentIntentId),
                    options,
                    createClientSecretParam(clientSecret)
                ),
                PaymentIntentJsonParser()
            )
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException.create(unexpected)
        }
    }

    /**
     * Analytics event: [AnalyticsEvent.PaymentIntentCancelSource]
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    override fun cancelPaymentIntentSource(
        paymentIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): PaymentIntent? {
        fireFingerprintRequest()
        fireAnalyticsRequest(AnalyticsEvent.PaymentIntentCancelSource, options.apiKey)

        try {
            return fetchStripeModel(
                apiRequestFactory.createPost(
                    getCancelPaymentIntentSourceUrl(paymentIntentId),
                    options,
                    mapOf("source" to sourceId)
                ),
                PaymentIntentJsonParser()
            )
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException.create(unexpected)
        }
    }

    /**
     * Confirm a [SetupIntent] using the provided [ConfirmSetupIntentParams]
     *
     * Analytics event: [AnalyticsEvent.SetupIntentConfirm]
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
        val setupIntentId =
            SetupIntent.ClientSecret(confirmSetupIntentParams.clientSecret).setupIntentId

        fireFingerprintRequest()
        fireAnalyticsRequest(
            analyticsDataFactory.createSetupIntentConfirmationParams(
                options.apiKey,
                confirmSetupIntentParams.paymentMethodCreateParams?.typeCode,
                setupIntentId
            ),
            options.apiKey
        )

        try {
            return fetchStripeModel(
                apiRequestFactory.createPost(
                    getConfirmSetupIntentUrl(setupIntentId),
                    options,
                    networkUtils.paramsWithUid(confirmSetupIntentParams.toParamMap())
                ),
                SetupIntentJsonParser()
            )
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException.create(unexpected)
        }
    }

    /**
     * Retrieve a [SetupIntent] using its client_secret
     *
     * Analytics event: [AnalyticsEvent.SetupIntentRetrieve]
     *
     * @param clientSecret client_secret of the SetupIntent to retrieve
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    override fun retrieveSetupIntent(
        clientSecret: String,
        options: ApiRequest.Options
    ): SetupIntent? {
        val setupIntentId = SetupIntent.ClientSecret(clientSecret).setupIntentId

        try {
            fireFingerprintRequest()
            fireAnalyticsRequest(
                analyticsDataFactory.createSetupIntentRetrieveParams(
                    options.apiKey,
                    setupIntentId
                ),
                options.apiKey
            )

            return fetchStripeModel(
                apiRequestFactory.createGet(
                    getRetrieveSetupIntentUrl(setupIntentId),
                    options,
                    createClientSecretParam(clientSecret)
                ),
                SetupIntentJsonParser()
            )
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException.create(unexpected)
        }
    }

    /**
     * Analytics event: [AnalyticsEvent.SetupIntentCancelSource]
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    override fun cancelSetupIntentSource(
        setupIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): SetupIntent? {
        fireFingerprintRequest()
        fireAnalyticsRequest(AnalyticsEvent.SetupIntentCancelSource, options.apiKey)

        try {
            return fetchStripeModel(
                apiRequestFactory.createPost(
                    getCancelSetupIntentSourceUrl(setupIntentId),
                    options,
                    mapOf("source" to sourceId)
                ),
                SetupIntentJsonParser()
            )
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
     * Analytics event: [AnalyticsEvent.SourceCreate]
     *
     * @param sourceParams a [SourceParams] object with [Source] creation params
     * @return a [Source] if one could be created from the input params,
     * or `null` if not
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    override fun createSource(
        sourceParams: SourceParams,
        options: ApiRequest.Options
    ): Source? {
        fireFingerprintRequest()
        fireAnalyticsRequest(
            analyticsDataFactory.createSourceCreationParams(options.apiKey, sourceParams.type),
            options.apiKey
        )

        try {
            return fetchStripeModel(
                apiRequestFactory.createPost(
                    sourcesUrl,
                    options,
                    sourceParams.toParamMap()
                        .plus(uidParamsFactory.createParams())
                ),
                SourceJsonParser()
            )
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
            return fetchStripeModel(
                apiRequestFactory.createGet(
                    apiUrl,
                    options,
                    SourceParams.createRetrieveSourceParams(clientSecret)
                ),
                SourceJsonParser()
            )
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException.create(unexpected)
        }
    }

    override fun retrieveSource(
        sourceId: String,
        clientSecret: String,
        options: ApiRequest.Options,
        callback: ApiResultCallback<Source>
    ) {
        RetrieveSourceTask(this, sourceId, clientSecret, options, callback).execute()
    }

    /**
     * Analytics event: [AnalyticsEvent.PaymentMethodCreate]
     */
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class)
    override fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: ApiRequest.Options
    ): PaymentMethod? {
        fireFingerprintRequest()

        try {
            val paymentMethod = fetchStripeModel(
                apiRequestFactory.createPost(
                    paymentMethodsUrl,
                    options,
                    paymentMethodCreateParams.toParamMap()
                        .plus(uidParamsFactory.createParams())
                ),
                PaymentMethodJsonParser()
            )

            fireAnalyticsRequest(
                analyticsDataFactory.createPaymentMethodCreationParams(
                    options.apiKey,
                    paymentMethod?.id
                ),
                options.apiKey
            )

            return paymentMethod
        } catch (unexpected: CardException) {
            // This particular kind of exception should not be possible from a Source API endpoint.
            throw APIException.create(unexpected)
        }
    }

    /**
     * Create a [Token] using the input token parameters.
     *
     * Analytics event: [AnalyticsEvent.TokenCreate]
     *
     * @param tokenParams a mapped set of parameters representing the object for which this token
     * is being created
     * @param options a [ApiRequest.Options] object that contains connection data like the api
     * key, api version, etc
     * @param tokenType the [Token.TokenType] being created
     * @return a [Token] that can be used to perform other operations with this card
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
                analyticsDataFactory.createTokenCreationParams(
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

    /**
     * Analytics event: [AnalyticsEvent.CustomerAddSource]
     */
    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun addCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options
    ): Source? {
        fireAnalyticsRequest(
            analyticsDataFactory.createAddSourceParams(
                productUsageTokens, publishableKey, sourceType
            ),
            publishableKey
        )

        return fetchStripeModel(
            apiRequestFactory.createPost(
                getAddCustomerSourceUrl(customerId),
                requestOptions,
                mapOf("source" to sourceId)
            ),
            SourceJsonParser()
        )
    }

    /**
     * Analytics event: [AnalyticsEvent.CustomerDeleteSource]
     */
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
            analyticsDataFactory.createDeleteSourceParams(productUsageTokens, publishableKey),
            publishableKey
        )

        return fetchStripeModel(
            apiRequestFactory.createDelete(
                getDeleteCustomerSourceUrl(customerId, sourceId),
                requestOptions
            ),
            SourceJsonParser()
        )
    }

    /**
     * Analytics event: [AnalyticsEvent.CustomerAttachPaymentMethod]
     */
    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun attachPaymentMethod(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): PaymentMethod? {
        fireFingerprintRequest()
        fireAnalyticsRequest(
            analyticsDataFactory
                .createAttachPaymentMethodParams(productUsageTokens, publishableKey),
            publishableKey
        )

        return fetchStripeModel(
            apiRequestFactory.createPost(
                getAttachPaymentMethodUrl(paymentMethodId),
                requestOptions,
                mapOf("customer" to customerId)
            ),
            PaymentMethodJsonParser()
        )
    }

    /**
     * Analytics event: [AnalyticsEvent.CustomerDetachPaymentMethod]
     */
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
                .createDetachPaymentMethodParams(productUsageTokens, publishableKey),
            publishableKey
        )

        return fetchStripeModel(
            apiRequestFactory.createPost(
                getDetachPaymentMethodUrl(paymentMethodId),
                requestOptions
            ),
            PaymentMethodJsonParser()
        )
    }

    /**
     * Retrieve a Customer's [PaymentMethod]s
     *
     * Analytics event: [AnalyticsEvent.CustomerRetrievePaymentMethods]
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
        val response = makeApiRequest(
            apiRequestFactory.createGet(
                paymentMethodsUrl,
                requestOptions,
                mapOf(
                    "customer" to customerId,
                    "type" to paymentMethodType
                )
            )
        )

        fireFingerprintRequest()
        fireAnalyticsRequest(
            analyticsDataFactory.createParams(
                AnalyticsEvent.CustomerRetrievePaymentMethods,
                publishableKey,
                productUsageTokens = productUsageTokens
            ),
            publishableKey
        )

        return try {
            val data = response.responseJson.optJSONArray("data") ?: JSONArray()
            (0 until data.length()).mapNotNull {
                PaymentMethodJsonParser().parse(data.optJSONObject(it))
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    /**
     * Analytics event: [AnalyticsEvent.CustomerSetDefaultSource]
     */
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
        fireFingerprintRequest()
        fireAnalyticsRequest(
            analyticsDataFactory.createParams(
                event = AnalyticsEvent.CustomerSetDefaultSource,
                publishableKey = publishableKey,
                productUsageTokens = productUsageTokens,
                sourceType = sourceType
            ),
            publishableKey
        )

        return fetchStripeModel(
            apiRequestFactory.createPost(
                getRetrieveCustomerUrl(customerId),
                requestOptions,
                mapOf("default_source" to sourceId)
            ),
            CustomerJsonParser()
        )
    }

    /**
     * Analytics event: [AnalyticsEvent.CustomerSetShippingInfo]
     */
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
            analyticsDataFactory.createParams(
                AnalyticsEvent.CustomerSetShippingInfo,
                publishableKey,
                productUsageTokens = productUsageTokens
            ),
            publishableKey
        )

        return fetchStripeModel(
            apiRequestFactory.createPost(
                getRetrieveCustomerUrl(customerId),
                requestOptions,
                mapOf("shipping" to shippingInformation.toParamMap())
            ),
            CustomerJsonParser()
        )
    }

    /**
     * Analytics event: [AnalyticsEvent.CustomerRetrieve]
     */
    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun retrieveCustomer(
        customerId: String,
        requestOptions: ApiRequest.Options
    ): Customer? {
        fireAnalyticsRequest(
            AnalyticsEvent.CustomerRetrieve,
            requestOptions.apiKey
        )

        return fetchStripeModel(
            apiRequestFactory.createGet(
                getRetrieveCustomerUrl(customerId),
                requestOptions
            ),
            CustomerJsonParser()
        )
    }

    /**
     * Analytics event: [AnalyticsEvent.IssuingRetrievePin]
     */
    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class, JSONException::class)
    override fun retrieveIssuingCardPin(
        cardId: String,
        verificationId: String,
        userOneTimeCode: String,
        ephemeralKeySecret: String
    ): String {
        fireAnalyticsRequest(
            AnalyticsEvent.IssuingRetrievePin,
            ephemeralKeySecret
        )

        val response = makeApiRequest(
            apiRequestFactory.createGet(
                getIssuingCardPinUrl(cardId),
                ApiRequest.Options(ephemeralKeySecret),
                mapOf("verification" to createVerificationParam(verificationId, userOneTimeCode))
            )
        )
        return response.responseJson.getString("pin")
    }

    /**
     * Analytics event: [AnalyticsEvent.IssuingUpdatePin]
     */
    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        AuthenticationException::class, CardException::class)
    override fun updateIssuingCardPin(
        cardId: String,
        newPin: String,
        verificationId: String,
        userOneTimeCode: String,
        ephemeralKeySecret: String
    ) {
        fireAnalyticsRequest(
            AnalyticsEvent.IssuingUpdatePin,
            ephemeralKeySecret
        )

        makeApiRequest(
            apiRequestFactory.createPost(
                getIssuingCardPinUrl(cardId),
                ApiRequest.Options(ephemeralKeySecret),
                mapOf(
                    "verification" to createVerificationParam(verificationId, userOneTimeCode),
                    "pin" to newPin
                )
            )
        )
    }

    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, APIException::class, CardException::class)
    override fun getFpxBankStatus(options: ApiRequest.Options): FpxBankStatuses {
        val response = makeApiRequest(
            apiRequestFactory.createGet(
                getApiUrl("fpx/bank_statuses"),
                options,
                mapOf("account_holder_type" to "individual")
            )
        )
        return FpxBankStatuses.fromJson(response.responseJson)
    }

    /**
     * Analytics event: [AnalyticsEvent.Auth3ds2Start]
     */
    @VisibleForTesting
    @Throws(InvalidRequestException::class, APIConnectionException::class, APIException::class,
        CardException::class, AuthenticationException::class, JSONException::class)
    internal fun start3ds2Auth(
        authParams: Stripe3ds2AuthParams,
        stripeIntentId: String,
        requestOptions: ApiRequest.Options
    ): Stripe3ds2AuthResult {
        fireAnalyticsRequest(
            analyticsDataFactory.createAuthParams(
                AnalyticsEvent.Auth3ds2Start,
                stripeIntentId,
                requestOptions.apiKey
            ),
            requestOptions.apiKey
        )

        val response = makeApiRequest(
            apiRequestFactory.createPost(
                getApiUrl("3ds2/authenticate"),
                requestOptions,
                authParams.toParamMap()
            )
        )

        return Stripe3ds2AuthResultJsonParser().parse(response.responseJson)
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
    internal fun complete3ds2Auth(sourceId: String, requestOptions: ApiRequest.Options): Boolean {
        val response = makeApiRequest(
            apiRequestFactory.createPost(
                getApiUrl("3ds2/challenge_complete"),
                requestOptions,
                mapOf("source" to sourceId)
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
     * Analytics event: [AnalyticsEvent.FileCreate]
     */
    override fun createFile(
        fileParams: StripeFileParams,
        requestOptions: ApiRequest.Options
    ): StripeFile {
        val response = makeFileUploadRequest(
            FileUploadRequest(fileParams, requestOptions, appInfo)
        )
        fireFingerprintRequest()
        fireAnalyticsRequest(AnalyticsEvent.FileCreate, requestOptions.apiKey)
        return StripeFileJsonParser().parse(response.responseJson)
    }

    /**
     * @return `https://api.stripe.com/v1/payment_methods/:id/detach`
     */
    @VisibleForTesting
    internal fun getDetachPaymentMethodUrl(paymentMethodId: String): String {
        return getApiUrl("payment_methods/%s/detach", paymentMethodId)
    }

    @Throws(InvalidRequestException::class, AuthenticationException::class, CardException::class,
        APIException::class)
    private fun handleApiError(response: StripeResponse) {
        val requestId = response.requestId
        val responseCode = response.responseCode
        val stripeError = StripeErrorJsonParser().parse(response.responseJson)
        when (responseCode) {
            HttpURLConnection.HTTP_BAD_REQUEST, HttpURLConnection.HTTP_NOT_FOUND -> {
                throw InvalidRequestException(
                    stripeError,
                    requestId,
                    responseCode
                )
            }
            HttpURLConnection.HTTP_UNAUTHORIZED -> {
                throw AuthenticationException(stripeError, requestId)
            }
            HttpURLConnection.HTTP_PAYMENT_REQUIRED -> {
                throw CardException(stripeError, requestId)
            }
            HttpURLConnection.HTTP_FORBIDDEN -> {
                throw PermissionException(stripeError, requestId)
            }
            429 -> {
                throw RateLimitException(stripeError, requestId)
            }
            else -> {
                throw APIException(stripeError, requestId, responseCode)
            }
        }
    }

    private fun <ModelType : StripeModel> fetchStripeModel(
        apiRequest: ApiRequest,
        jsonParser: ModelJsonParser<ModelType>
    ): ModelType? {
        return jsonParser.parse(makeApiRequest(apiRequest).responseJson)
    }

    @VisibleForTesting
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, CardException::class, APIException::class)
    internal fun makeApiRequest(apiRequest: ApiRequest): StripeResponse {
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

    @VisibleForTesting
    @Throws(AuthenticationException::class, InvalidRequestException::class,
        APIConnectionException::class, CardException::class, APIException::class)
    internal fun makeFileUploadRequest(fileUploadRequest: FileUploadRequest): StripeResponse {
        val dnsCacheData = disableDnsCache()

        val response = try {
            stripeApiRequestExecutor.execute(fileUploadRequest)
        } catch (ex: IOException) {
            throw APIConnectionException.create(ex, fileUploadRequest.baseUrl)
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
        return fetchStripeModel(
            apiRequestFactory.createPost(url, options, params),
            TokenJsonParser()
        )
    }

    private fun fireFingerprintRequest() {
        makeFireAndForgetRequest(fingerprintRequestFactory.create())
    }

    private fun fireAnalyticsRequest(
        event: AnalyticsEvent,
        publishableKey: String
    ) {
        fireAnalyticsRequest(
            analyticsDataFactory.createParams(
                event,
                publishableKey
            ),
            publishableKey
        )
    }

    @VisibleForTesting
    internal fun fireAnalyticsRequest(
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

    private class RetrieveSourceTask constructor(
        private val stripeRepository: StripeRepository,
        private val sourceId: String,
        private val clientSecret: String,
        private val requestOptions: ApiRequest.Options,
        callback: ApiResultCallback<Source>
    ) : ApiOperation<Source>(callback = callback) {

        @Throws(StripeException::class)
        override suspend fun getResult(): Source? {
            return stripeRepository.retrieveSource(sourceId, clientSecret, requestOptions)
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
