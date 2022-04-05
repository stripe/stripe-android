package com.stripe.android.networking

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.stripe.android.DefaultFraudDetectionDataRepository
import com.stripe.android.FraudDetectionDataRepository
import com.stripe.android.Stripe
import com.stripe.android.StripeApiBeta
import com.stripe.android.cards.Bin
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
import com.stripe.android.model.BankConnectionsLinkedAccountSession
import com.stripe.android.model.BankStatuses
import com.stripe.android.model.CardMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams.Companion.PARAM_CLIENT_SECRET
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.CreateLinkAccountSessionParams
import com.stripe.android.model.Customer
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
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
import com.stripe.android.model.parsers.BankConnectionsLinkAccountSessionJsonParser
import com.stripe.android.model.parsers.CardMetadataJsonParser
import com.stripe.android.model.parsers.ConsumerPaymentDetailsJsonParser
import com.stripe.android.model.parsers.ConsumerSessionJsonParser
import com.stripe.android.model.parsers.ConsumerSessionLookupJsonParser
import com.stripe.android.model.parsers.CustomerJsonParser
import com.stripe.android.model.parsers.FpxBankStatusesJsonParser
import com.stripe.android.model.parsers.IssuingCardPinJsonParser
import com.stripe.android.model.parsers.PaymentIntentJsonParser
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import com.stripe.android.model.parsers.PaymentMethodPreferenceForPaymentIntentJsonParser
import com.stripe.android.model.parsers.PaymentMethodPreferenceForSetupIntentJsonParser
import com.stripe.android.model.parsers.PaymentMethodPreferenceJsonParser
import com.stripe.android.model.parsers.PaymentMethodsListJsonParser
import com.stripe.android.model.parsers.RadarSessionJsonParser
import com.stripe.android.model.parsers.SetupIntentJsonParser
import com.stripe.android.model.parsers.SourceJsonParser
import com.stripe.android.model.parsers.Stripe3ds2AuthResultJsonParser
import com.stripe.android.model.parsers.TokenJsonParser
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.utils.StripeUrlUtils
import kotlinx.coroutines.Dispatchers
import org.json.JSONException
import org.json.JSONObject
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
internal class StripeApiRepository @JvmOverloads internal constructor(
    context: Context,
    publishableKeyProvider: () -> String,
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
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory =
        PaymentAnalyticsRequestFactory(context, publishableKeyProvider, productUsageTokens),
    private val fraudDetectionDataParamsUtils: FraudDetectionDataParamsUtils = FraudDetectionDataParamsUtils(),
    betas: Set<StripeApiBeta> = emptySet(),
    apiVersion: String = ApiVersion(betas = betas.map { it.code }.toSet()).code,
    sdkVersion: String = StripeSdkVersion.VERSION
) : StripeRepository() {

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
    }

    override suspend fun retrieveStripeIntent(
        clientSecret: String,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): StripeIntent {
        return when {
            PaymentIntent.ClientSecret.isMatch(clientSecret) -> {
                requireNotNull(
                    retrievePaymentIntent(clientSecret, options, expandFields)
                ) {
                    "Could not retrieve PaymentIntent."
                }
            }
            SetupIntent.ClientSecret.isMatch(clientSecret) -> {
                requireNotNull(
                    retrieveSetupIntent(clientSecret, options, expandFields)
                ) {
                    "Could not retrieve SetupIntent."
                }
            }
            else -> {
                error("Invalid client secret.")
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
        return confirmPaymentIntentInternal(
            confirmPaymentIntentParams = confirmPaymentIntentParams.maybeForDashboard(options),
            options = options,
            expandFields = expandFields
        )
    }

    private suspend fun confirmPaymentIntentInternal(
        confirmPaymentIntentParams: ConfirmPaymentIntentParams,
        options: ApiRequest.Options,
        expandFields: List<String>
    ): PaymentIntent? {
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
        val apiUrl = getConfirmPaymentIntentUrl(
            PaymentIntent.ClientSecret(confirmPaymentIntentParams.clientSecret).paymentIntentId
        )

        fireFraudDetectionDataRequest()

        return fetchStripeModel(
            apiRequestFactory.createPost(apiUrl, options, params),
            PaymentIntentJsonParser()
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
        val params: Map<String, Any?> =
            if (options.apiKeyIsUserKey) {
                createExpandParam(expandFields)
            } else {
                createClientSecretParam(clientSecret, expandFields)
            }

        fireFraudDetectionDataRequest()

        return fetchStripeModel(
            apiRequestFactory.createGet(
                getRetrievePaymentIntentUrl(paymentIntentId),
                options,
                params
            ),
            PaymentIntentJsonParser()
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
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    override suspend fun refreshPaymentIntent(
        clientSecret: String,
        options: ApiRequest.Options,
    ): PaymentIntent? {
        val paymentIntentId = PaymentIntent.ClientSecret(clientSecret).paymentIntentId

        fireFraudDetectionDataRequest()

        return fetchStripeModel(
            apiRequestFactory.createPost(
                getRefreshPaymentIntentUrl(paymentIntentId),
                options,
                createClientSecretParam(clientSecret, emptyList())
            ),
            PaymentIntentJsonParser()
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.PaymentIntentRefresh)
            )
        }
    }

    /**
     * Retrieve a [PaymentIntent] using its client_secret, with the accepted payment method types
     * ordered according to the [locale] provided.
     *
     * Analytics event: [PaymentAnalyticsEvent.PaymentIntentRetrieve]
     *
     * @param clientSecret client_secret of the PaymentIntent to retrieve
     * @param locale locale used to determine the order of the payment method types
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    override suspend fun retrievePaymentIntentWithOrderedPaymentMethods(
        clientSecret: String,
        options: ApiRequest.Options,
        locale: Locale
    ): PaymentIntent? = retrieveStripeIntentWithOrderedPaymentMethods(
        clientSecret,
        options,
        locale,
        parser = PaymentMethodPreferenceForPaymentIntentJsonParser(),
        analyticsEvent = PaymentAnalyticsEvent.PaymentIntentRetrieve
    )

    /**
     * Analytics event: [PaymentAnalyticsEvent.PaymentIntentCancelSource]
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
        fireFraudDetectionDataRequest()

        return fetchStripeModel(
            apiRequestFactory.createPost(
                getCancelPaymentIntentSourceUrl(paymentIntentId),
                options,
                mapOf("source" to sourceId)
            ),
            PaymentIntentJsonParser()
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

        fireFraudDetectionDataRequest()

        return fetchStripeModel(
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

        fireFraudDetectionDataRequest()

        return fetchStripeModel(
            apiRequestFactory.createGet(
                getRetrieveSetupIntentUrl(setupIntentId),
                options,
                createClientSecretParam(clientSecret, expandFields)
            ),
            SetupIntentJsonParser()
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.SetupIntentRetrieve)
            )
        }
    }

    /**
     * Retrieve a [SetupIntent] using its client_secret, with the accepted payment method types
     * ordered according to the [locale] provided.
     *
     * Analytics event: [PaymentAnalyticsEvent.SetupIntentRetrieve]
     *
     * @param clientSecret client_secret of the SetupIntent to retrieve
     * @param locale locale used to determine the order of the payment method types
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    override suspend fun retrieveSetupIntentWithOrderedPaymentMethods(
        clientSecret: String,
        options: ApiRequest.Options,
        locale: Locale
    ): SetupIntent? = retrieveStripeIntentWithOrderedPaymentMethods(
        clientSecret,
        options,
        locale,
        parser = PaymentMethodPreferenceForSetupIntentJsonParser(),
        analyticsEvent = PaymentAnalyticsEvent.SetupIntentRetrieve
    )

    /**
     * Analytics event: [PaymentAnalyticsEvent.SetupIntentCancelSource]
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
        return fetchStripeModel(
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
        fireFraudDetectionDataRequest()

        return fetchStripeModel(
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
        return fetchStripeModel(
            apiRequestFactory.createGet(
                getRetrieveSourceApiUrl(sourceId),
                options,
                SourceParams.createRetrieveSourceParams(clientSecret)
            ),
            SourceJsonParser()
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.SourceRetrieve)
            )
        }
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.PaymentMethodCreate]
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
        fireFraudDetectionDataRequest()

        return fetchStripeModel(
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
                    paymentMethodCreateParams.type,
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
        fireFraudDetectionDataRequest()

        return fetchStripeModel(
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
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getAddCustomerSourceUrl(customerId),
                requestOptions,
                mapOf("source" to sourceId)
            ),
            SourceJsonParser()
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
        return fetchStripeModel(
            apiRequestFactory.createDelete(
                getDeleteCustomerSourceUrl(customerId, sourceId),
                requestOptions
            ),
            SourceJsonParser()
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
        fireFraudDetectionDataRequest()

        return fetchStripeModel(
            apiRequestFactory.createPost(
                getAttachPaymentMethodUrl(paymentMethodId),
                requestOptions,
                mapOf("customer" to customerId)
            ),
            PaymentMethodJsonParser()
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory
                    .createAttachPaymentMethod(
                        productUsageTokens
                    )
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
    ): PaymentMethod? {
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getDetachPaymentMethodUrl(paymentMethodId),
                requestOptions
            ),
            PaymentMethodJsonParser()
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory
                    .createDetachPaymentMethod(
                        productUsageTokens
                    )
            )
        }
    }

    /**
     * Retrieve a Customer's [PaymentMethod]s
     *
     * Analytics event: [PaymentAnalyticsEvent.CustomerRetrievePaymentMethods]
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
        val paymentMethodsList = fetchStripeModel(
            apiRequestFactory.createGet(
                paymentMethodsUrl,
                requestOptions,
                listPaymentMethodsParams.toParamMap()
            ),
            PaymentMethodsListJsonParser()
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(
                    PaymentAnalyticsEvent.CustomerRetrievePaymentMethods,
                    productUsageTokens = productUsageTokens
                )
            )
        }

        return paymentMethodsList?.paymentMethods.orEmpty()
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.CustomerSetDefaultSource]
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
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getRetrieveCustomerUrl(customerId),
                requestOptions,
                mapOf("default_source" to sourceId)
            ),
            CustomerJsonParser()
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
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getRetrieveCustomerUrl(customerId),
                requestOptions,
                mapOf("shipping" to shippingInformation.toParamMap())
            ),
            CustomerJsonParser()
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
        return fetchStripeModel(
            apiRequestFactory.createGet(
                getRetrieveCustomerUrl(customerId),
                requestOptions
            ),
            CustomerJsonParser()
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
        requestOptions: ApiRequest.Options
    ): String? {
        val issuingCardPin = fetchStripeModel(
            apiRequestFactory.createGet(
                getIssuingCardPinUrl(cardId),
                requestOptions,
                mapOf(
                    "verification" to createVerificationParam(verificationId, userOneTimeCode)
                )
            ),
            IssuingCardPinJsonParser()
        ) {
            fireAnalyticsRequest(PaymentAnalyticsEvent.IssuingRetrievePin)
        }

        return issuingCardPin?.pin
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.IssuingUpdatePin]
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
        requestOptions: ApiRequest.Options
    ) {
        makeApiRequest(
            apiRequestFactory.createPost(
                getIssuingCardPinUrl(cardId),
                requestOptions,
                mapOf(
                    "verification" to createVerificationParam(verificationId, userOneTimeCode),
                    "pin" to newPin
                )
            )
        ) {
            fireAnalyticsRequest(PaymentAnalyticsEvent.IssuingUpdatePin)
        }
    }

    override suspend fun getFpxBankStatus(
        options: ApiRequest.Options
    ): BankStatuses {
        return runCatching {
            val fpxBankStatuses = fetchStripeModel(
                apiRequestFactory.createGet(
                    getApiUrl("fpx/bank_statuses"),

                    // don't pass connected account
                    options.copy(stripeAccount = null),

                    mapOf("account_holder_type" to "individual")
                ),
                FpxBankStatusesJsonParser()
            ) {
                fireAnalyticsRequest(PaymentAnalyticsEvent.FpxBankStatusesRetrieve)
            }

            requireNotNull(fpxBankStatuses)
        }.getOrDefault(BankStatuses())
    }

    override suspend fun getCardMetadata(
        bin: Bin,
        options: ApiRequest.Options
    ): CardMetadata? {
        return runCatching {
            fetchStripeModel(
                apiRequestFactory.createGet(
                    getEdgeUrl("card-metadata"),
                    options.copy(stripeAccount = null),
                    mapOf("key" to options.apiKey, "bin_prefix" to bin.value)
                ),
                CardMetadataJsonParser(bin)
            ) {
                // no-op
            }
        }.onFailure {
            fireAnalyticsRequest(PaymentAnalyticsEvent.CardMetadataLoadFailure)
        }.getOrNull()
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.Auth3ds2Start]
     */
    @VisibleForTesting
    override suspend fun start3ds2Auth(
        authParams: Stripe3ds2AuthParams,
        requestOptions: ApiRequest.Options
    ): Stripe3ds2AuthResult? {
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getApiUrl("3ds2/authenticate"),
                requestOptions,
                authParams.toParamMap()
            ),
            Stripe3ds2AuthResultJsonParser()
        ) {
            fireAnalyticsRequest(
                paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.Auth3ds2Start)
            )
        }
    }

    override suspend fun complete3ds2Auth(
        sourceId: String,
        requestOptions: ApiRequest.Options
    ): Stripe3ds2AuthResult? {
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getApiUrl("3ds2/challenge_complete"),
                requestOptions,
                mapOf("source" to sourceId)
            ),
            Stripe3ds2AuthResultJsonParser()
        ) {
            // no-op
        }
    }

    /**
     * Analytics event: [PaymentAnalyticsEvent.FileCreate]
     */
    override suspend fun createFile(
        fileParams: StripeFileParams,
        requestOptions: ApiRequest.Options
    ): StripeFile {
        val response = makeFileUploadRequest(
            FileUploadRequest(fileParams, requestOptions, appInfo)
        ) {
            fireAnalyticsRequest(PaymentAnalyticsEvent.FileCreate)
        }
        return StripeFileJsonParser().parse(response.responseJson())
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
        ) {
            fireAnalyticsRequest(PaymentAnalyticsEvent.StripeUrlRetrieve)
        }

        return response.responseJson()
    }

    /**
     * Get the latest [FraudDetectionData] from [FraudDetectionDataRepository] and send in POST request
     * to `/v1/radar/session`.
     */
    override suspend fun createRadarSession(
        requestOptions: ApiRequest.Options
    ): RadarSession? {
        return runCatching {
            require(Stripe.advancedFraudSignalsEnabled) {
                "Stripe.advancedFraudSignalsEnabled must be set to 'true' to create a Radar Session."
            }
            requireNotNull(fraudDetectionDataRepository.getLatest()) {
                "Could not obtain fraud data required to create a Radar Session."
            }
        }.map {
            val params = it.params.plus(buildPaymentUserAgentPair())
            fetchStripeModel(
                apiRequestFactory.createPost(
                    getApiUrl("radar/session"),
                    requestOptions,
                    params
                ),
                RadarSessionJsonParser()
            ) {
                fireAnalyticsRequest(
                    paymentAnalyticsRequestFactory.createRequest(PaymentAnalyticsEvent.RadarSessionCreate)
                )
            }
        }.getOrElse {
            throw StripeException.create(it)
        }
    }

    /**
     * Retrieves the ConsumerSession if the given email is associated with a Link account.
     */
    override suspend fun lookupConsumerSession(
        email: String,
        authSessionCookie: String?,
        requestOptions: ApiRequest.Options
    ): ConsumerSessionLookup? {
        return fetchStripeModel(
            apiRequestFactory.createPost(
                consumerSessionLookupUrl,
                requestOptions,
                mapOf("email_address" to email.lowercase())
                    .plus(
                        authSessionCookie?.let {
                            mapOf(
                                "cookies" to
                                    mapOf("verification_session_client_secrets" to listOf(it))
                            )
                        } ?: emptyMap()
                    )
            ),
            ConsumerSessionLookupJsonParser()
        ) {
            // no-op
        }
    }

    /**
     * Creates a new Link account for the credentials provided.
     */
    override suspend fun consumerSignUp(
        email: String,
        phoneNumber: String,
        country: String,
        authSessionCookie: String?,
        requestOptions: ApiRequest.Options
    ): ConsumerSession? {
        return fetchStripeModel(
            apiRequestFactory.createPost(
                consumerSignUpUrl,
                requestOptions,
                mapOf(
                    "email_address" to email.lowercase(),
                    "phone_number" to phoneNumber,
                    "country" to country
                ).plus(
                    authSessionCookie?.let {
                        mapOf(
                            "cookies" to
                                mapOf("verification_session_client_secrets" to listOf(it))
                        )
                    } ?: emptyMap()
                )
            ),
            ConsumerSessionJsonParser()
        ) {
            // no-op
        }
    }

    /**
     * Triggers an SMS verification for the consumer corresponding to the given client secret.
     */
    override suspend fun startConsumerVerification(
        consumerSessionClientSecret: String,
        locale: Locale,
        authSessionCookie: String?,
        requestOptions: ApiRequest.Options
    ): ConsumerSession? {
        return fetchStripeModel(
            apiRequestFactory.createPost(
                startConsumerVerificationUrl,
                requestOptions,
                mapOf(
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                    "type" to "SMS",
                    "locale" to locale.toLanguageTag()
                ).plus(
                    authSessionCookie?.let {
                        mapOf(
                            "cookies" to
                                mapOf("verification_session_client_secrets" to listOf(it))
                        )
                    } ?: emptyMap()
                )
            ),
            ConsumerSessionJsonParser()
        ) {
            // no-op
        }
    }

    /**
     * Confirms an SMS verification for the consumer corresponding to the given client secret.
     */
    override suspend fun confirmConsumerVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        authSessionCookie: String?,
        requestOptions: ApiRequest.Options
    ): ConsumerSession? {
        return fetchStripeModel(
            apiRequestFactory.createPost(
                confirmConsumerVerificationUrl,
                requestOptions,
                mapOf(
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                    "type" to "SMS",
                    "code" to verificationCode,
                    "client_type" to "MOBILE_SDK"
                ).plus(
                    authSessionCookie?.let {
                        mapOf(
                            "cookies" to
                                mapOf("verification_session_client_secrets" to listOf(it))
                        )
                    } ?: emptyMap()
                )
            ),
            ConsumerSessionJsonParser()
        ) {
            // no-op
        }
    }

    /**
     * Logs out the consumer and invalidates the cookie.
     */
    override suspend fun logoutConsumer(
        consumerSessionClientSecret: String,
        authSessionCookie: String?,
        requestOptions: ApiRequest.Options
    ): ConsumerSession? {
        return fetchStripeModel(
            apiRequestFactory.createPost(
                logoutConsumerUrl,
                requestOptions,
                mapOf(
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                ).plus(
                    authSessionCookie?.let {
                        mapOf(
                            "cookies" to
                                mapOf("verification_session_client_secrets" to listOf(it))
                        )
                    } ?: emptyMap()
                )
            ),
            ConsumerSessionJsonParser()
        ) {
            // no-op
        }
    }

    /**
     * Fetches the saved payment methods for the given customer.
     */
    override suspend fun listPaymentDetails(
        consumerSessionClientSecret: String,
        paymentMethodTypes: Set<String>,
        requestOptions: ApiRequest.Options
    ): ConsumerPaymentDetails? {
        return fetchStripeModel(
            apiRequestFactory.createGet(
                consumerPaymentDetailsUrl,
                requestOptions,
                mapOf(
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                    "types" to paymentMethodTypes.toList()
                )
            ),
            ConsumerPaymentDetailsJsonParser()
        ) {
            // no-op
        }
    }

    override suspend fun createPaymentIntentLinkAccountSession(
        paymentIntentId: String,
        params: CreateLinkAccountSessionParams,
        requestOptions: ApiRequest.Options
    ): BankConnectionsLinkedAccountSession? {
        return fetchStripeModel(
            apiRequestFactory.createPost(
                url = getPaymentIntentLinkAccountSessionUrl(paymentIntentId),
                options = requestOptions,
                params = params.toMap()
            ),
            BankConnectionsLinkAccountSessionJsonParser(),
        ) {
            // no-op
        }
    }

    override suspend fun createSetupIntentLinkAccountSession(
        setupIntentId: String,
        params: CreateLinkAccountSessionParams,
        requestOptions: ApiRequest.Options
    ): BankConnectionsLinkedAccountSession? {
        return fetchStripeModel(
            apiRequestFactory.createPost(
                url = getSetupIntentLinkAccountSessionUrl(setupIntentId),
                options = requestOptions,
                params = params.toMap()
            ),
            BankConnectionsLinkAccountSessionJsonParser(),
        ) {
            // no-op
        }
    }

    /**
     * @return `https://api.stripe.com/v1/payment_intents/:id/link_account_session`
     */
    @VisibleForTesting
    @JvmSynthetic
    internal fun getPaymentIntentLinkAccountSessionUrl(paymentIntentId: String): String {
        return getApiUrl("payment_intents/%s/link_account_sessions", paymentIntentId)
    }

    /**
     * @return `https://api.stripe.com/v1/setup_intents/:id/link_account_session`
     */
    @VisibleForTesting
    @JvmSynthetic
    internal fun getSetupIntentLinkAccountSessionUrl(setupIntentId: String): String {
        return getApiUrl("setup_intents/%s/link_account_sessions", setupIntentId)
    }

    /**
     * Attaches the Link Account Session to the Payment Intent
     */
    override suspend fun attachLinkAccountSessionToPaymentIntent(
        clientSecret: String,
        paymentIntentId: String,
        linkAccountSessionId: String,
        requestOptions: ApiRequest.Options
    ): PaymentIntent? {
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getAttachLinkAccountSessionToPaymentIntentUrl(
                    paymentIntentId,
                    linkAccountSessionId
                ),
                requestOptions,
                mapOf(
                    "client_secret" to clientSecret
                )
            ),
            PaymentIntentJsonParser()
        ) {
            // no-op
        }
    }

    /**
     * Attaches the Link Account Session to the Setup Intent
     */
    override suspend fun attachLinkAccountSessionToSetupIntent(
        clientSecret: String,
        setupIntentId: String,
        linkAccountSessionId: String,
        requestOptions: ApiRequest.Options
    ): SetupIntent? {
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getAttachLinkAccountSessionToSetupIntentUrl(setupIntentId, linkAccountSessionId),
                requestOptions,
                mapOf(
                    "client_secret" to clientSecret
                )
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
    ): PaymentIntent? {
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getVerifyMicrodepositsOnPaymentIntentUrl(PaymentIntent.ClientSecret(clientSecret).paymentIntentId),
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
    ): PaymentIntent? {
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getVerifyMicrodepositsOnPaymentIntentUrl(PaymentIntent.ClientSecret(clientSecret).paymentIntentId),
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
    ): SetupIntent? {
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getVerifyMicrodepositsOnSetupIntentUrl(SetupIntent.ClientSecret(clientSecret).setupIntentId),
                requestOptions,
                mapOf(
                    "client_secret" to clientSecret,
                    "amounts" to listOf(firstAmount, secondAmount)
                )
            ),
            SetupIntentJsonParser()
        ) {
            // no-op
        }
    }

    /**
     * Verifies the SetupIntent with microdeposits descriptor code
     */
    override suspend fun verifySetupIntentWithMicrodeposits(
        clientSecret: String,
        descriptorCode: String,
        requestOptions: ApiRequest.Options
    ): SetupIntent? {
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getVerifyMicrodepositsOnSetupIntentUrl(SetupIntent.ClientSecret(clientSecret).setupIntentId),
                requestOptions,
                mapOf(
                    "client_secret" to clientSecret,
                    "descriptor_code" to descriptorCode
                )
            ),
            SetupIntentJsonParser()
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

    private suspend fun <T : StripeIntent> retrieveStripeIntentWithOrderedPaymentMethods(
        clientSecret: String,
        options: ApiRequest.Options,
        locale: Locale,
        parser: PaymentMethodPreferenceJsonParser<T>,
        analyticsEvent: PaymentAnalyticsEvent
    ): T? {
        // Unsupported for user key sessions.
        if (options.apiKeyIsUserKey) return null

        fireFraudDetectionDataRequest()

        val params = createClientSecretParam(
            clientSecret,
            listOf("payment_method_preference.${parser.stripeIntentFieldName}.payment_method")
        ).plus(
            mapOf(
                "type" to parser.stripeIntentFieldName,
                "locale" to locale.toLanguageTag()
            )
        )

        return fetchStripeModel(
            apiRequestFactory.createGet(
                getApiUrl("elements/sessions"),
                options,
                params
            ),
            parser
        ) {
            fireAnalyticsRequest(paymentAnalyticsRequestFactory.createRequest(analyticsEvent))
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
        val stripeError = StripeErrorJsonParser().parse(response.responseJson())
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

    private suspend fun <ModelType : StripeModel> fetchStripeModel(
        apiRequest: ApiRequest,
        jsonParser: ModelJsonParser<ModelType>,
        onResponse: () -> Unit
    ): ModelType? {
        return jsonParser.parse(makeApiRequest(apiRequest, onResponse).responseJson())
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
        PAYMENT_USER_AGENT to
            setOf("stripe-android/${StripeSdkVersion.VERSION_NAME}")
                .plus(productUsageTokens)
                .plus(attribution)
                .joinToString(";")

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
        options: ApiRequest.Options,
    ): ConfirmPaymentIntentParams {
        if (!options.apiKeyIsUserKey || paymentMethodCreateParams == null) {
            return this
        }

        // For user key auth, we must create the PM first.
        val paymentMethodId = requireNotNull(
            createPaymentMethod(paymentMethodCreateParams, options)?.id
        )
        return ConfirmPaymentIntentParams.createForDashboard(
            clientSecret = clientSecret,
            paymentMethodId = paymentMethodId
        )
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
         * @return `https://api.stripe.com/v1/consumers/sessions/lookup`
         */
        internal val consumerSessionLookupUrl: String
            @JvmSynthetic
            get() = getApiUrl("consumers/sessions/lookup")

        /**
         * @return `https://api.stripe.com/v1/consumers/accounts/sign_up`
         */
        internal val consumerSignUpUrl: String
            @JvmSynthetic
            get() = getApiUrl("consumers/accounts/sign_up")

        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/start_verification`
         */
        internal val startConsumerVerificationUrl: String
            @JvmSynthetic
            get() = getApiUrl("consumers/sessions/start_verification")

        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/confirm_verification`
         */
        internal val confirmConsumerVerificationUrl: String
            @JvmSynthetic
            get() = getApiUrl("consumers/sessions/confirm_verification")

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
         * link_account_sessions/:linkAccountSessionId/attach`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getAttachLinkAccountSessionToPaymentIntentUrl(
            paymentIntentId: String,
            linkAccountSessionId: String
        ): String {
            return getApiUrl(
                "payment_intents/%s/link_account_sessions/%s/attach",
                paymentIntentId,
                linkAccountSessionId
            )
        }

        /**
         * @return `https://api.stripe.com/v1/setup_intents/:setupIntentId/
         * link_account_sessions/:linkAccountSessionId/attach`
         */
        @VisibleForTesting
        @JvmSynthetic
        internal fun getAttachLinkAccountSessionToSetupIntentUrl(
            setupIntentId: String,
            linkAccountSessionId: String
        ): String {
            return getApiUrl(
                "setup_intents/%s/link_account_sessions/%s/attach",
                setupIntentId,
                linkAccountSessionId
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
