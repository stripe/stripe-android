package com.stripe.android.networking

import android.content.Context
import android.util.Pair
import androidx.annotation.VisibleForTesting
import com.stripe.android.AnalyticsEvent
import com.stripe.android.ApiVersion
import com.stripe.android.AppInfo
import com.stripe.android.FingerprintData
import com.stripe.android.FingerprintDataRepository
import com.stripe.android.Logger
import com.stripe.android.Stripe
import com.stripe.android.cards.Bin
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.CardException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.exception.PermissionException
import com.stripe.android.exception.RateLimitException
import com.stripe.android.model.CardMetadata
import com.stripe.android.model.Complete3ds2Result
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.Customer
import com.stripe.android.model.FpxBankStatuses
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.android.model.Stripe3ds2AuthParams
import com.stripe.android.model.Stripe3ds2AuthResult
import com.stripe.android.model.StripeErrorJsonParser
import com.stripe.android.model.StripeFile
import com.stripe.android.model.StripeFileParams
import com.stripe.android.model.StripeModel
import com.stripe.android.model.Token
import com.stripe.android.model.TokenParams
import com.stripe.android.model.parsers.CardMetadataJsonParser
import com.stripe.android.model.parsers.CustomerJsonParser
import com.stripe.android.model.parsers.FpxBankStatusesJsonParser
import com.stripe.android.model.parsers.ModelJsonParser
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.model.parsers.SetupIntentJsonParser
import com.stripe.android.model.parsers.SourceJsonParser
import com.stripe.android.model.parsers.Stripe3ds2AuthResultJsonParser
import com.stripe.android.model.parsers.StripeFileJsonParser
import com.stripe.android.model.parsers.TokenJsonParser
import com.stripe.android.utils.StripeUrlUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.security.Security
import java.util.Locale

/**
 * An implementation of [StripeRepository] that makes network requests to the Stripe API.
 */
