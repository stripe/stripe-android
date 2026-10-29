package com.stripe.android.networking

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.DefaultFraudDetectionDataRepository
import com.stripe.android.Stripe
import com.stripe.android.StripeApiBeta
import com.stripe.android.cards.Bin
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.CardNumber
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.core.ApiConfiguration
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
import com.stripe.android.core.exception.safeAnalyticsMessage
import com.stripe.android.core.frauddetection.FraudDetectionData
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import com.stripe.android.core.injection.IOContext
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
import com.stripe.android.core.networking.RequestCredentials
import com.stripe.android.core.networking.RequestId
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.networking.responseJson
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.exception.CardException
import com.stripe.android.model.BankStatuses
import com.stripe.android.model.CancelCaptchaChallengeParams
import com.stripe.android.model.CardMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_CLIENT_SECRET
import com.stripe.android.model.ConfirmationToken
import com.stripe.android.model.ConfirmationTokenParams
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerShippingAddresses
import com.stripe.android.model.CreateFinancialConnectionsSessionForDeferredPaymentParams
import com.stripe.android.model.CreateFinancialConnectionsSessionParams
import com.stripe.android.model.Customer
import com.stripe.android.model.FinancialConnectionsSession
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.MobileCardElementConfig
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.model.PaymentMethodMessagePromotionList
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.model.RadarSessionWithHCaptcha
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
import com.stripe.android.model.parsers.ConfirmationTokenJsonParser
import com.stripe.android.model.parsers.ConsumerPaymentDetailsJsonParser
import com.stripe.android.model.parsers.ConsumerPaymentDetailsShareJsonParser
import com.stripe.android.model.parsers.ConsumerSessionJsonParser
import com.stripe.android.model.parsers.ConsumerShippingAddressesParser
import com.stripe.android.model.parsers.CustomerJsonParser
import com.stripe.android.model.parsers.FinancialConnectionsSessionJsonParser
import com.stripe.android.model.parsers.FpxBankStatusesJsonParser
import com.stripe.android.model.parsers.IssuingCardPinJsonParser
import com.stripe.android.model.parsers.MobileCardElementConfigParser
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.model.parsers.PaymentMethodMessageJsonParser
import com.stripe.android.model.parsers.PaymentMethodMessagePromotionJsonParser
import com.stripe.android.model.parsers.PaymentMethodsListJsonParser
import com.stripe.android.model.parsers.RadarSessionWithHCaptchaJsonParser
import com.stripe.android.model.parsers.SetupIntentJsonParser
import com.stripe.android.model.parsers.SourceJsonParser
import com.stripe.android.model.parsers.Stripe3ds2AuthResultJsonParser
import com.stripe.android.model.parsers.TokenJsonParser
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.utils.StripeUrlUtils
import com.stripe.android.utils.mapResult
import kotlinx.coroutines.Dispatchers
import java.io.IOException
import java.net.HttpURLConnection
import java.security.Security
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

