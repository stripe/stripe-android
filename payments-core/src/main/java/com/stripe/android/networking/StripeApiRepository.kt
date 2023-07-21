package com.stripe.android.networking

import android.content.Context
import android.net.http.HttpResponseCache
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.DefaultFraudDetectionDataRepository
import com.stripe.android.FraudDetectionDataRepository
import com.stripe.android.Stripe
import com.stripe.android.StripeApiBeta
import com.stripe.android.cards.Bin
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.CardNumber
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.AppInfo
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.exception.PermissionException
import com.stripe.android.core.exception.RateLimitException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFileParams
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.model.parsers.StripeFileJsonParser
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.FileUploadRequest
import com.stripe.android.core.networking.HTTP_TOO_MANY_REQUESTS
import com.stripe.android.core.networking.RequestId
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.networking.responseJson
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.exception.CardException
import com.stripe.android.model.BankStatuses
import com.stripe.android.model.CardMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_CLIENT_SECRET
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsCreateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.CreateFinancialConnectionsSessionForDeferredPaymentParams
import com.stripe.android.model.CreateFinancialConnectionsSessionParams
import com.stripe.android.model.Customer
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.FinancialConnectionsSession
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.model.RadarSession
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.Source
import com.stripe.android.model.SourceParams
import com.stripe.android.model.Stripe3ds2AuthParams
import com.stripe.android.model.Stripe3ds2AuthResult
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.Token
import com.stripe.android.model.TokenParams
import com.stripe.android.model.parsers.CardMetadataJsonParser
import com.stripe.android.model.parsers.ConsumerPaymentDetailsJsonParser
import com.stripe.android.model.parsers.ConsumerSessionJsonParser
import com.stripe.android.model.parsers.CustomerJsonParser
import com.stripe.android.model.parsers.ElementsSessionJsonParser
import com.stripe.android.model.parsers.FinancialConnectionsSessionJsonParser
import com.stripe.android.model.parsers.FpxBankStatusesJsonParser
import com.stripe.android.model.parsers.IssuingCardPinJsonParser
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.model.parsers.PaymentMethodMessageJsonParser
import com.stripe.android.model.parsers.PaymentMethodsListJsonParser
import com.stripe.android.model.parsers.RadarSessionJsonParser
import com.stripe.android.model.parsers.SetupIntentJsonParser
import com.stripe.android.model.parsers.SourceJsonParser
import com.stripe.android.model.parsers.Stripe3ds2AuthResultJsonParser
import com.stripe.android.model.parsers.TokenJsonParser
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.utils.StripeUrlUtils
import com.stripe.android.utils.mapResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.security.Security
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

