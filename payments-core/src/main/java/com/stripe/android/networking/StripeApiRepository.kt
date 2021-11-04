package com.stripe.android.networking

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.stripe.android.ApiVersion
import com.stripe.android.AppInfo
import com.stripe.android.DefaultFraudDetectionDataRepository
import com.stripe.android.FraudDetectionDataRepository
import com.stripe.android.Logger
import com.stripe.android.Stripe
import com.stripe.android.StripeApiBeta
import com.stripe.android.cards.Bin
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.HTTP_TOO_MANY_REQUESTS
import com.stripe.android.core.networking.RequestId
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.networking.responseJson
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.CardException
import com.stripe.android.exception.PermissionException
import com.stripe.android.exception.RateLimitException
import com.stripe.android.exception.StripeException
import com.stripe.android.model.BankStatuses
import com.stripe.android.model.CardMetadata
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
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
import com.stripe.android.model.StripeErrorJsonParser
import com.stripe.android.model.StripeFile
import com.stripe.android.model.StripeFileParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeModel
import com.stripe.android.model.Token
import com.stripe.android.model.TokenParams
import com.stripe.android.model.parsers.CardMetadataJsonParser
import com.stripe.android.model.parsers.CustomerJsonParser
import com.stripe.android.model.parsers.FpxBankStatusesJsonParser
import com.stripe.android.model.parsers.IssuingCardPinJsonParser
import com.stripe.android.model.parsers.ModelJsonParser
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
import com.stripe.android.model.parsers.StripeFileJsonParser
import com.stripe.android.model.parsers.TokenJsonParser
import com.stripe.android.payments.core.injection.IOContext
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.PUBLISHABLE_KEY
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
    private val appInfo: AppInfo? = null,
    private val logger: Logger = Logger.noop(),
    private val workContext: CoroutineContext = Dispatchers.IO,
    private val productUsageTokens: Set<String> = emptySet(),