/**
 * An implementation of [StripeRepository] that makes network requests to the Stripe API.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class StripeApiRepository @JvmOverloads internal constructor(
    private val context: Context,
    private val publishableKeyProvider: () -> String,
    private val requestSurface: RequestSurface,
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
        DefaultFraudDetectionDataRepository(
            context = context,
            apiConfigurationProvider = {
                ApiConfiguration.State(
                    publishableKey = publishableKeyProvider(),
                    stripeAccountId = null,
                    betas = emptySet(),
                )
            },
            workContext = workContext,
        ),
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory =
        DefaultCardAccountRangeRepositoryFactory(
            context = context,
            productUsageTokens = productUsageTokens,
            requestSurface = requestSurface,
            analyticsRequestExecutor = analyticsRequestExecutor,
            apiConfigurationProvider = {
                ApiConfiguration.State(
                    publishableKey = publishableKeyProvider(),
                    stripeAccountId = null,
                    betas = emptySet(),
                )
            },
        ),
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory =
        PaymentAnalyticsRequestFactory(context, publishableKeyProvider, productUsageTokens),
    betas: Set<StripeApiBeta> = emptySet(),
    private val apiVersion: String = ApiVersion(betas = betas.map { it.code }.toSet()).code,
    private val sdkVersion: String = StripeSdkVersion.VERSION,
    private val requestApiConfiguration: ApiConfiguration.State?,
    private val initializeRequestServices: Boolean,
) : StripeRepository {

    @Inject
    constructor(
        appContext: Context,
        apiConfigurationProvider: Provider<ApiConfiguration.State>,
        requestSurface: RequestSurface,
        @IOContext workContext: CoroutineContext,
        @Named(PRODUCT_USAGE) productUsageTokens: Set<String>,
        paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        logger: Logger,
        fraudDetectionDataRepository: FraudDetectionDataRepository,
    ) : this(
        context = appContext,
        publishableKeyProvider = { apiConfigurationProvider.get().publishableKey },
        requestSurface = requestSurface,
        logger = logger,
        workContext = workContext,
        productUsageTokens = productUsageTokens,
        fraudDetectionDataRepository = fraudDetectionDataRepository,
        cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(
            context = appContext,
            productUsageTokens = productUsageTokens,
            requestSurface = requestSurface,
            analyticsRequestExecutor = analyticsRequestExecutor,
            apiConfigurationProvider = apiConfigurationProvider,
        ),
        paymentAnalyticsRequestFactory = paymentAnalyticsRequestFactory,
        analyticsRequestExecutor = analyticsRequestExecutor,
        requestApiConfiguration = null,
        initializeRequestServices = true,
    )

    override fun withApiConfiguration(apiConfiguration: ApiConfiguration.State): StripeRepository {
        return StripeApiRepository(
            context = context,
            publishableKeyProvider = publishableKeyProvider,
            requestSurface = requestSurface,
            appInfo = appInfo,
            logger = logger,
            workContext = workContext,
            productUsageTokens = productUsageTokens,
            stripeNetworkClient = stripeNetworkClient,
            analyticsRequestExecutor = analyticsRequestExecutor,
            fraudDetectionDataRepository = fraudDetectionDataRepository,
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            paymentAnalyticsRequestFactory = paymentAnalyticsRequestFactory,
            apiVersion = apiVersion,
            sdkVersion = sdkVersion,
            requestApiConfiguration = apiConfiguration.copy(betas = apiConfiguration.betas.toSet()),
            initializeRequestServices = false,
        )
    }

    private val apiRequestFactory = ApiRequest.Factory(
        appInfo = appInfo,
        apiVersion = apiVersion,
        sdkVersion = sdkVersion
    )

    private val fraudDetectionData: FraudDetectionData?
        get() = fraudDetectionDataRepository.getCached()

    init {
        if (initializeRequestServices) fireFraudDetectionDataRequest()
    }

    override suspend fun retrieveStripeIntent(
        clientSecret: String,
        options: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<StripeIntent> {
        return when {
            PaymentIntent.ClientSecret.isMatch(clientSecret) -> {
                retrievePaymentIntent(clientSecret, options, expandFields, idempotencyKey = idempotencyKey)
            }
            SetupIntent.ClientSecret.isMatch(clientSecret) -> {
                retrieveSetupIntent(clientSecret, options, expandFields, idempotencyKey = idempotencyKey)
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
        options: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        return confirmPaymentIntentParams.maybeForDashboard(options, idempotencyKey = idempotencyKey).mapResult {
            confirmPaymentIntentInternal(
                confirmPaymentIntentParams = it,
                options = options,
                expandFields = expandFields,
                idempotencyKey = idempotencyKey,
            )
        }
    }

    private suspend fun confirmPaymentIntentInternal(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        options: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        val params = ConfirmationParamsUtils.prepareConfirmationParams(
            confirmPaymentIntentParams.toParamMap()
                // Omit client_secret with user key auth.
                .let { if (options.apiKeyIsUserKey) it.minus(PARAM_CLIENT_SECRET) else it }
                .plus(createExpandParam(expandFields)),
            confirmPaymentIntentParams.paymentMethodCreateParams,
            sourceParams = confirmPaymentIntentParams.sourceParams,
            fraudDetectionData = fraudDetectionData,
            productUsageTokens = productUsageTokens,
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
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = PaymentIntentJsonParser(),
        ) { result ->
            val paymentMethodType =
                confirmPaymentIntentParams.paymentMethodCreateParams?.typeCode
                    ?: confirmPaymentIntentParams.sourceParams?.type

            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createPaymentIntentConfirmation(
                    paymentMethodType = paymentMethodType,
                    errorMessage = result.errorMessage(options),
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
        options: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
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
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        options: RequestCredentials,
        idempotencyKey: String?,
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
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = PaymentIntentJsonParser(),
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.PaymentIntentRefresh)
            )
        }
    }

    /**
     * Refresh a [SetupIntent] using its client_secret
     *
     * Analytics event: [PaymentAnalyticsEvent.SetupIntentRefresh]
     *
     * @param clientSecret client_secret of the SetupIntent to retrieve
     */
    override suspend fun refreshSetupIntent(
        clientSecret: String,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        val setupIntentId = runCatching {
            SetupIntent.ClientSecret(clientSecret).setupIntentId
        }.getOrElse {
            return Result.failure(it)
        }

        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getRefreshSetupIntentUrl(setupIntentId),
                options = options,
                params = createClientSecretParam(clientSecret, emptyList()),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = SetupIntentJsonParser(),
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.SetupIntentRefresh)
            )
        }
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.PaymentIntentCancelSource]
     */
    override suspend fun cancelPaymentIntentSource(
        paymentIntentId: String,
        sourceId: String,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getCancelPaymentIntentSourceUrl(paymentIntentId),
                options = options,
                params = mapOf("source" to sourceId),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        options: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        return confirmSetupIntentParams.maybeForDashboard(options, idempotencyKey = idempotencyKey).mapResult {
            confirmSetupIntentInternal(
                confirmSetupIntentParams = it,
                options = options,
                expandFields = expandFields,
                idempotencyKey = idempotencyKey,
            )
        }
    }

    private suspend fun confirmSetupIntentInternal(
        confirmSetupIntentParams: ConfirmSetupIntentParams,
        options: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
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
                ConfirmationParamsUtils.prepareConfirmationParams(
                    confirmSetupIntentParams.toParamMap()
                        // Omit client_secret with user key auth.
                        .let { if (options.apiKeyIsUserKey) it.minus(PARAM_CLIENT_SECRET) else it }
                        .plus(createExpandParam(expandFields)),
                    confirmSetupIntentParams.paymentMethodCreateParams,
                    sourceParams = null,
                    fraudDetectionData = fraudDetectionData,
                    productUsageTokens = productUsageTokens,
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            SetupIntentJsonParser()
        ) { result ->
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createSetupIntentConfirmation(
                    paymentMethodType = confirmSetupIntentParams.paymentMethodCreateParams?.typeCode,
                    errorMessage = result.errorMessage(options),
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
        options: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        val setupIntentId = runCatching {
            SetupIntent.ClientSecret(clientSecret).setupIntentId
        }.getOrElse {
            return Result.failure(it)
        }
        val params: Map<String, Any?> =
            if (options.apiKeyIsUserKey) {
                createExpandParam(expandFields)
            } else {
                createClientSecretParam(clientSecret, expandFields)
            }

        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = getRetrieveSetupIntentUrl(setupIntentId),
                options = options,
                params = params,
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                getCancelSetupIntentSourceUrl(setupIntentId),
                options,
                mapOf("source" to sourceId),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Source> {
        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                sourcesUrl,
                options,
                sourceParams.toParamMap()
                    .plus(buildPaymentUserAgentPair(sourceParams.attribution))
                    .plus(fraudDetectionData?.params.orEmpty()),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Source> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = getRetrieveSourceApiUrl(sourceId),
                options = options,
                params = SourceParams.createRetrieveSourceParams(clientSecret),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                paymentMethodsUrl,
                options,
                paymentMethodCreateParams.toParamMap()
                    .plus(buildPaymentUserAgentPair(paymentMethodCreateParams.attribution))
                    .plus(fraudDetectionData?.params.orEmpty()),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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

    override suspend fun updatePaymentMethod(
        paymentMethodId: String,
        paymentMethodUpdateParams: PaymentMethodUpdateParams,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getPaymentMethodUrl(paymentMethodId),
                options = options,
                params = paymentMethodUpdateParams.toParamMap(),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = PaymentMethodJsonParser()
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createPaymentMethodUpdate(
                    paymentMethodCode = paymentMethodUpdateParams.type.code,
                    productUsageTokens = paymentMethodUpdateParams.productUsageTokens,
                )
            )
        }
    }

    override suspend fun setDefaultPaymentMethod(
        customerId: String,
        paymentMethodId: String?,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Customer> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getSetDefaultPaymentMethodUrl(customerId = customerId),
                options = options,
                params = mapOf("payment_method" to (paymentMethodId ?: "")),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = CustomerJsonParser()
        )
    }

    override suspend fun retrieveSavedPaymentMethodFromCardPresentPaymentMethod(
        cardPresentPaymentMethodId: String,
        customerId: String,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = getSavedPaymentMethodFromCardPresentPaymentMethod(
                    customerId = customerId,
                    paymentMethodId = cardPresentPaymentMethodId,
                ),
                options = options,
                params = emptyMap<String, String>(),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = PaymentMethodJsonParser()
        )
    }

    /**
     * Create a [Token] using the input token parameters.
     *
     * Analytics event: [PaymentAnalyticsEvent.TokenCreate]
     *
     * @param tokenParams a [TokenParams] representing the object for which this token is being created
     * @param options a [RequestCredentials] object that contains connection data like the api
     * key, api version, etc
     *
     * @return a [Result] containing the generated [Token] or the encountered [Exception]
     */
    override suspend fun createToken(
        tokenParams: TokenParams,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Token> {
        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                tokensUrl,
                options,
                tokenParams.toParamMap()
                    .plus(buildPaymentUserAgentPair(tokenParams.attribution))
                    .plus(fraudDetectionData?.params.orEmpty()),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
     * Analytics event: [PaymentAnalyticsEvent.ConfirmationTokenCreate]
     */
    override suspend fun createConfirmationToken(
        confirmationTokenParams: ConfirmationTokenParams,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConfirmationToken> {
        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                confirmationTokensUrl,
                options,
                ConfirmationParamsUtils.prepareConfirmationParams(
                    confirmationTokenParams.toParamMap(),
                    confirmationTokenParams.paymentMethodData,
                    sourceParams = null,
                    fraudDetectionData = fraudDetectionData,
                    productUsageTokens = productUsageTokens,
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            ConfirmationTokenJsonParser()
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(
                    PaymentAnalyticsEvent.ConfirmationTokenCreate,
                    productUsageTokens = confirmationTokenParams.paymentMethodData?.attribution ?: emptySet(),
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
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Source> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getAddCustomerSourceUrl(customerId),
                options = requestOptions,
                params = mapOf("source" to sourceId),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Source> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createDelete(
                url = getDeleteCustomerSourceUrl(customerId, sourceId),
                options = requestOptions,
                apiConfiguration = requestApiConfiguration,
                params = null,
                idempotencyKey = idempotencyKey,
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
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        fireFraudDetectionDataRequest()

        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getAttachPaymentMethodUrl(paymentMethodId),
                options = requestOptions,
                params = mapOf("customer" to customerId),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getDetachPaymentMethodUrl(paymentMethodId),
                options = requestOptions,
                apiConfiguration = requestApiConfiguration,
                params = null,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = PaymentMethodJsonParser()
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createDetachPaymentMethod(productUsageTokens)
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
        customerSessionClientSecret: String,
        productUsageTokens: Set<String>,
        paymentMethodId: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getElementsDetachPaymentMethodUrl(paymentMethodId),
                options = requestOptions,
                params = mapOf("customer_session_client_secret" to customerSessionClientSecret),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        productUsageTokens: Set<String>,
        requestOptions: RequestCredentials,
        apiConfiguration: ApiConfiguration.State,
        idempotencyKey: String?,
    ): Result<List<PaymentMethod>> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = paymentMethodsUrl,
                options = requestOptions,
                params = listPaymentMethodsParams.toParamMap(),
                apiConfiguration = apiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = PaymentMethodsListJsonParser(),
            onResponse = {
                fireAnalyticsRequest(
                    paymentAnalyticsRequestFactory.createRequest(
                        PaymentAnalyticsEvent.CustomerRetrievePaymentMethods,
                        productUsageTokens = productUsageTokens,
                        publishableKeyOverride = apiConfiguration.publishableKey
                    )
                )
            },
        ).map {
            it.paymentMethods
        }
    }

    /**
     * Retrieve a single [PaymentMethod] for a customer.
     *
     * Analytics event: [PaymentAnalyticsEvent.CustomerRetrievePaymentMethod]
     */
    override suspend fun retrieveCustomerPaymentMethod(
        customerId: String,
        paymentMethodId: String,
        productUsageTokens: Set<String>,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = getRetrieveCustomerPaymentMethodUrl(customerId, paymentMethodId),
                options = requestOptions,
                apiConfiguration = requestApiConfiguration,
                params = null,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = PaymentMethodJsonParser(),
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(
                    PaymentAnalyticsEvent.CustomerRetrievePaymentMethod,
                    productUsageTokens = productUsageTokens
                )
            )
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
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Customer> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getRetrieveCustomerUrl(customerId),
                options = requestOptions,
                params = mapOf("default_source" to sourceId),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Customer> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getRetrieveCustomerUrl(customerId),
                options = requestOptions,
                params = mapOf("shipping" to shippingInformation.toParamMap()),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Customer> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = getRetrieveCustomerUrl(customerId),
                options = requestOptions,
                apiConfiguration = requestApiConfiguration,
                params = null,
                idempotencyKey = idempotencyKey,
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
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<String> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = getIssuingCardPinUrl(cardId),
                options = requestOptions,
                params = mapOf(
                    "verification" to createVerificationParam(verificationId, userOneTimeCode)
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
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
                    apiConfiguration = requestApiConfiguration,
                    idempotencyKey = idempotencyKey,
                ),
                onResponse = { fireAnalyticsRequest(PaymentAnalyticsEvent.IssuingUpdatePin) },
            )
        }.exceptionOrNull()
    }

    override suspend fun getFpxBankStatus(
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<BankStatuses> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = getApiUrl("fpx/bank_statuses"),
                // don't pass connected account
                options = options.copy(stripeAccount = null),
                params = mapOf("account_holder_type" to "individual"),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = FpxBankStatusesJsonParser(),
        ) {
            fireAnalyticsRequest(PaymentAnalyticsEvent.FpxBankStatusesRetrieve)
        }
    }

    override suspend fun getCardMetadata(
        bin: Bin,
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<CardMetadata> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createGet(
                url = getEdgeUrl("card-metadata"),
                options = options.copy(stripeAccount = null),
                params = mapOf("key" to options.apiKey, "bin_prefix" to bin.value),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = CardMetadataJsonParser(bin, isNetwork = true),
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
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Stripe3ds2AuthResult> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getApiUrl("3ds2/authenticate"),
                options = requestOptions,
                params = authParams.toParamMap(),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Stripe3ds2AuthResult> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getApiUrl("3ds2/challenge_complete"),
                options = requestOptions,
                params = mapOf("source" to sourceId),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = Stripe3ds2AuthResultJsonParser(),
        )
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.FileCreate]
     */
    override suspend fun createFile(
        fileParams: StripeFileParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<StripeFile> {
        val response = runCatching {
            makeFileUploadRequest(
                apiRequestOptions = requestOptions,
                fileUploadRequest = FileUploadRequest(
                    fileParams,
                    requestOptions,
                    appInfo,
                    apiConfiguration = requestApiConfiguration,
                    idempotencyKey = idempotencyKey,
                ),
                onResponse = { fireAnalyticsRequest(PaymentAnalyticsEvent.FileCreate) },
            )
        }

        return response.mapCatching {
            StripeFileJsonParser().parse(it.responseJson())
        }
    }

    override suspend fun retrieveObject(
        url: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<StripeResponse<String>> {
        if (!StripeUrlUtils.isStripeUrl(url)) {
            return Result.failure(IllegalArgumentException("Unrecognized domain: $url"))
        }

        return runCatching {
            makeApiRequest(
                apiRequest = apiRequestFactory.createGet(
                    url,
                    requestOptions,
                    apiConfiguration = requestApiConfiguration,
                    params = null,
                    idempotencyKey = idempotencyKey,
                ),
                onResponse = { fireAnalyticsRequest(PaymentAnalyticsEvent.StripeUrlRetrieve) },
            )
        }
    }

    /**
     * Get the latest [FraudDetectionData] from [FraudDetectionDataRepository] and send in POST request
     * to `/v1/radar/session`.
     */
    override suspend fun createRadarSession(
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<RadarSessionWithHCaptcha> {
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
                    apiConfiguration = requestApiConfiguration,
                    idempotencyKey = idempotencyKey,
                ),
                jsonParser = RadarSessionWithHCaptchaJsonParser(),
            ) {
                fireAnalyticsRequest(
                    paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.RadarSessionCreate)
                )
            }
        }.getOrElse {
            Result.failure(StripeException.create(it))
        }
    }

    override suspend fun attachHCaptchaToRadarSession(
        radarSessionToken: String,
        hcaptchaToken: String,
        hcaptchaEKey: String?,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<RadarSessionWithHCaptcha> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getApiUrl("radar/session/%s/attach_hcaptcha_token", radarSessionToken),
                options = requestOptions,
                params = mapOf(
                    "passive_captcha_token" to hcaptchaToken
                ).plus(
                    hcaptchaEKey?.let {
                        mapOf(
                            "passive_captcha_ekey" to hcaptchaEKey
                        )
                    } ?: emptyMap()
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = RadarSessionWithHCaptchaJsonParser(),
        )
    }

    override suspend fun sharePaymentDetails(
        consumerSessionClientSecret: String,
        id: String,
        extraParams: Map<String, *>?,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethod> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = sharePaymentDetailsUrl,
                options = requestOptions,
                params = mapOf(
                    "request_surface" to requestSurface.value,
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                    "id" to id,
                    buildPaymentUserAgentPair(),
                ).plus(extraParams ?: emptyMap()),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = ConsumerPaymentDetailsShareJsonParser,
        ).map { it.paymentMethod }
    }

    override suspend fun logOut(
        consumerSessionClientSecret: String,
        consumerAccountPublishableKey: String?,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerSession> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = logoutConsumerUrl,
                options = requestOptions,
                params = mapOf(
                    "request_surface" to requestSurface.value,
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = ConsumerSessionJsonParser(),
        )
    }

    override suspend fun createFinancialConnectionsSessionForDeferredPayments(
        params: CreateFinancialConnectionsSessionForDeferredPaymentParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<FinancialConnectionsSession> {
        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                url = deferredFinancialConnectionsSessionUrl,
                options = requestOptions,
                params = params.toMap(),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            FinancialConnectionsSessionJsonParser()
        ) {
            // no-op
        }
    }

    override suspend fun createPaymentIntentFinancialConnectionsSession(
        paymentIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<FinancialConnectionsSession> {
        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                url = getPaymentIntentFinancialConnectionsSessionUrl(paymentIntentId),
                options = requestOptions,
                params = params.toMap(),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            FinancialConnectionsSessionJsonParser()
        ) {
            // no-op
        }
    }

    override suspend fun createSetupIntentFinancialConnectionsSession(
        setupIntentId: String,
        params: CreateFinancialConnectionsSessionParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<FinancialConnectionsSession> {
        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                url = getSetupIntentFinancialConnectionsSessionUrl(setupIntentId),
                options = requestOptions,
                params = params.toMap(),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        requestOptions: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                getAttachFinancialConnectionsSessionToPaymentIntentUrl(
                    paymentIntentId,
                    financialConnectionsSessionId
                ),
                requestOptions,
                mapOf("client_secret" to clientSecret)
                    .plus(createExpandParam(expandFields)),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        requestOptions: RequestCredentials,
        expandFields: List<String>,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                getAttachFinancialConnectionsSessionToSetupIntentUrl(
                    setupIntentId,
                    financialConnectionsSessionId
                ),
                requestOptions,
                mapOf("client_secret" to clientSecret)
                    .plus(createExpandParam(expandFields)),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
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
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
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
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
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
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
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
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
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
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            SetupIntentJsonParser()
        )
    }

    override suspend fun retrievePaymentMethodMessage(
        paymentMethods: List<String>,
        amount: Int,
        currency: String,
        country: String?,
        locale: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethodMessage> {
        return fetchStripeModelResult(
            apiRequestFactory.createGet(
                url = "https://ppm.stripe.com/config",
                options = requestOptions,
                params = mapOf<String, Any?>(
                    "amount" to amount,
                    "country" to country,
                    "currency" to currency,
                    "locale" to locale,
                    "key" to requestOptions.apiKey,
                    "_stripe_account" to requestOptions.stripeAccount
) + paymentMethods.mapIndexed { index, paymentMethod ->
                    "payment_methods[$index]" to paymentMethod
                },
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            PaymentMethodMessageJsonParser()
        ) {
            // no-op
        }
    }

    override suspend fun retrievePaymentMethodMessagePromotionsForPaymentSheet(
        amount: Int,
        currency: String,
        country: String?,
        locale: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentMethodMessagePromotionList> {
        return fetchStripeModelResult(
            apiRequestFactory.createGet(
                url = "https://ppm.stripe.com/config",
                options = requestOptions,
                params = mapOf<String, Any?>(
                    "amount" to amount,
                    "country" to country,
                    "currency" to currency,
                    "locale" to locale,
                    "key" to requestOptions.apiKey,
                    "_stripe_account" to requestOptions.stripeAccount
),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            PaymentMethodMessagePromotionJsonParser()
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

    /**
     * @return `https://api.stripe.com/v1/payment_methods/:id/detach`
     */
    @VisibleForTesting
    internal fun getElementsDetachPaymentMethodUrl(paymentMethodId: String): String {
        return getApiUrl("elements/payment_methods/%s/detach", paymentMethodId)
    }

    override suspend fun retrieveCardMetadata(
        cardNumber: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<CardMetadata> {
        val unvalidatedNumber = CardNumber.Unvalidated(cardNumber)

        val bin = unvalidatedNumber.bin ?: return Result.failure(
            InvalidRequestException(
                message = "cardNumber cannot be less than 6 characters",
            )
        )

        val cardAccountRangeRepository = if (
            cardAccountRangeRepositoryFactory is DefaultCardAccountRangeRepositoryFactory
        ) {
            cardAccountRangeRepositoryFactory.createWithStripeRepository(
                stripeRepository = this,
                publishableKey = publishableKeyProvider(),
                betas = requestApiConfiguration?.betas.orEmpty(),
            )
        } else {
            cardAccountRangeRepositoryFactory.createWithStripeRepository(
                stripeRepository = this,
                publishableKey = publishableKeyProvider(),
            )
        }

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

    override suspend fun retrieveCardElementConfig(
        requestOptions: RequestCredentials,
        params: Map<String, String>?,
        idempotencyKey: String?,
    ): Result<MobileCardElementConfig> {
        return fetchStripeModelResult(
            apiRequestFactory.createGet(
                url = mobileCardElementConfigUrl,
                options = requestOptions,
                params = params,
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = MobileCardElementConfigParser(),
        )
    }

    override suspend fun listPaymentDetails(
        clientSecret: String,
        paymentMethodTypes: Set<String>,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerPaymentDetails> {
        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                listConsumerPaymentDetailsUrl,
                requestOptions,
                mapOf(
                    "request_surface" to requestSurface.value,
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to clientSecret
                    ),
                    "types" to paymentMethodTypes.toList()
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            ConsumerPaymentDetailsJsonParser
        )
    }

    override suspend fun listShippingAddresses(
        clientSecret: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerShippingAddresses> {
        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                listShippingAddresses,
                requestOptions,
                mapOf(
                    "request_surface" to requestSurface.value,
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to clientSecret
                    ),
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            ConsumerShippingAddressesParser
        )
    }

    override suspend fun deletePaymentDetails(
        clientSecret: String,
        paymentDetailsId: String,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<Unit> {
        return runCatching {
            makeApiRequest(
                apiRequestFactory.createDelete(
                    getConsumerPaymentDetailsUrl(paymentDetailsId),
                    requestOptions,
                    mapOf(
                        "request_surface" to requestSurface.value,
                        "credentials" to mapOf(
                            "consumer_session_client_secret" to clientSecret
                        )
                    ),
                    apiConfiguration = requestApiConfiguration,
                    idempotencyKey = idempotencyKey,
                ),
                onResponse = {}
            )
        }
    }

    override suspend fun updatePaymentDetails(
        clientSecret: String,
        paymentDetailsUpdateParams: ConsumerPaymentDetailsUpdateParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConsumerPaymentDetails> {
        return fetchStripeModelResult(
            apiRequestFactory.createPost(
                getConsumerPaymentDetailsUrl(paymentDetailsUpdateParams.id),
                requestOptions,
                mapOf(
                    "request_surface" to requestSurface.value,
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to clientSecret
                    )
                ).plus(
                    paymentDetailsUpdateParams.toParamMap()
                ),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            ConsumerPaymentDetailsJsonParser
        )
    }

    override suspend fun cancelPaymentIntentCaptchaChallenge(
        paymentIntentId: String,
        params: CancelCaptchaChallengeParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<PaymentIntent> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getCancelPaymentIntentCaptchaChallengeUrl(paymentIntentId),
                options = requestOptions,
                params = params.toParamMap(),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = PaymentIntentJsonParser(),
        )
    }

    override suspend fun cancelSetupIntentCaptchaChallenge(
        setupIntentId: String,
        params: CancelCaptchaChallengeParams,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): Result<SetupIntent> {
        return fetchStripeModelResult(
            apiRequest = apiRequestFactory.createPost(
                url = getCancelSetupIntentCaptchaChallengeUrl(setupIntentId),
                options = requestOptions,
                params = params.toParamMap(),
                apiConfiguration = requestApiConfiguration,
                idempotencyKey = idempotencyKey,
            ),
            jsonParser = SetupIntentJsonParser(),
        )
    }

    @Throws(
        InvalidRequestException::class,
        AuthenticationException::class,
        CardException::class,
        APIException::class
    )
    private fun handleApiError(
        requestOptions: RequestCredentials,
        response: StripeResponse<String>,
    ) {
        val requestId = response.requestId?.value
        val responseCode = response.code

        val stripeError = StripeErrorJsonParser()
            .parse(response.responseJson())
            .withLocalizedMessage(context, requestId, requestOptions.apiKeyIsLiveMode)

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
        onResponse: (result: Result<StripeResponse<String>>) -> Unit = {},
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
        onResponse: (result: Result<StripeResponse<String>>) -> Unit
    ): StripeResponse<String> {
        val dnsCacheData = disableDnsCache()

        val response = runCatching {
            stripeNetworkClient.executeRequest(apiRequest)
        }.also {
            onResponse(it)
        }.getOrElse {
            throw when (it) {
                is IOException -> APIConnectionException.create(it, apiRequest.baseUrl)
                else -> it
            }
        }

        if (response.isError) {
            handleApiError(apiRequest.options, response,)
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
        apiRequestOptions: RequestCredentials,
        fileUploadRequest: FileUploadRequest,
        onResponse: (RequestId?) -> Unit,
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
            handleApiError(apiRequestOptions, response,)
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
        return ConfirmationParamsUtils.buildPaymentUserAgent(productUsageTokens, attribution)
    }

    private suspend fun ConfirmPaymentIntentParams.maybeForDashboard(
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConfirmPaymentIntentParams> {
        if (!options.apiKeyIsUserKey) {
            return Result.success(this)
        }

        val paymentMethodIdResult = if (paymentMethodId != null) {
            Result.success(paymentMethodId)
        } else if (paymentMethodCreateParams != null) {
            // New payment method: create the PM first, then build Dashboard params.
            createPaymentMethod(
                paymentMethodCreateParams = paymentMethodCreateParams,
                options = options,
                idempotencyKey = idempotencyKey,
            ).map { it.id }
        } else {
            return Result.success(this)
        }

        return paymentMethodIdResult.mapCatching { paymentMethod ->
            ConfirmPaymentIntentParams.createForDashboard(
                clientSecret = clientSecret,
                paymentMethodId = paymentMethod,
                paymentMethodOptions = paymentMethodOptions,
            )
        }
    }

    private suspend fun ConfirmSetupIntentParams.maybeForDashboard(
        options: RequestCredentials,
        idempotencyKey: String?,
    ): Result<ConfirmSetupIntentParams> {
        if (!options.apiKeyIsUserKey) {
            return Result.success(this)
        }

        val paymentMethodIdResult = if (paymentMethodId != null) {
            Result.success(paymentMethodId)
        } else if (paymentMethodCreateParams != null) {
            // New payment method: create the PM first, then build Dashboard params.
            createPaymentMethod(
                paymentMethodCreateParams = paymentMethodCreateParams,
                options = options,
                idempotencyKey = idempotencyKey,
            ).map { it.id }
        } else {
            return Result.success(this)
        }

        return paymentMethodIdResult.mapCatching { paymentMethod ->
            ConfirmSetupIntentParams.createForDashboard(
                clientSecret = clientSecret,
                paymentMethodId = paymentMethod,
                paymentMethodOptions = paymentMethodOptions,
            )
        }
    }

    private fun Result<StripeResponse<String>>.errorMessage(
        requestOptions: RequestCredentials,
    ): String? {
        val response = getOrNull()
        val resultError = exceptionOrNull()

        return when {
            resultError != null -> resultError.safeAnalyticsMessage
            response != null && response.isError -> {
                runCatching {
                    handleApiError(requestOptions, response,)
                }.let { errorResult ->
                    errorResult.exceptionOrNull()?.safeAnalyticsMessage
                }
            }

            else -> null
        }
    }

    private sealed class DnsCacheData {
        data class Success(
            val originalDnsCacheTtl: String?
        ) : DnsCacheData()

        data object Failure : DnsCacheData()
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
         * @return `https://api.stripe.com/v1/confirmation_tokens`
         */
        internal val confirmationTokensUrl: String
            @JvmSynthetic
            get() = getApiUrl("confirmation_tokens")

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
         * @return `https://api.stripe.com/v1/consumers/sessions/log_out`
         */
        internal val logoutConsumerUrl: String
            @JvmSynthetic
            get() = getApiUrl("consumers/sessions/log_out")

        /**
         * @return `https://api.stripe.com/v1/consumers/payment_details/list`
         */
        internal val listConsumerPaymentDetailsUrl: String
            @JvmSynthetic
            get() = getApiUrl("consumers/payment_details/list")

        /**
         * @return `https://api.stripe.com/v1/consumers/shipping_addresses/list`
         */
        internal val listShippingAddresses: String
            @JvmSynthetic
            get() = getApiUrl("consumers/shipping_addresses/list")

        /**
         * @return `https://api.stripe.com/v1/consumers/payment_details/share`
         */
        internal val sharePaymentDetailsUrl: String
            @JvmSynthetic
            get() = getApiUrl("consumers/payment_details/share")

        /**
         * @return `https://api.stripe.com/v1/connections/link_account_sessions_for_deferred_payment`
         */
        internal val deferredFinancialConnectionsSessionUrl: String
            @JvmSynthetic
            get() = getApiUrl("connections/link_account_sessions_for_deferred_payment")

        internal val mobileCardElementConfigUrl: String
            get() = getMerchantUiUrl("mobile-card-element-config")

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
         * This is an undocumented API and is only used for certain SIs which have a delay to
         * transfer its status out of "requires_action" after user performs the confirmation.
         *
         * @return `https://api.stripe.com/v1/setup_intents/:id/refresh`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getRefreshSetupIntentUrl(paymentIntentId: String): String {
            return getApiUrl("setup_intents/%s/refresh", paymentIntentId)
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
         * @return `https://api.stripe.com/v1/payment_intents/:id/cancel_challenge`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getCancelPaymentIntentCaptchaChallengeUrl(paymentIntentId: String): String {
            return getApiUrl("payment_intents/%s/cancel_challenge", paymentIntentId)
        }

        /**
         * @return `https://api.stripe.com/v1/setup_intents/:id/cancel_challenge`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getCancelSetupIntentCaptchaChallengeUrl(setupIntentId: String): String {
            return getApiUrl("setup_intents/%s/cancel_challenge", setupIntentId)
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
         * @return `https://api.stripe.com/v1/customers/:id/payment_methods/:id`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getRetrieveCustomerPaymentMethodUrl(
            customerId: String,
            paymentMethodId: String
        ): String {
            return getApiUrl("customers/%s/payment_methods/%s", customerId, paymentMethodId)
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

        /**
         * @return `https://api.stripe.com/v1/setup_intents/:clientSecret/verify_microdeposits`
         */
        @JvmSynthetic
        internal fun getPaymentMethodUrl(
            paymentMethodId: String,
        ): String {
            return getApiUrl("payment_methods/$paymentMethodId")
        }

        /**
         * @return `https://api.stripe.com/v1/elements/customers/:customerId/set_default_payment_method`
         */
        @VisibleForTesting
        internal fun getSetDefaultPaymentMethodUrl(
            customerId: String,
        ): String {
            return getApiUrl("elements/customers/$customerId/set_default_payment_method")
        }

        /**
         * @return `https://api.stripe.com/v1/elements/customers/:customerId/
         * saved_payment_method_from_card_present_payment_method/$paymentMethodId`
         */
        @VisibleForTesting
        internal fun getSavedPaymentMethodFromCardPresentPaymentMethod(
            customerId: String,
            paymentMethodId: String,
        ): String {
            return getApiUrl(
                "elements/customers/$customerId/" +
                    "saved_payment_method_from_card_present_payment_method/$paymentMethodId"
            )
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

        private fun getMerchantUiUrl(path: String): String {
            return "https://merchant-ui-api.stripe.com/elements/$path"
        }

        private fun createExpandParam(expandFields: List<String>): Map<String, List<String>> {
            return expandFields.takeIf { it.isNotEmpty() }?.let {
                mapOf("expand" to it)
            }.orEmpty()
        }
    }
}