internal class StripeApiRepository @JvmOverloads internal constructor(
    context: Context,
    private val publishableKey: String,
    private val appInfo: AppInfo? = null,
    private val logger: Logger = Logger.noop(),
    private val stripeApiRequestExecutor: ApiRequestExecutor = ApiRequestExecutor.Default(logger),
    private val analyticsRequestExecutor: AnalyticsRequestExecutor =
        AnalyticsRequestExecutor.Default(logger),
    private val fingerprintDataRepository: FingerprintDataRepository =
        FingerprintDataRepository.Default(context),
    private val analyticsDataFactory: AnalyticsDataFactory =
        AnalyticsDataFactory(context, publishableKey),
    private val fingerprintParamsUtils: FingerprintParamsUtils = FingerprintParamsUtils(),
    apiVersion: String = ApiVersion.get().code,
    sdkVersion: String = Stripe.VERSION
) : StripeRepository {
    private val analyticsRequestFactory = AnalyticsRequest.Factory(logger)
    private val apiRequestFactory = ApiRequest.Factory(
        appInfo = appInfo,
        apiVersion = apiVersion,
        sdkVersion = sdkVersion
    )

    private val fingerprintData: FingerprintData?
        get() = fingerprintDataRepository.get()

    init {
        fireFingerprintRequest()
    }

    /**
     * Confirm a [PaymentIntent] using the provided [ConfirmPaymentIntentParams]
     *
     * Analytics event: [AnalyticsEvent.PaymentIntentConfirm]
     *
     * @param confirmPaymentIntentParams contains the confirmation params
     * @return a [PaymentIntent] reflecting the updated state after applying the parameter
     * provided
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    override suspend fun confirmPaymentIntent(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): PaymentIntent? {
        val params = fingerprintParamsUtils.addFingerprintData(
            confirmPaymentIntentParams.toParamMap()
                .plus(createExpandParam(expandFields)),
            fingerprintData
        )
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
                    paymentMethodType
                )
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
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    override suspend fun retrievePaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): PaymentIntent? {
        val paymentIntentId = PaymentIntent.ClientSecret(clientSecret).paymentIntentId

        fireFingerprintRequest()
        fireAnalyticsRequest(
            analyticsDataFactory.createPaymentIntentRetrieveParams(paymentIntentId)
        )

        try {
            return fetchStripeModel(
                apiRequestFactory.createGet(
                    getRetrievePaymentIntentUrl(paymentIntentId),
                    options,
                    createClientSecretParam(clientSecret, expandFields)
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
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    override suspend fun cancelPaymentIntentSource(
        paymentIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): PaymentIntent? {
        fireFingerprintRequest()
        fireAnalyticsRequest(AnalyticsEvent.PaymentIntentCancelSource)

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
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    override suspend fun confirmSetupIntent(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): SetupIntent? {
        val setupIntentId =
            SetupIntent.ClientSecret(confirmSetupIntentParams.clientSecret).setupIntentId

        fireFingerprintRequest()
        fireAnalyticsRequest(
            analyticsDataFactory.createSetupIntentConfirmationParams(
                confirmSetupIntentParams.paymentMethodCreateParams?.typeCode,
                setupIntentId
            )
        )

        try {
            return fetchStripeModel(
                apiRequestFactory.createPost(
                    getConfirmSetupIntentUrl(setupIntentId),
                    options,
                    fingerprintParamsUtils.addFingerprintData(
                        confirmSetupIntentParams.toParamMap()
                            .plus(createExpandParam(expandFields)),
                        fingerprintData
                    )
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
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    override suspend fun retrieveSetupIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): SetupIntent? {
        val setupIntentId = SetupIntent.ClientSecret(clientSecret).setupIntentId

        try {
            fireFingerprintRequest()
            fireAnalyticsRequest(
                analyticsDataFactory.createSetupIntentRetrieveParams(
                    setupIntentId
                )
            )

            return fetchStripeModel(
                apiRequestFactory.createGet(
                    getRetrieveSetupIntentUrl(setupIntentId),
                    options,
                    createClientSecretParam(clientSecret, expandFields)
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
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    override suspend fun cancelSetupIntentSource(
        setupIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): SetupIntent? {
        fireAnalyticsRequest(AnalyticsEvent.SetupIntentCancelSource)

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

    /**
     * Create a [Source] using the input [SourceParams].
     *
     * Analytics event: [AnalyticsEvent.SourceCreate]
     *
     * @param sourceParams a [SourceParams] object with [Source] creation params
     * @return a [Source] if one could be created from the input params,
     * or `null` if not
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    override suspend fun createSource(
        sourceParams: SourceParams,
        options: ApiRequest.Options
    ): Source? {
        fireFingerprintRequest()
        fireAnalyticsRequest(
            analyticsDataFactory.createSourceCreationParams(
                sourceParams.type,
                sourceParams.attribution
            )
        )

        try {
            return fetchStripeModel(
                apiRequestFactory.createPost(
                    sourcesUrl,
                    options,
                    sourceParams.toParamMap()
                        .plus(fingerprintData?.params.orEmpty())
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
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    override suspend fun retrieveSource(
        sourceId: String,
        clientSecret: String,
        options: ApiRequest.Options
    ): Source? {
        val apiUrl = getRetrieveSourceApiUrl(sourceId)

        fireAnalyticsRequest(
            analyticsDataFactory.createSourceRetrieveParams(sourceId)
        )

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

    /**
     * Analytics event: [AnalyticsEvent.PaymentMethodCreate]
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    override suspend fun createPaymentMethod(
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
                        .plus(fingerprintData?.params.orEmpty())
                ),
                PaymentMethodJsonParser()
            )

            fireAnalyticsRequest(
                analyticsDataFactory.createPaymentMethodCreationParams(
                    paymentMethod?.id,
                    paymentMethod?.type,
                    productUsageTokens = paymentMethodCreateParams.attribution
                )
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
     * @param tokenParams a [TokenParams] representing the object for which this token is being created
     * @param options a [ApiRequest.Options] object that contains connection data like the api
     * key, api version, etc
     *
     * @return a [Token] that can be used to perform other operations with this card
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        CardException::class,
        APIException::class
    )
    override suspend fun createToken(
        tokenParams: TokenParams,
        options: ApiRequest.Options
    ): Token? {
        fireFingerprintRequest()

        fireAnalyticsRequest(
            analyticsDataFactory.createTokenCreationParams(
                productUsageTokens = tokenParams.attribution,
                tokenType = tokenParams.tokenType
            )
        )

        return fetchStripeModel(
            apiRequestFactory.createPost(
                tokensUrl,
                options,
                tokenParams.toParamMap()
                    .plus(fingerprintData?.params.orEmpty())
            ),
            TokenJsonParser()
        )
    }

    /**
     * Analytics event: [AnalyticsEvent.CustomerAddSource]
     */
    @Throws(
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        AuthenticationException::class,
        CardException::class
    )
    override suspend fun addCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options
    ): Source? {
        fireAnalyticsRequest(
            analyticsDataFactory.createAddSourceParams(
                productUsageTokens,
                sourceType
            )
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
    @Throws(
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        AuthenticationException::class,
        CardException::class
    )
    override suspend fun deleteCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Source? {
        fireAnalyticsRequest(
            analyticsDataFactory.createDeleteSourceParams(productUsageTokens)
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
    @Throws(
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        AuthenticationException::class,
        CardException::class
    )
    override suspend fun attachPaymentMethod(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): PaymentMethod? {
        fireFingerprintRequest()
        fireAnalyticsRequest(
            analyticsDataFactory
                .createAttachPaymentMethodParams(productUsageTokens)
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
    @Throws(
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        AuthenticationException::class,
        CardException::class
    )
    override suspend fun detachPaymentMethod(
        publishableKey: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): PaymentMethod? {
        fireAnalyticsRequest(
            analyticsDataFactory
                .createDetachPaymentMethodParams(productUsageTokens)
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
    @Throws(
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        AuthenticationException::class,
        CardException::class
    )
    override suspend fun getPaymentMethods(
        listPaymentMethodsParams: ListPaymentMethodsParams,
        publishableKey: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options
    ): List<PaymentMethod> {
        val response = makeApiRequest(
            apiRequestFactory.createGet(
                paymentMethodsUrl,
                requestOptions,
                listPaymentMethodsParams.toParamMap()
            )
        )

        fireAnalyticsRequest(
            analyticsDataFactory.createParams(
                AnalyticsEvent.CustomerRetrievePaymentMethods,
                productUsageTokens = productUsageTokens
            )
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
    @Throws(
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        AuthenticationException::class,
        CardException::class
    )
    override suspend fun setDefaultCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options
    ): Customer? {
        fireAnalyticsRequest(
            analyticsDataFactory.createParams(
                event = AnalyticsEvent.CustomerSetDefaultSource,
                productUsageTokens = productUsageTokens,
                sourceType = sourceType
            )
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
    @Throws(
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        AuthenticationException::class,
        CardException::class
    )
    override suspend fun setCustomerShippingInfo(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        shippingInformation: ShippingInformation,
        requestOptions: ApiRequest.Options
    ): Customer? {
        fireAnalyticsRequest(
            analyticsDataFactory.createParams(
                AnalyticsEvent.CustomerSetShippingInfo,
                productUsageTokens = productUsageTokens
            )
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
    @Throws(
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        AuthenticationException::class,
        CardException::class
    )
    override suspend fun retrieveCustomer(
        customerId: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options
    ): Customer? {
        fireAnalyticsRequest(
            analyticsDataFactory.createParams(
                AnalyticsEvent.CustomerRetrieve,
                productUsageTokens = productUsageTokens
            )
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
    @Throws(
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        AuthenticationException::class,
        CardException::class,
        JSONException::class
    )
    override suspend fun retrieveIssuingCardPin(
        cardId: String,
        verificationId: String,
        userOneTimeCode: String,
        ephemeralKeySecret: String
    ): String {
        fireAnalyticsRequest(
            AnalyticsEvent.IssuingRetrievePin
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
    @Throws(
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        AuthenticationException::class,
        CardException::class
    )
    override suspend fun updateIssuingCardPin(
        cardId: String,
        newPin: String,
        verificationId: String,
        userOneTimeCode: String,
        ephemeralKeySecret: String
    ) {
        fireAnalyticsRequest(
            AnalyticsEvent.IssuingUpdatePin
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

    override suspend fun getFpxBankStatus(
        options: ApiRequest.Options
    ): FpxBankStatuses {
        return makeApiRequest(
            apiRequestFactory.createGet(
                getApiUrl("fpx/bank_statuses"),

                // don't pass connected account
                options.copy(stripeAccount = null),

                mapOf("account_holder_type" to "individual")
            )
        ).let {
            FpxBankStatusesJsonParser().parse(it.responseJson)
        }
    }

    override suspend fun getCardMetadata(
        bin: Bin,
        options: ApiRequest.Options
    ): CardMetadata? {
        return runCatching {
            makeApiRequest(
                apiRequestFactory.createGet(
                    getEdgeUrl("card-metadata"),
                    options.copy(stripeAccount = null),
                    mapOf("key" to options.apiKey, "bin_prefix" to bin.value)
                )
            ).let {
                CardMetadataJsonParser(bin).parse(it.responseJson)
            }
        }.onFailure {
            fireAnalyticsRequest(
                AnalyticsEvent.CardMetadataLoadFailure
            )
        }.getOrNull()
    }

    /**
     * Analytics event: [AnalyticsEvent.Auth3ds2Start]
     */
    @VisibleForTesting
    override suspend fun start3ds2Auth(
        authParams: Stripe3ds2AuthParams,
        stripeIntentId: String,
        requestOptions: ApiRequest.Options
    ): Stripe3ds2AuthResult {
        fireAnalyticsRequest(
            analyticsDataFactory.createAuthParams(
                AnalyticsEvent.Auth3ds2Start,
                stripeIntentId
            )
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

    override suspend fun complete3ds2Auth(
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Complete3ds2Result {
        val response = makeApiRequest(
            apiRequestFactory.createPost(
                getApiUrl("3ds2/challenge_complete"),
                requestOptions,
                mapOf("source" to sourceId)
            )
        )
        return Complete3ds2Result(response.isOk)
    }

    /**
     * Analytics event: [AnalyticsEvent.FileCreate]
     */
    override suspend fun createFile(
        fileParams: StripeFileParams,
        requestOptions: ApiRequest.Options
    ): StripeFile {
        val response = makeFileUploadRequest(
            FileUploadRequest(fileParams, requestOptions, appInfo)
        )
        fireAnalyticsRequest(AnalyticsEvent.FileCreate)
        return StripeFileJsonParser().parse(response.responseJson)
    }

    @Throws(
        IllegalArgumentException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class,
        CardException::class,
        AuthenticationException::class
    )
    override suspend fun retrieveObject(
        url: String,
        requestOptions: ApiRequest.Options
    ): JSONObject {
        if (!StripeUrlUtils.isStripeUrl(url)) {
            throw IllegalArgumentException("Unrecognized domain: $url")
        }
        val response = makeApiRequest(
            apiRequestFactory.createGet(
                url,
                requestOptions
            )
        )
        return response.responseJson
    }

    /**
     * @return `https://api.stripe.com/v1/payment_methods/:id/detach`
     */
    @VisibleForTesting
    internal fun getDetachPaymentMethodUrl(paymentMethodId: String): String {
        return getApiUrl("payment_methods/%s/detach", paymentMethodId)
    }

    @Throws(
        InvalidRequestException::class,
        AuthenticationException::class,
        CardException::class,
        APIException::class
    )
    private fun handleApiError(response: StripeResponse) {
        val requestId = response.requestId?.value
        val responseCode = response.code
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

    private suspend fun <ModelType : StripeModel> fetchStripeModel(
        apiRequest: ApiRequest,
        jsonParser: ModelJsonParser<ModelType>
    ): ModelType? {
        return jsonParser.parse(makeApiRequest(apiRequest).responseJson)
    }

    @VisibleForTesting
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        CardException::class,
        APIException::class
    )
    internal fun makeApiRequest(apiRequest: ApiRequest): StripeResponse {
        val dnsCacheData = disableDnsCache()

        val response = runCatching {
            stripeApiRequestExecutor.execute(apiRequest)
        }.getOrElse {
            throw when (it) {
                is IOException -> APIConnectionException.create(it, apiRequest.baseUrl)
                else -> it
            }
        }

        if (response.isError) {
            handleApiError(response)
        }

        resetDnsCacheTtl(dnsCacheData)

        return response
    }

    @VisibleForTesting
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        CardException::class,
        APIException::class
    )
    internal fun makeFileUploadRequest(fileUploadRequest: FileUploadRequest): StripeResponse {
        val dnsCacheData = disableDnsCache()

        val response = runCatching {
            stripeApiRequestExecutor.execute(fileUploadRequest)
        }.getOrElse {
            throw when (it) {
                is IOException -> APIConnectionException.create(it, fileUploadRequest.baseUrl)
                else -> it
            }
        }

        if (response.isError) {
            handleApiError(response)
        }

        resetDnsCacheTtl(dnsCacheData)

        return response
    }

    private fun makeAnalyticsRequest(request: AnalyticsRequest) {
        analyticsRequestExecutor.executeAsync(request)
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
            Security.setProperty(
                DNS_CACHE_TTL_PROPERTY_NAME,
                dnsCacheData.second ?: "-1"
            )
        }
    }

    private fun fireFingerprintRequest() {
        fingerprintDataRepository.refresh()
    }

    private fun fireAnalyticsRequest(
        event: AnalyticsEvent
    ) {
        fireAnalyticsRequest(
            analyticsDataFactory.createParams(event)
        )
    }

    @VisibleForTesting
    internal fun fireAnalyticsRequest(
        params: Map<String, Any>
    ) {
        makeAnalyticsRequest(
            analyticsRequestFactory.create(params)
        )
    }

    private fun createClientSecretParam(
        clientSecret: String,
        expandFields: List<String>
    ): Map<String, Any> {
        return mapOf("client_secret" to clientSecret)
            .plus(createExpandParam(expandFields))
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

        private fun getEdgeUrl(path: String): String {
            return "${ApiRequest.API_HOST}/edge-internal/$path"
        }

        private fun createExpandParam(expandFields: List<String>): Map<String, List<String>> {
            return expandFields.takeIf { it.isNotEmpty() }?.let {
                mapOf("expand" to it)
            }.orEmpty()
        }
    }
}