//    private val stripeApiRequestExecutor: ApiRequestExecutor = DefaultApiRequestExecutor(
//        workContext = workContext,
//        logger = logger
//    ),

    private val stripeNetworkClient: StripeNetworkClient = DefaultStripeNetworkClient(
        workContext = workContext,
        logger = logger
    ),
    private val analyticsRequestExecutor: AnalyticsRequestExecutor =
        DefaultAnalyticsRequestExecutor(logger, workContext),
    private val fraudDetectionDataRepository: FraudDetectionDataRepository =
        DefaultFraudDetectionDataRepository(context, workContext),
    private val analyticsRequestFactory: AnalyticsRequestFactory =
        AnalyticsRequestFactory(context, publishableKeyProvider, productUsageTokens),
    private val fraudDetectionDataParamsUtils: FraudDetectionDataParamsUtils = FraudDetectionDataParamsUtils(),
    betas: Set<StripeApiBeta> = emptySet(),
    apiVersion: String = ApiVersion(betas = betas).code,
    sdkVersion: String = Stripe.VERSION
) : StripeRepository() {

    @Inject
    constructor(
        appContext: Context,
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @IOContext workContext: CoroutineContext,
        @Named(PRODUCT_USAGE) productUsageTokens: Set<String>,
        analyticsRequestFactory: AnalyticsRequestFactory,
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        logger: Logger
    ) : this(
        context = appContext,
        publishableKeyProvider = publishableKeyProvider,
        logger = logger,
        workContext = workContext,
        productUsageTokens = productUsageTokens,
        analyticsRequestFactory = analyticsRequestFactory,
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
        val params = fraudDetectionDataParamsUtils.addFraudDetectionData(
            // Add payment_user_agent if the Payment Method is being created on this call
            maybeAddPaymentUserAgent(
                confirmPaymentIntentParams.toParamMap(),
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
                analyticsRequestFactory.createPaymentIntentConfirmation(
                    paymentMethodType
                )
            )
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

        fireFraudDetectionDataRequest()

        return fetchStripeModel(
            apiRequestFactory.createGet(
                getRetrievePaymentIntentUrl(paymentIntentId),
                options,
                createClientSecretParam(clientSecret, expandFields)
            ),
            PaymentIntentJsonParser()
        ) {
            fireAnalyticsRequest(
                analyticsRequestFactory.createRequest(AnalyticsEvent.PaymentIntentRetrieve)
            )
        }
    }

    /**
     * Refresh a [PaymentIntent] using its client_secret
     *
     * Analytics event: [AnalyticsEvent.PaymentIntentRefresh]
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
                analyticsRequestFactory.createRequest(AnalyticsEvent.PaymentIntentRefresh)
            )
        }
    }

    /**
     * Retrieve a [PaymentIntent] using its client_secret, with the accepted payment method types
     * ordered according to the [locale] provided.
     *
     * Analytics event: [AnalyticsEvent.PaymentIntentRetrieve]
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
        analyticsEvent = AnalyticsEvent.PaymentIntentRetrieve
    )

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
        fireFraudDetectionDataRequest()

        return fetchStripeModel(
            apiRequestFactory.createPost(
                getCancelPaymentIntentSourceUrl(paymentIntentId),
                options,
                mapOf("source" to sourceId)
            ),
            PaymentIntentJsonParser()
        ) {
            fireAnalyticsRequest(AnalyticsEvent.PaymentIntentCancelSource)
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
                analyticsRequestFactory.createSetupIntentConfirmation(
                    confirmSetupIntentParams.paymentMethodCreateParams?.typeCode
                )
            )
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
                analyticsRequestFactory.createRequest(AnalyticsEvent.SetupIntentRetrieve)
            )
        }
    }

    /**
     * Retrieve a [SetupIntent] using its client_secret, with the accepted payment method types
     * ordered according to the [locale] provided.
     *
     * Analytics event: [AnalyticsEvent.SetupIntentRetrieve]
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
        analyticsEvent = AnalyticsEvent.SetupIntentRetrieve
    )

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
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getCancelSetupIntentSourceUrl(setupIntentId),
                options,
                mapOf("source" to sourceId)
            ),
            SetupIntentJsonParser()
        ) {
            fireAnalyticsRequest(AnalyticsEvent.SetupIntentCancelSource)
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
                analyticsRequestFactory.createSourceCreation(
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
                analyticsRequestFactory.createRequest(AnalyticsEvent.SourceRetrieve)
            )
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
                analyticsRequestFactory.createPaymentMethodCreation(
                    paymentMethodCreateParams.type,
                    productUsageTokens = paymentMethodCreateParams.attribution
                )
            )
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
                analyticsRequestFactory.createTokenCreation(
                    productUsageTokens = tokenParams.attribution,
                    tokenType = tokenParams.tokenType
                )
            )
        }
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
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getAddCustomerSourceUrl(customerId),
                requestOptions,
                mapOf("source" to sourceId)
            ),
            SourceJsonParser()
        ) {
            fireAnalyticsRequest(
                analyticsRequestFactory.createAddSource(
                    productUsageTokens,
                    sourceType
                )
            )
        }
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
        return fetchStripeModel(
            apiRequestFactory.createDelete(
                getDeleteCustomerSourceUrl(customerId, sourceId),
                requestOptions
            ),
            SourceJsonParser()
        ) {
            fireAnalyticsRequest(
                analyticsRequestFactory.createDeleteSource(
                    productUsageTokens
                )
            )
        }
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
                analyticsRequestFactory
                    .createAttachPaymentMethod(
                        productUsageTokens
                    )
            )
        }
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
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getDetachPaymentMethodUrl(paymentMethodId),
                requestOptions
            ),
            PaymentMethodJsonParser()
        ) {
            fireAnalyticsRequest(
                analyticsRequestFactory
                    .createDetachPaymentMethod(
                        productUsageTokens
                    )
            )
        }
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
        val paymentMethodsList = fetchStripeModel(
            apiRequestFactory.createGet(
                paymentMethodsUrl,
                requestOptions,
                listPaymentMethodsParams.toParamMap()
            ),
            PaymentMethodsListJsonParser()
        ) {
            fireAnalyticsRequest(
                analyticsRequestFactory.createRequest(
                    AnalyticsEvent.CustomerRetrievePaymentMethods,
                    productUsageTokens = productUsageTokens
                )
            )
        }

        return paymentMethodsList?.paymentMethods.orEmpty()
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
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getRetrieveCustomerUrl(customerId),
                requestOptions,
                mapOf("default_source" to sourceId)
            ),
            CustomerJsonParser()
        ) {
            fireAnalyticsRequest(
                analyticsRequestFactory.createRequest(
                    event = AnalyticsEvent.CustomerSetDefaultSource,
                    productUsageTokens = productUsageTokens,
                    sourceType = sourceType
                )
            )
        }
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
        return fetchStripeModel(
            apiRequestFactory.createPost(
                getRetrieveCustomerUrl(customerId),
                requestOptions,
                mapOf("shipping" to shippingInformation.toParamMap())
            ),
            CustomerJsonParser()
        ) {
            fireAnalyticsRequest(
                analyticsRequestFactory.createRequest(
                    AnalyticsEvent.CustomerSetShippingInfo,
                    productUsageTokens = productUsageTokens
                )
            )
        }
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
        return fetchStripeModel(
            apiRequestFactory.createGet(
                getRetrieveCustomerUrl(customerId),
                requestOptions
            ),
            CustomerJsonParser()
        ) {
            fireAnalyticsRequest(
                analyticsRequestFactory.createRequest(
                    AnalyticsEvent.CustomerRetrieve,
                    productUsageTokens = productUsageTokens
                )
            )
        }
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
            fireAnalyticsRequest(AnalyticsEvent.IssuingRetrievePin)
        }

        return issuingCardPin?.pin
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
            fireAnalyticsRequest(AnalyticsEvent.IssuingUpdatePin)
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
                fireAnalyticsRequest(AnalyticsEvent.FpxBankStatusesRetrieve)
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
            fireAnalyticsRequest(AnalyticsEvent.CardMetadataLoadFailure)
        }.getOrNull()
    }

    /**
     * Analytics event: [AnalyticsEvent.Auth3ds2Start]
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
                analyticsRequestFactory.createRequest(AnalyticsEvent.Auth3ds2Start)
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
     * Analytics event: [AnalyticsEvent.FileCreate]
     */
    override suspend fun createFile(
        fileParams: StripeFileParams,
        requestOptions: ApiRequest.Options
    ): StripeFile {
        val response = makeFileUploadRequest(
            FileUploadRequest(fileParams, requestOptions, appInfo)
        ) {
            fireAnalyticsRequest(AnalyticsEvent.FileCreate)
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
            fireAnalyticsRequest(AnalyticsEvent.StripeUrlRetrieve)
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
                    analyticsRequestFactory.createRequest(AnalyticsEvent.RadarSessionCreate)
                )
            }
        }.getOrElse {
            throw StripeException.create(it)
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
        analyticsEvent: AnalyticsEvent
    ): T? {
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
            fireAnalyticsRequest(analyticsRequestFactory.createRequest(analyticsEvent))
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
//            stripeApiRequestExecutor.execute(apiRequest)
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
//            stripeApiRequestExecutor.execute(fileUploadRequest)
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
        event: AnalyticsEvent
    ) {
        fireAnalyticsRequest(
            analyticsRequestFactory.createRequest(event)
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
            setOf("stripe-android/${Stripe.VERSION_NAME}")
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