/**
 * An implementation of [StripeRepository] that makes network requests to the Stripe API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class StripeApiRepository @JvmOverloads internal constructor(
    private val context: Context,
    private val publishableKeyProvider: () -> String,
    private val appInfo: AppInfo? = Stripe.appInfo,
    private val logger: Logger = Logger.noop(),
    private val workContext: CoroutineContext = Dispatchers.IO,
    private val productUsageTokens: Set<String> = emptySet(),
    private val stripeNetworkClient: StripeNetworkClient = DefaultStripeNetworkClient(
        workContext = workContext,
        logger = logger
    ),
    private val analyticsRequestExecutor: AnalyticsRequestExecutor =
        DefaultAnalyticsRequestExecutor(logger, workContext),
    private val fraudDetectionDataRepository: FraudDetectionDataRepository =
        DefaultFraudDetectionDataRepository(context, workContext),
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory =
        DefaultCardAccountRangeRepositoryFactory(context, analyticsRequestExecutor),
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory =
        PaymentAnalyticsRequestFactory(context, publishableKeyProvider, productUsageTokens),
    private val fraudDetectionDataParamsUtils: FraudDetectionDataParamsUtils = FraudDetectionDataParamsUtils(),
    betas: Set<StripeApiBeta> = emptySet(),
    apiVersion: String = ApiVersion(betas = betas.map { it.code }.toSet()).code,
    sdkVersion: String = StripeSdkVersion.VERSION
) : StripeRepository {

    @Inject
    constructor(
        appContext: Context,
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @IOContext workContext: CoroutineContext,
        @Named(PRODUCT_USAGE) productUsageTokens: Set<String>,
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        logger: Logger
    ) : this(
        context = appContext,
        publishableKeyProvider = publishableKeyProvider,
        logger = logger,
        workContext = workContext,
        productUsageTokens = productUsageTokens,
        paymentAnalyticsRequestFactory = paymentAnalyticsRequestFactory,
        analyticsRequestExecutor = analyticsRequestExecutor
    )

    private val apiRequestFactory = ApiRequest.Factory(
        appInfo = appInfo,
        apiVersion = apiVersion,
        sdkVersion = sdkVersion
    )

    private val fraudDetectionData: FraudDetectionData?
        get() = fraudDetectionDataRepository.getCached()

    init {
        fireFraudDetectionDataRequest()

        CoroutineScope(workContext).launch {
            val httpCacheDir = File(context.cacheDir, "stripe_api_repository_cache")
            val httpCacheSize = (10 * 1024 * 1024).toLong() // 10 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize)
        }
    }

    override suspend fun retrieveStripeIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): Result<StripeIntent> {
        return when {
            PaymentIntent.ClientSecret.isMatch(clientSecret) -> {
                retrievePaymentIntent(clientSecret, options, expandFields)
            }
            SetupIntent.ClientSecret.isMatch(clientSecret) -> {
                retrieveSetupIntent(clientSecret, options, expandFields)
            }
            else -> {
                Result.failure(IllegalStateException("Invalid client secret."))
            }
        }
    }

    /**
     * Confirm a [PaymentIntent] using the provided [ConfirmPaymentIntentParams]
     *
     * Analytics event: [PaymentAnalyticsEvent.PaymentIntentConfirm]
     *
     * @param confirmPaymentIntentParams contains the confirmation params
     * @return a [PaymentIntent] reflecting the updated state after applying the parameter
     * provided
     */
    override suspend fun confirmPaymentIntent(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): Result<PaymentIntent> {
        return confirmPaymentIntentParams.maybeForDashboard(options).mapResult {
            confirmPaymentIntentInternal(
                confirmPaymentIntentParams = it,
                options = options,
                expandFields = expandFields
            )
        }
    }

    private suspend fun confirmPaymentIntentInternal(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): Result<PaymentIntent> {
        val params = fraudDetectionDataParamsUtils.addFraudDetectionData(
            // Add payment_user_agent if the Payment Method is being created on this call
            maybeAddPaymentUserAgent(
                confirmPaymentIntentParams.toParamMap()
                    // Omit client_secret with user key auth.
                    .let { if (options.apiKeyIsUserKey) it.minus(PARAM_CLIENT_SECRET) else it },
                confirmPaymentIntentParams.paymentMethodCreateParams,
                confirmPaymentIntentParams.sourceParams
            ).plus(createExpandParam(expandFields)),
            fraudDetectionData
        )

        val paymentIntentId = runCatching {
            PaymentIntent.ClientSecret(confirmPaymentIntentParams.clientSecret).paymentIntentId
        }.getOrElse {
            return Result.failure(it)
        }

        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getConfirmPaymentIntentUrl(paymentIntentId),
                options = options,
                params = params,
            ),
            jsonParser = PaymentIntentJsonParser(),
        ) {
            val paymentMethodType =
                confirmPaymentIntentParams.paymentMethodCreateParams?.typeCode
                    ?: confirmPaymentIntentParams.sourceParams?.type
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createPaymentIntentConfirmation(
                    paymentMethodType
                )
            )
        }
    }

    /**
     * Retrieve a [PaymentIntent] using its client_secret
     *
     * Analytics event: [PaymentAnalyticsEvent.PaymentIntentRetrieve]
     *
     * @param clientSecret client_secret of the PaymentIntent to retrieve
     */
    override suspend fun retrievePaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): Result<PaymentIntent> {
        val paymentIntentId = runCatching {
            PaymentIntent.ClientSecret(clientSecret).paymentIntentId
        }.getOrElse {
            return Result.failure(it)
        }

        val params = if (options.apiKeyIsUserKey) {
            createExpandParam(expandFields)
        } else {
            createClientSecretParam(clientSecret, expandFields)
        }

        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = getRetrievePaymentIntentUrl(paymentIntentId),
                options = options,
                params = params,
            ),
            jsonParser = PaymentIntentJsonParser(),
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.PaymentIntentRetrieve)
            )
        }
    }

    /**
     * Refresh a [PaymentIntent] using its client_secret
     *
     * Analytics event: [PaymentAnalyticsEvent.PaymentIntentRefresh]
     *
     * @param clientSecret client_secret of the PaymentIntent to retrieve
     */
    override suspend fun refreshPaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options
    ): Result<PaymentIntent> {
        val paymentIntentId = runCatching {
            PaymentIntent.ClientSecret(clientSecret).paymentIntentId
        }.getOrElse {
            return Result.failure(it)
        }

        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getRefreshPaymentIntentUrl(paymentIntentId),
                options = options,
                params = createClientSecretParam(clientSecret, emptyList()),
            ),
            jsonParser = PaymentIntentJsonParser(),
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.PaymentIntentRefresh)
            )
        }
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.PaymentIntentCancelSource]
     */
    override suspend fun cancelPaymentIntentSource(
        paymentIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): Result<PaymentIntent> {
        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getCancelPaymentIntentSourceUrl(paymentIntentId),
                options = options,
                params = mapOf("source" to sourceId),
            ),
            jsonParser = PaymentIntentJsonParser(),
        ) {
            fireAnalyticsRequest(PaymentAnalyticsEvent.PaymentIntentCancelSource)
        }
    }

    /**
     * Confirm a [SetupIntent] using the provided [ConfirmSetupIntentParams]
     *
     * Analytics event: [PaymentAnalyticsEvent.SetupIntentConfirm]
     *
     * @param confirmSetupIntentParams contains the confirmation params
     * @return a [SetupIntent] reflecting the updated state after applying the parameter
     * provided
     */
    override suspend fun confirmSetupIntent(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): Result<SetupIntent> {
        val setupIntentId = runCatching {
            SetupIntent.ClientSecret(confirmSetupIntentParams.clientSecret).setupIntentId
        }.getOrElse {
            return Result.failure(it)
        }

        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                getConfirmSetupIntentUrl(setupIntentId),
                options,
                fraudDetectionDataParamsUtils.addFraudDetectionData(
                    // Add payment_user_agent if the Payment Method is being created on this call
                    maybeAddPaymentUserAgent(
                        confirmSetupIntentParams.toParamMap(),
                        confirmSetupIntentParams.paymentMethodCreateParams
                    ).plus(createExpandParam(expandFields)),
                    fraudDetectionData
                )
            ),
            SetupIntentJsonParser()
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createSetupIntentConfirmation(
                    confirmSetupIntentParams.paymentMethodCreateParams?.typeCode
                )
            )
        }
    }

    /**
     * Retrieve a [SetupIntent] using its client_secret
     *
     * Analytics event: [PaymentAnalyticsEvent.SetupIntentRetrieve]
     *
     * @param clientSecret client_secret of the SetupIntent to retrieve
     */
    override suspend fun retrieveSetupIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): Result<SetupIntent> {
        val setupIntentId = runCatching {
            SetupIntent.ClientSecret(clientSecret).setupIntentId
        }.getOrElse {
            return Result.failure(it)
        }

        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = getRetrieveSetupIntentUrl(setupIntentId),
                options = options,
                params = createClientSecretParam(clientSecret, expandFields),
            ),
            jsonParser = SetupIntentJsonParser(),
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.SetupIntentRetrieve)
            )
        }
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.SetupIntentCancelSource]
     */
    override suspend fun cancelSetupIntentSource(
        setupIntentId: String,
        sourceId: String,
        options: ApiRequest.Options
    ): Result<SetupIntent> {
        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                getCancelSetupIntentSourceUrl(setupIntentId),
                options,
                mapOf("source" to sourceId)
            ),
            SetupIntentJsonParser()
        ) {
            fireAnalyticsRequest(PaymentAnalyticsEvent.SetupIntentCancelSource)
        }
    }

    /**
     * Create a [Source] using the input [SourceParams].
     *
     * Analytics event: [PaymentAnalyticsEvent.SourceCreate]
     *
     * @param sourceParams a [SourceParams] object with [Source] creation params
     * @return a [Result] containing the generated [Source] or the encountered [Exception]
     */
    override suspend fun createSource(
        sourceParams: SourceParams,
        options: ApiRequest.Options
    ): Result<Source> {
        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                sourcesUrl,
                options,
                sourceParams.toParamMap()
                    .plus(buildPaymentUserAgentPair(sourceParams.attribution))
                    .plus(fraudDetectionData?.params.orEmpty())
            ),
            SourceJsonParser()
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createSourceCreation(
                    sourceParams.type,
                    sourceParams.attribution
                )
            )
        }
    }

    /**
     * Retrieve an existing [Source] object from the server.
     *
     * @param sourceId the [Source.id] field for the Source to query
     * @param clientSecret the [Source.clientSecret] field for the Source to query
     * @return a [Result] containing the retrieved [Source] or the encountered [Exception]
     */
    override suspend fun retrieveSource(
        sourceId: String,
        clientSecret: String,
        options: ApiRequest.Options
    ): Result<Source> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = getRetrieveSourceApiUrl(sourceId),
                options = options,
                params = SourceParams.createRetrieveSourceParams(clientSecret),
            ),
            jsonParser = SourceJsonParser(),
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.SourceRetrieve)
            )
        }
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.PaymentMethodCreate]
     */
    override suspend fun createPaymentMethod(
        paymentMethodCreateParams: PaymentMethodCreateParams,
        options: ApiRequest.Options
    ): Result<PaymentMethod> {
        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                paymentMethodsUrl,
                options,
                paymentMethodCreateParams.toParamMap()
                    .plus(buildPaymentUserAgentPair(paymentMethodCreateParams.attribution))
                    .plus(fraudDetectionData?.params.orEmpty())
            ),
            PaymentMethodJsonParser()
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createPaymentMethodCreation(
                    paymentMethodCreateParams.code,
                    productUsageTokens = paymentMethodCreateParams.attribution
                )
            )
        }
    }

    /**
     * Create a [Token] using the input token parameters.
     *
     * Analytics event: [PaymentAnalyticsEvent.TokenCreate]
     *
     * @param tokenParams a [TokenParams] representing the object for which this token is being created
     * @param options a [ApiRequest.Options] object that contains connection data like the api
     * key, api version, etc
     *
     * @return a [Result] containing the generated [Token] or the encountered [Exception]
     */
    override suspend fun createToken(
        tokenParams: TokenParams,
        options: ApiRequest.Options
    ): Result<Token> {
        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                tokensUrl,
                options,
                tokenParams.toParamMap()
                    .plus(buildPaymentUserAgentPair(tokenParams.attribution))
                    .plus(fraudDetectionData?.params.orEmpty())
            ),
            TokenJsonParser()
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createTokenCreation(
                    productUsageTokens = tokenParams.attribution,
                    tokenType = tokenParams.tokenType
                )
            )
        }
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.CustomerAddSource]
     */
    override suspend fun addCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options
    ): Result<Source> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getAddCustomerSourceUrl(customerId),
                options = requestOptions,
                params = mapOf("source" to sourceId),
            ),
            jsonParser = SourceJsonParser(),
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createAddSource(
                    productUsageTokens,
                    sourceType
                )
            )
        }
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.CustomerDeleteSource]
     */
    override suspend fun deleteCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Result<Source> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createDelete(
                url = getDeleteCustomerSourceUrl(customerId, sourceId),
                options = requestOptions,
            ),
            jsonParser = SourceJsonParser(),
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createDeleteSource(
                    productUsageTokens
                )
            )
        }
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.CustomerAttachPaymentMethod]
     */
    override suspend fun attachPaymentMethod(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethod> {
        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getAttachPaymentMethodUrl(paymentMethodId),
                options = requestOptions,
                params = mapOf("customer" to customerId)
            ),
            jsonParser = PaymentMethodJsonParser()
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createAttachPaymentMethod(productUsageTokens)
            )
        }
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.CustomerDetachPaymentMethod]
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
    ): Result<PaymentMethod> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getDetachPaymentMethodUrl(paymentMethodId),
                options = requestOptions,
            ),
            jsonParser = PaymentMethodJsonParser()
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createDetachPaymentMethod(productUsageTokens)
            )
        }
    }

    /**
     * Retrieve a Customer's [PaymentMethod]s
     *
     * Analytics event: [PaymentAnalyticsEvent.CustomerRetrievePaymentMethods]
     */
    override suspend fun getPaymentMethods(
        listPaymentMethodsParams: ListPaymentMethodsParams,
        publishableKey: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options
    ): Result<List<PaymentMethod>> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = paymentMethodsUrl,
                options = requestOptions,
                params = listPaymentMethodsParams.toParamMap(),
            ),
            jsonParser = PaymentMethodsListJsonParser(),
            onResponse = {
                fireAnalyticsRequest(
                    paymentAnalyticsRequestFactory.createRequest(
                        PaymentAnalyticsEvent.CustomerRetrievePaymentMethods,
                        productUsageTokens = productUsageTokens
                    )
                )
            },
        ).map {
            it.paymentMethods
        }
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.CustomerSetDefaultSource]
     */
    override suspend fun setDefaultCustomerSource(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        sourceId: String,
        @Source.SourceType sourceType: String,
        requestOptions: ApiRequest.Options
    ): Result<Customer> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getRetrieveCustomerUrl(customerId),
                options = requestOptions,
                params = mapOf("default_source" to sourceId),
            ),
            jsonParser = CustomerJsonParser(),
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(
                    event = PaymentAnalyticsEvent.CustomerSetDefaultSource,
                    productUsageTokens = productUsageTokens,
                    sourceType = sourceType
                )
            )
        }
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.CustomerSetShippingInfo]
     */
    override suspend fun setCustomerShippingInfo(
        customerId: String,
        publishableKey: String,
        productUsageTokens: Set<String>,
        shippingInformation: ShippingInformation,
        requestOptions: ApiRequest.Options
    ): Result<Customer> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getRetrieveCustomerUrl(customerId),
                options = requestOptions,
                params = mapOf("shipping" to shippingInformation.toParamMap()),
            ),
            jsonParser = CustomerJsonParser(),
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(
                    PaymentAnalyticsEvent.CustomerSetShippingInfo,
                    productUsageTokens = productUsageTokens
                )
            )
        }
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.CustomerRetrieve]
     */
    override suspend fun retrieveCustomer(
        customerId: String,
        productUsageTokens: Set<String>,
        requestOptions: ApiRequest.Options
    ): Result<Customer> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = getRetrieveCustomerUrl(customerId),
                options = requestOptions,
            ),
            jsonParser = CustomerJsonParser(),
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(
                    PaymentAnalyticsEvent.CustomerRetrieve,
                    productUsageTokens = productUsageTokens
                )
            )
        }
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.IssuingRetrievePin]
     */
    override suspend fun retrieveIssuingCardPin(
        cardId: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: ApiRequest.Options
    ): Result<String> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = getIssuingCardPinUrl(cardId),
                options = requestOptions,
                params = mapOf(
                    "verification" to createVerificationParam(verificationId, userOneTimeCode)
                ),
            ),
            jsonParser = IssuingCardPinJsonParser(),
            onResponse = { fireAnalyticsRequest(PaymentAnalyticsEvent.IssuingRetrievePin) },
        ).map { it.pin }
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.IssuingUpdatePin]
     */
    override suspend fun updateIssuingCardPin(
        cardId: String,
        newPin: String,
        verificationId: String,
        userOneTimeCode: String,
        requestOptions: ApiRequest.Options
    ): Throwable? {
        return runCatching {
            makeApiRequest(
                apiRequest = apiRequestFactory.createPost(
                    url = getIssuingCardPinUrl(cardId),
                    options = requestOptions,
                    params = mapOf(
                        "verification" to createVerificationParam(verificationId, userOneTimeCode),
                        "pin" to newPin,
                    ),
                ),
                onResponse = { fireAnalyticsRequest(PaymentAnalyticsEvent.IssuingUpdatePin) },
            )
        }.exceptionOrNull()
    }

    override suspend fun getFpxBankStatus(
        options: ApiRequest.Options
    ): Result<BankStatuses> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = getApiUrl("fpx/bank_statuses"),
                // don't pass connected account
                options = options.copy(stripeAccount = null),
                params = mapOf("account_holder_type" to "individual"),
            ),
            jsonParser = FpxBankStatusesJsonParser(),
        ) {
            fireAnalyticsRequest(PaymentAnalyticsEvent.FpxBankStatusesRetrieve)
        }
    }

    override suspend fun getCardMetadata(
        bin: Bin,
        options: ApiRequest.Options
    ): Result<CardMetadata> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = getEdgeUrl("card-metadata"),
                options = options.copy(stripeAccount = null),
                params = mapOf("key" to options.apiKey, "bin_prefix" to bin.value),
            ),
            jsonParser = CardMetadataJsonParser(bin),
        ).onFailure {
            fireAnalyticsRequest(PaymentAnalyticsEvent.CardMetadataLoadFailure)
        }
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.Auth3ds2Start]
     */
    @VisibleForTesting
    override suspend fun start3ds2Auth(
        authParams: Stripe3ds2AuthParams,
        requestOptions: ApiRequest.Options
    ): Result<Stripe3ds2AuthResult> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getApiUrl("3ds2/authenticate"),
                options = requestOptions,
                params = authParams.toParamMap(),
            ),
            jsonParser = Stripe3ds2AuthResultJsonParser(),
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.Auth3ds2Start)
            )
        }
    }

    override suspend fun complete3ds2Auth(
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Result<Stripe3ds2AuthResult> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getApiUrl("3ds2/challenge_complete"),
                options = requestOptions,
                params = mapOf("source" to sourceId),
            ),
            jsonParser = Stripe3ds2AuthResultJsonParser(),
        )
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.FileCreate]
     */
    override suspend fun createFile(
        fileParams: StripeFileParams,
        requestOptions: ApiRequest.Options
    ): Result<StripeFile> {
        val response = runCatching {
            makeFileUploadRequest(
                fileUploadRequest = FileUploadRequest(fileParams, requestOptions, appInfo),
                onResponse = { fireAnalyticsRequest(PaymentAnalyticsEvent.FileCreate) },
            )
        }

        return response.mapCatching {
            StripeFileJsonParser().parse(it.responseJson())
        }
    }

    override suspend fun retrieveObject(
        url: String,
        requestOptions: ApiRequest.Options
    ): Result<StripeResponse<String>> {
        if (!StripeUrlUtils.isStripeUrl(url)) {
            return Result.failure(IllegalArgumentException("Unrecognized domain: $url"))
        }

        return runCatching {
            makeApiRequest(
                apiRequest = apiRequestFactory.createGet(url, requestOptions),
                onResponse = { fireAnalyticsRequest(PaymentAnalyticsEvent.StripeUrlRetrieve) },
            )
        }
    }

    /**
     * Get the latest [FraudDetectionData] from [FraudDetectionDataRepository] and send in POST request
     * to `/v1/radar/session`.
     */
    override suspend fun createRadarSession(
        requestOptions: ApiRequest.Options
    ): Result<RadarSession> {
        val validation = runCatching {
            require(Stripe.advancedFraudSignalsEnabled) {
                "Stripe.advancedFraudSignalsEnabled must be set to 'true' to create a Radar Session."
            }

            requireNotNull(fraudDetectionDataRepository.getLatest()) {
                "Could not obtain fraud data required to create a Radar Session."
            }
        }

        return validation.mapCatching { fraudData ->
            val params = fraudData.params + buildPaymentUserAgentPair()

            fetchStripeModelResult(
                apiRequest = apiRequestFactory.createPost(
                    url = getApiUrl("radar/session"),
                    options = requestOptions,
                    params = params,
                ),
                jsonParser = RadarSessionJsonParser(),
            ) {
                fireAnalyticsRequest(
                    paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.RadarSessionCreate)
                )
            }
        }.getOrElse {
            Result.failure(StripeException.create(it))
        }
    }

    /**
     * Creates a new Link account for the credentials provided.
     */
    override suspend fun consumerSignUp(
        email: String,
        phoneNumber: String,
        country: String,
        name: String?,
        locale: Locale?,
        authSessionCookie: String?,
        consentAction: ConsumerSignUpConsentAction,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerSession> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = consumerSignUpUrl,
                options = requestOptions,
                params = mapOf(
                    "request_surface" to "android_payment_element",
                    "email_address" to email.lowercase(),
                    "phone_number" to phoneNumber,
                    "country" to country,
                    "consent_action" to consentAction.value
                ).plus(
                    authSessionCookie?.let {
                        mapOf(
                            "cookies" to
                                mapOf("verification_session_client_secrets" to listOf(it))
                        )
                    } ?: emptyMap()
                ).plus(
                    locale?.let {
                        mapOf("locale" to it.toLanguageTag())
                    } ?: emptyMap()
                ).plus(
                    name?.let {
                        mapOf("legal_name" to it)
                    } ?: emptyMap()
                )
            ),
            jsonParser = ConsumerSessionJsonParser(),
        )
    }

    override suspend fun createPaymentDetails(
        consumerSessionClientSecret: String,
        paymentDetailsCreateParams: ConsumerPaymentDetailsCreateParams,
        requestOptions: ApiRequest.Options
    ): Result<ConsumerPaymentDetails> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = consumerPaymentDetailsUrl,
                options = requestOptions,
                params = mapOf(
                    "request_surface" to "android_payment_element",
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                    "active" to false
                ).plus(
                    paymentDetailsCreateParams.toParamMap()
                )
            ),
            jsonParser = ConsumerPaymentDetailsJsonParser(),
        )
    }

    override suspend fun createFinancialConnectionsSessionForDeferredPayments(
        params: CreateFinancialConnectionsSessionForDeferredPaymentParams,
        requestOptions: ApiRequest.Options
    ): Result<FinancialConnectionsSession> {
        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                url = deferredFinancialConnectionsSessionUrl,
                options = requestOptions,
                params = params.toMap()
            ),
            FinancialConnectionsSessionJsonParser()
        ) {
            // no-op
        }
    }

    override suspend fun createPaymentIntentFinancialConnectionsSession(
        paymentIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: ApiRequest.Options
    ): Result<FinancialConnectionsSession> {
        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                url = getPaymentIntentFinancialConnectionsSessionUrl(paymentIntentId),
                options = requestOptions,
                params = params.toMap()
            ),
            FinancialConnectionsSessionJsonParser()
        ) {
            // no-op
        }
    }

    override suspend fun createSetupIntentFinancialConnectionsSession(
        setupIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: ApiRequest.Options
    ): Result<FinancialConnectionsSession> {
        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                url = getSetupIntentFinancialConnectionsSessionUrl(setupIntentId),
                options = requestOptions,
                params = params.toMap()
            ),
            FinancialConnectionsSessionJsonParser()
        ) {
            // no-op
        }
    }

    /**
     * @return `https://api.stripe.com/v1/payment_intents/:id/link_account_session`
     */
    @VisibleForTesting
    @JvmSynthetic
    internal fun getPaymentIntentFinancialConnectionsSessionUrl(paymentIntentId: String): String {
        return getApiUrl("payment_intents/%s/link_account_sessions", paymentIntentId)
    }

    /**
     * @return `https://api.stripe.com/v1/setup_intents/:id/link_account_session`
     */
    @VisibleForTesting
    @JvmSynthetic
    internal fun getSetupIntentFinancialConnectionsSessionUrl(setupIntentId: String): String {
        return getApiUrl("setup_intents/%s/link_account_sessions", setupIntentId)
    }

    /**
     * Attaches the Link Account Session to the Payment Intent
     */
    override suspend fun attachFinancialConnectionsSessionToPaymentIntent(
        clientSecret: String,
        paymentIntentId: String,
        financialConnectionsSessionId: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): Result<PaymentIntent> {
        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                getAttachFinancialConnectionsSessionToPaymentIntentUrl(
                    paymentIntentId,
                    financialConnectionsSessionId
                ),
                requestOptions,
                mapOf("client_secret" to clientSecret)
                    .plus(createExpandParam(expandFields))
            ),
            PaymentIntentJsonParser()
        ) {
            // no-op
        }
    }

    /**
     * Attaches the Link Account Session to the Setup Intent
     */
    override suspend fun attachFinancialConnectionsSessionToSetupIntent(
        clientSecret: String,
        setupIntentId: String,
        financialConnectionsSessionId: String,
        requestOptions: ApiRequest.Options,
        expandFields: List<String>
    ): Result<SetupIntent> {
        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                getAttachFinancialConnectionsSessionToSetupIntentUrl(
                    setupIntentId,
                    financialConnectionsSessionId
                ),
                requestOptions,
                mapOf("client_secret" to clientSecret)
                    .plus(createExpandParam(expandFields))
            ),
            SetupIntentJsonParser()
        ) {
            // no-op
        }
    }

    /**
     * Verifies the PaymentIntent with microdeposits amounts
     */
    override suspend fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        requestOptions: ApiRequest.Options
    ): Result<PaymentIntent> {
        val paymentIntentId = runCatching {
            PaymentIntent.ClientSecret(clientSecret).paymentIntentId
        }.getOrElse {
            return Result.failure(it)
        }

        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                getVerifyMicrodepositsOnPaymentIntentUrl(paymentIntentId),
                requestOptions,
                mapOf(
                    "client_secret" to clientSecret,
                    "amounts" to listOf(firstAmount, secondAmount)
                )
            ),
            PaymentIntentJsonParser()
        ) {
            // no-op
        }
    }

    /**
     * Verifies the PaymentIntent with microdeposits descriptor code
     */
    override suspend fun verifyPaymentIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentIntent> {
        val paymentIntentId = runCatching {
            PaymentIntent.ClientSecret(clientSecret).paymentIntentId
        }.getOrElse {
            return Result.failure(it)
        }

        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                getVerifyMicrodepositsOnPaymentIntentUrl(paymentIntentId),
                requestOptions,
                mapOf(
                    "client_secret" to clientSecret,
                    "descriptor_code" to descriptorCode
                )
            ),
            PaymentIntentJsonParser()
        ) {
            // no-op
        }
    }

    /**
     * Verifies the SetupIntent with microdeposits amounts
     */
    override suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        firstAmount: Int,
        secondAmount: Int,
        requestOptions: ApiRequest.Options
    ): Result<SetupIntent> {
        val setupIntentId = runCatching {
            SetupIntent.ClientSecret(clientSecret).setupIntentId
        }.getOrElse {
            return Result.failure(it)
        }

        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                getVerifyMicrodepositsOnSetupIntentUrl(setupIntentId),
                requestOptions,
                mapOf(
                    "client_secret" to clientSecret,
                    "amounts" to listOf(firstAmount, secondAmount)
                )
            ),
            SetupIntentJsonParser()
        )
    }

    /**
     * Verifies the SetupIntent with microdeposits descriptor code
     */
    override suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: ApiRequest.Options
    ): Result<SetupIntent> {
        val setupIntentId = runCatching {
            SetupIntent.ClientSecret(clientSecret).setupIntentId
        }.getOrElse {
            return Result.failure(it)
        }

        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                getVerifyMicrodepositsOnSetupIntentUrl(setupIntentId),
                requestOptions,
                mapOf(
                    "client_secret" to clientSecret,
                    "descriptor_code" to descriptorCode
                )
            ),
            SetupIntentJsonParser()
        )
    }

    override suspend fun retrievePaymentMethodMessage(
        paymentMethods: List<String>,
        amount: Int,
        currency: String,
        country: String,
        locale: String,
        logoColor: String,
        requestOptions: ApiRequest.Options
    ): Result<PaymentMethodMessage> {
        return fetchStripeModelResult(
            apiRequestFactory.createGet(
                url = "https://ppm.stripe.com/content",
                options = requestOptions,
                params = mapOf<String, Any>(
                    "amount" to amount,
                    "client" to "android",
                    "country" to country,
                    "currency" to currency,
                    "locale" to locale,
                    "logo_color" to logoColor,
                ) + paymentMethods.mapIndexed { index, paymentMethod ->
                    Pair("payment_methods[$index]", paymentMethod)
                }
            ),
            PaymentMethodMessageJsonParser()
        ) {
            // no-op
        }
    }

    /**
     * @return `https://api.stripe.com/v1/payment_methods/:id/detach`
     */
    @VisibleForTesting
    internal fun getDetachPaymentMethodUrl(paymentMethodId: String): String {
        return getApiUrl("payment_methods/%s/detach", paymentMethodId)
    }

    override suspend fun retrieveElementsSession(
        params: ElementsSessionParams,
        options: ApiRequest.Options,
    ): Result<ElementsSession> {
        return retrieveElementsSession(
            params = params,
            options = options,
            analyticsEvent = null,
        )
    }

    override suspend fun retrieveCardMetadata(
        cardNumber: String,
        requestOptions: ApiRequest.Options
    ): Result<CardMetadata> {
        val unvalidatedNumber = CardNumber.Unvalidated(cardNumber)

        val bin = unvalidatedNumber.bin ?: return Result.failure(
            InvalidRequestException(
                message = "cardNumber cannot be less than 6 characters",
            )
        )

        val cardAccountRangeRepository =
            cardAccountRangeRepositoryFactory.createWithStripeRepository(
                stripeRepository = this,
                publishableKey = publishableKeyProvider()
            )

        val accountRanges = cardAccountRangeRepository.getAccountRanges(
            cardNumber = unvalidatedNumber
        ).orEmpty()

        return Result.success(
            CardMetadata(
                bin = bin,
                accountRanges = accountRanges
            )
        )
    }

    private suspend fun retrieveElementsSession(
        params: ElementsSessionParams,
        options: ApiRequest.Options,
        analyticsEvent: PaymentAnalyticsEvent?,
    ): Result<ElementsSession> {
        // Unsupported for user key sessions.
        if (options.apiKeyIsUserKey) {
            return Result.failure(IllegalArgumentException("Invalid API key"))
        }

        fireFraudDetectionDataRequest()

        val parser = ElementsSessionJsonParser(
            params = params,
            apiKey = options.apiKey
        )

        val requestParams = buildMap {
            this["type"] = params.type
            params.clientSecret?.let { this["client_secret"] = it }
            params.locale.let { this["locale"] = it }
            (params as? ElementsSessionParams.DeferredIntentType)?.let { type ->
                this.putAll(type.deferredIntentParams.toQueryParams())
            }
        }

        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = getApiUrl("elements/sessions"),
                options = options,
                params = requestParams + createExpandParam(params.expandFields),
            ),
            jsonParser = parser,
        ) {
            analyticsEvent?.let {
                fireAnalyticsRequest(paymentAnalyticsRequestFactory.createRequest(analyticsEvent))
            }
        }
    }

    @Throws(
        InvalidRequestException::class,
        AuthenticationException::class,
        CardException::class,
        APIException::class
    )
    private fun handleApiError(response: StripeResponse<String>) {
        val requestId = response.requestId?.value
        val responseCode = response.code

        val stripeError = StripeErrorJsonParser()
            .parse(response.responseJson())
            .withLocalizedMessage(context)

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
            HTTP_TOO_MANY_REQUESTS -> {
                throw RateLimitException(stripeError, requestId)
            }
            else -> {
                throw APIException(stripeError, requestId, responseCode)
            }
        }
    }

    private suspend fun <ModelType : StripeModel> fetchStripeModelResult(
        apiRequest: ApiRequest,
        jsonParser: ModelJsonParser<ModelType>,
        onResponse: () -> Unit = {},
    ): Result<ModelType> {
        return runCatching {
            val response = makeApiRequest(apiRequest, onResponse).responseJson()
            jsonParser.parse(response) ?: throw APIException(
                message = "Unable to parse response with ${jsonParser::class.java.simpleName}",
            )
        }
    }

    @VisibleForTesting
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        CardException::class,
        APIException::class
    )
    internal suspend fun makeApiRequest(
        apiRequest: ApiRequest,
        onResponse: () -> Unit
    ): StripeResponse<String> {
        val dnsCacheData = disableDnsCache()

        val response = runCatching {
            stripeNetworkClient.executeRequest(apiRequest)
        }.also {
            onResponse()
        }.getOrElse {
            throw when (it) {
                is IOException -> APIConnectionException.create(it, apiRequest.baseUrl)
                else -> it
            }
        }

        if (response.isError) {
            handleApiError(response)
        }

        resetDnsCache(dnsCacheData)

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
    internal suspend fun makeFileUploadRequest(
        fileUploadRequest: FileUploadRequest,
        onResponse: (RequestId?) -> Unit
    ): StripeResponse<String> {
        val dnsCacheData = disableDnsCache()

        val response = runCatching {
            stripeNetworkClient.executeRequest(fileUploadRequest)
        }.also {
            onResponse(it.getOrNull()?.requestId)
        }.getOrElse {
            throw when (it) {
                is IOException -> APIConnectionException.create(it, fileUploadRequest.url)
                else -> it
            }
        }

        if (response.isError) {
            handleApiError(response)
        }

        resetDnsCache(dnsCacheData)

        return response
    }

    private fun disableDnsCache(): DnsCacheData {
        return runCatching {
            val originalDnsCacheTtl = Security.getProperty(DNS_CACHE_TTL_PROPERTY_NAME)
            // disable DNS cache
            Security.setProperty(DNS_CACHE_TTL_PROPERTY_NAME, "0")
            DnsCacheData.Success(originalDnsCacheTtl)
        }.getOrDefault(
            DnsCacheData.Failure
        )
    }

    private fun resetDnsCache(dnsCacheData: DnsCacheData) {
        if (dnsCacheData is DnsCacheData.Success) {
            // value unspecified by implementation
            // DNS_CACHE_TTL_PROPERTY_NAME of -1 = cache forever
            Security.setProperty(
                DNS_CACHE_TTL_PROPERTY_NAME,
                dnsCacheData.originalDnsCacheTtl ?: "-1"
            )
        }
    }

    private fun fireFraudDetectionDataRequest() {
        fraudDetectionDataRepository.refresh()
    }

    private fun fireAnalyticsRequest(
        event: PaymentAnalyticsEvent
    ) {
        fireAnalyticsRequest(
            paymentAnalyticsRequestFactory.createRequest(event)
        )
    }

    @VisibleForTesting
    internal fun fireAnalyticsRequest(
        params: AnalyticsRequest
    ) {
        analyticsRequestExecutor.executeAsync(params)
    }

    private fun createClientSecretParam(
        clientSecret: String,
        expandFields: List<String>
    ): Map<String, Any> {
        return mapOf("client_secret" to clientSecret)
            .plus(createExpandParam(expandFields))
    }

    private fun buildPaymentUserAgentPair(attribution: Set<String> = emptySet()) =
        PAYMENT_USER_AGENT to buildPaymentUserAgent(attribution)

    override fun buildPaymentUserAgent(attribution: Set<String>): String {
        return setOf("stripe-android/${StripeSdkVersion.VERSION_NAME}")
            .plus(productUsageTokens)
            .plus(attribution)
            .joinToString(";")
    }

    /**
     *  Add payment_user_agent to the map if it contains Payment Method data,
     *  including attribution from [paymentMethodCreateParams] or [sourceParams].
     */
    private fun maybeAddPaymentUserAgent(
        params: Map<String, Any>,
        paymentMethodCreateParams: PaymentMethodCreateParams?,
        sourceParams: SourceParams? = null
    ): Map<String, Any> =
        (params[ConfirmStripeIntentParams.PARAM_PAYMENT_METHOD_DATA] as? Map<*, *>)?.let {
            params.plus(
                ConfirmStripeIntentParams.PARAM_PAYMENT_METHOD_DATA to it.plus(
                    buildPaymentUserAgentPair(paymentMethodCreateParams?.attribution ?: emptySet())
                )
            )
        } ?: (params[ConfirmPaymentIntentParams.PARAM_SOURCE_DATA] as? Map<*, *>)?.let {
            params.plus(
                ConfirmPaymentIntentParams.PARAM_SOURCE_DATA to it.plus(
                    buildPaymentUserAgentPair(sourceParams?.attribution ?: emptySet())
                )
            )
        } ?: params

    private suspend fun ConfirmPaymentIntentParams.maybeForDashboard(
        options: ApiRequest.Options
    ): Result<ConfirmPaymentIntentParams> {
        if (!options.apiKeyIsUserKey || paymentMethodCreateParams == null) {
            return Result.success(this)
        }

        // For user key auth, we must create the PM first.
        val paymentMethodResult = createPaymentMethod(
            paymentMethodCreateParams = paymentMethodCreateParams,
            options = options,
        )

        return paymentMethodResult.mapCatching { paymentMethod ->
            ConfirmPaymentIntentParams.createForDashboard(
                clientSecret = clientSecret,
                paymentMethodId = paymentMethod.id!!,
                paymentMethodOptions = paymentMethodOptions,
            )
        }
    }

    private sealed class DnsCacheData {
        data class Success(
            val originalDnsCacheTtl: String?
        ) : DnsCacheData()

        object Failure : DnsCacheData()
    }

    internal companion object {
        private const val DNS_CACHE_TTL_PROPERTY_NAME = "networkaddress.cache.ttl"
        private const val PAYMENT_USER_AGENT = "payment_user_agent"

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
         * @return `https://api.stripe.com/v1/consumers/accounts/sign_up`
         */
        internal val consumerSignUpUrl: String
            @JvmSynthetic
            get() = getApiUrl("consumers/accounts/sign_up")

        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/log_out`
         */
        internal val logoutConsumerUrl: String
            @JvmSynthetic
            get() = getApiUrl("consumers/sessions/log_out")

        /**
         * @return `https://api.stripe.com/v1/consumers/payment_details`
         */
        internal val consumerPaymentDetailsUrl: String
            @JvmSynthetic
            get() = getApiUrl("consumers/payment_details")

        /**
         * @return `https://api.stripe.com/v1/consumers/payment_details/list`
         */
        internal val listConsumerPaymentDetailsUrl: String
            @JvmSynthetic
            get() = getApiUrl("consumers/payment_details/list")

        /**
         * @return `https://api.stripe.com/v1/consumers/link_account_sessions`
         */
        internal val linkFinancialConnectionsSessionUrl: String
            @JvmSynthetic
            get() = getApiUrl("consumers/link_account_sessions")

        /**
         * @return `https://api.stripe.com/v1/connections/link_account_sessions_for_deferred_payment`
         */
        internal val deferredFinancialConnectionsSessionUrl: String
            @JvmSynthetic
            get() = getApiUrl("connections/link_account_sessions_for_deferred_payment")

        /**
         * @return `https://api.stripe.com/v1/consumers/payment_details/:id`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getConsumerPaymentDetailsUrl(paymentDetailsId: String): String {
            return getApiUrl("consumers/payment_details/$paymentDetailsId")
        }

        /**
         * @return `https://api.stripe.com/v1/payment_intents/:id`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getRetrievePaymentIntentUrl(paymentIntentId: String): String {
            return getApiUrl("payment_intents/%s", paymentIntentId)
        }

        /**
         * This is an undocumented API and is only used for certain PIs which have a delay to
         * transfer its status out of "requires_action" after user performs the confirmation.
         *
         * @return `https://api.stripe.com/v1/payment_intents/:id/refresh`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getRefreshPaymentIntentUrl(paymentIntentId: String): String {
            return getApiUrl("payment_intents/%s/refresh", paymentIntentId)
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
         * @return `https://api.stripe.com/v1/payment_intents/:paymentIntentId/
         * link_account_sessions/:financialConnectionsSessionId/attach`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getAttachFinancialConnectionsSessionToPaymentIntentUrl(
            paymentIntentId: String,
            financialConnectionsSessionId: String
        ): String {
            return getApiUrl(
                "payment_intents/%s/link_account_sessions/%s/attach",
                paymentIntentId,
                financialConnectionsSessionId
            )
        }

        /**
         * @return `https://api.stripe.com/v1/setup_intents/:setupIntentId/
         * link_account_sessions/:financialConnectionsSessionId/attach`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getAttachFinancialConnectionsSessionToSetupIntentUrl(
            setupIntentId: String,
            financialConnectionsSessionId: String
        ): String {
            return getApiUrl(
                "setup_intents/%s/link_account_sessions/%s/attach",
                setupIntentId,
                financialConnectionsSessionId
            )
        }

        /**
         * @return `https://api.stripe.com/v1/payment_intents/:clientSecret/verify_microdeposits`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getVerifyMicrodepositsOnPaymentIntentUrl(
            clientSecret: String
        ): String {
            return getApiUrl(
                "payment_intents/%s/verify_microdeposits",
                clientSecret
            )
        }

        /**
         * @return `https://api.stripe.com/v1/setup_intents/:clientSecret/verify_microdeposits`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getVerifyMicrodepositsOnSetupIntentUrl(
            clientSecret: String
        ): String {
            return getApiUrl(
                "setup_intents/%s/verify_microdeposits",
                clientSecret
            )
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
