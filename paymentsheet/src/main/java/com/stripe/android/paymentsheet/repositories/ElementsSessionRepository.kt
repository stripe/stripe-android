package com.stripe.android.paymentsheet.repositories

import android.app.Application
import com.stripe.android.DefaultFraudDetectionDataRepository
import com.stripe.android.PaymentConfiguration
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.Stripe
import com.stripe.android.common.di.APPLICATION_ID
import com.stripe.android.common.di.MOBILE_SESSION_ID
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.HTTP_INTERNAL_SERVER_ERROR
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithResultParser
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.parsers.ElementsSessionJsonParser
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.toDeferredIntentParams
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

internal interface ElementsSessionRepository {
    suspend fun get(
        initializationMode: PaymentElementLoader.InitializationMode,
        customer: PaymentSheet.CustomerConfiguration?,
        customPaymentMethods: List<PaymentSheet.CustomPaymentMethod>,
        externalPaymentMethods: List<String>,
        savedPaymentMethodSelectionId: String?,
        countryOverride: String?,
        linkDisallowedFundingSourceCreation: Set<String> = emptySet(),
    ): Result<ElementsSession>
}

internal class RealElementsSessionRepository @Inject constructor(
    application: Application,
    private val stripeNetworkClient: StripeNetworkClient,
    private val stripeRepository: StripeRepository,
    private val lazyPaymentConfig: Provider<PaymentConfiguration>,
    @IOContext private val workContext: CoroutineContext,
    @Named(MOBILE_SESSION_ID) private val mobileSessionIdProvider: Provider<String>,
    @Named(APPLICATION_ID) private val appId: String,
) : ElementsSessionRepository {

    private val fraudDetectionDataRepository =
        DefaultFraudDetectionDataRepository(application, workContext)

    private val apiRequestFactory = ApiRequest.Factory(
        appInfo = Stripe.appInfo,
        apiVersion = Stripe.API_VERSION,
        sdkVersion = StripeSdkVersion.VERSION,
    )
    private val stripeErrorJsonParser = StripeErrorJsonParser()

    // The PaymentConfiguration can change after initialization, so this needs to get a new
    // request options each time requested.
    private val requestOptions: ApiRequest.Options
        get() = ApiRequest.Options(
            apiKey = lazyPaymentConfig.get().publishableKey,
            stripeAccount = lazyPaymentConfig.get().stripeAccountId,
        )

    override suspend fun get(
        initializationMode: PaymentElementLoader.InitializationMode,
        customer: PaymentSheet.CustomerConfiguration?,
        customPaymentMethods: List<PaymentSheet.CustomPaymentMethod>,
        externalPaymentMethods: List<String>,
        savedPaymentMethodSelectionId: String?,
        countryOverride: String?,
        linkDisallowedFundingSourceCreation: Set<String>,
    ): Result<ElementsSession> {
        fraudDetectionDataRepository.refresh()

        val params = initializationMode.toElementsSessionParams(
            customer = customer,
            customPaymentMethods = customPaymentMethods,
            externalPaymentMethods = externalPaymentMethods,
            savedPaymentMethodSelectionId = savedPaymentMethodSelectionId,
            mobileSessionId = mobileSessionIdProvider.get(),
            appId = appId,
            countryOverride = countryOverride,
            linkDisallowedFundingSourceCreation = linkDisallowedFundingSourceCreation,
        )

        val options = requestOptions
        val elementsSession = retrieveElementsSession(params, options)

        return elementsSession.getResultOrElse { elementsSessionFailure ->
            if (shouldFallback(elementsSession)) {
                fallback(params, elementsSessionFailure)
            } else {
                elementsSession
            }
        }
    }

    private suspend fun retrieveElementsSession(
        params: ElementsSessionParams,
        options: ApiRequest.Options,
    ): Result<ElementsSession> {
        val requestParams = buildMap {
            this["type"] = params.type
            this["mobile_app_id"] = params.appId
            params.clientSecret?.let { this["client_secret"] = it }
            params.locale.let { this["locale"] = it }
            params.customerSessionClientSecret?.let { this["customer_session_client_secret"] = it }
            params.legacyCustomerEphemeralKey?.let { this["legacy_customer_ephemeral_key"] = it }
            params.externalPaymentMethods.takeIf { it.isNotEmpty() }?.let { this["external_payment_methods"] = it }
            params.customPaymentMethods.takeIf { it.isNotEmpty() }?.let { this["custom_payment_methods"] = it }
            params.mobileSessionId?.takeIf { it.isNotEmpty() }?.let { this["mobile_session_id"] = it }
            params.savedPaymentMethodSelectionId?.let { this["client_default_payment_method"] = it }
            params.sellerDetails?.let { this.putAll(it.toQueryParams()) }
            putAll(params.link.toQueryParams())
            params.countryOverride?.let { this["country_override"] = it }
            (params as? ElementsSessionParams.DeferredIntentType)?.let { type ->
                this.putAll(type.deferredIntentParams.toQueryParams())
            }
        }

        val expandParam = params.expandFields.takeIf { it.isNotEmpty() }?.let {
            mapOf("expand" to it)
        }.orEmpty()

        val requestFactory = apiRequestFactoryForParams(params)

        return executeRequestWithResultParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = requestFactory.createGet(
                url = ELEMENTS_SESSIONS_URL,
                options = options,
                params = requestParams + expandParam,
            ),
            responseJsonParser = ElementsSessionJsonParser(
                params = params,
                isLiveMode = options.apiKeyIsLiveMode,
            ),
        )
    }

    private suspend fun fallback(
        params: ElementsSessionParams,
        elementsSessionFailure: Throwable,
    ): Result<ElementsSession> = withContext(workContext) {
        val stripeIntent = when (params) {
            is ElementsSessionParams.PaymentIntentType -> {
                stripeRepository.retrievePaymentIntent(
                    clientSecret = params.clientSecret,
                    options = requestOptions,
                    expandFields = listOf("payment_method")
                )
            }
            is ElementsSessionParams.SetupIntentType -> {
                stripeRepository.retrieveSetupIntent(
                    clientSecret = params.clientSecret,
                    options = requestOptions,
                    expandFields = listOf("payment_method")
                )
            }
            is ElementsSessionParams.DeferredIntentType -> {
                Result.success(params.toStripeIntent(requestOptions))
            }
        }
        stripeIntent.map { intent ->
            ElementsSession.createFromFallback(
                stripeIntent = intent.withoutWeChatPay(),
                sessionsError = elementsSessionFailure
            )
        }
    }

    private fun apiRequestFactoryForParams(params: ElementsSessionParams): ApiRequest.Factory {
        if (params.sellerDetails?.networkBusinessProfile != null) {
            return ApiRequest.Factory(
                appInfo = Stripe.appInfo,
                apiVersion = ApiVersion(
                    betas = setOf(SELLER_PAYMENT_METHODS_BETA)
                ).code,
                sdkVersion = StripeSdkVersion.VERSION,
            )
        }
        return apiRequestFactory
    }

    private fun shouldFallback(elementsSession: Result<ElementsSession>): Boolean {
        return (elementsSession.exceptionOrNull() as? StripeException)?.let {
            it.statusCode >= HTTP_INTERNAL_SERVER_ERROR
        } ?: false
    }

    private companion object {
        private val ELEMENTS_SESSIONS_URL = "${ApiRequest.API_HOST}/v1/elements/sessions"
        private const val SELLER_PAYMENT_METHODS_BETA = "payment_element_seller_payment_methods_beta_1=v1"
    }
}

private fun StripeIntent.withoutWeChatPay(): StripeIntent {
    // We don't know if the merchant is eligible for H5 payments, so we filter out WeChat Pay.
    val filteredPaymentMethodTypes =
        paymentMethodTypes.filter { it != PaymentMethod.Type.WeChatPay.code }.ifEmpty { listOf("card") }
    return when (this) {
        is PaymentIntent -> copy(paymentMethodTypes = filteredPaymentMethodTypes)
        is SetupIntent -> copy(paymentMethodTypes = filteredPaymentMethodTypes)
    }
}

internal fun PaymentElementLoader.InitializationMode.toElementsSessionParams(
    customer: PaymentSheet.CustomerConfiguration?,
    customPaymentMethods: List<PaymentSheet.CustomPaymentMethod>,
    externalPaymentMethods: List<String>,
    savedPaymentMethodSelectionId: String?,
    mobileSessionId: String,
    appId: String,
    countryOverride: String?,
    linkDisallowedFundingSourceCreation: Set<String>,
): ElementsSessionParams {
    val customerSessionClientSecret = customer?.customerSessionClientSecret
    val legacyCustomerEphemeralKey = customer?.legacyCustomerEphemeralKey
    val customPaymentMethodIds = customPaymentMethods.toElementSessionParam()

    val linkParams = ElementsSessionParams.Link(
        disallowFundingSourceCreation = linkDisallowedFundingSourceCreation
    )

    return when (this) {
        is PaymentElementLoader.InitializationMode.PaymentIntent -> {
            ElementsSessionParams.PaymentIntentType(
                clientSecret = clientSecret,
                customerSessionClientSecret = customerSessionClientSecret,
                legacyCustomerEphemeralKey = legacyCustomerEphemeralKey,
                customPaymentMethods = customPaymentMethodIds,
                externalPaymentMethods = externalPaymentMethods,
                savedPaymentMethodSelectionId = savedPaymentMethodSelectionId,
                mobileSessionId = mobileSessionId,
                appId = appId,
                countryOverride = countryOverride,
                link = linkParams,
            )
        }

        is PaymentElementLoader.InitializationMode.SetupIntent -> {
            ElementsSessionParams.SetupIntentType(
                clientSecret = clientSecret,
                customerSessionClientSecret = customerSessionClientSecret,
                legacyCustomerEphemeralKey = legacyCustomerEphemeralKey,
                externalPaymentMethods = externalPaymentMethods,
                customPaymentMethods = customPaymentMethodIds,
                savedPaymentMethodSelectionId = savedPaymentMethodSelectionId,
                mobileSessionId = mobileSessionId,
                appId = appId,
                countryOverride = countryOverride,
                link = linkParams,
            )
        }

        is PaymentElementLoader.InitializationMode.DeferredIntent -> {
            ElementsSessionParams.DeferredIntentType(
                deferredIntentParams = intentConfiguration.toDeferredIntentParams(),
                customPaymentMethods = customPaymentMethodIds,
                externalPaymentMethods = externalPaymentMethods,
                customerSessionClientSecret = customerSessionClientSecret,
                legacyCustomerEphemeralKey = legacyCustomerEphemeralKey,
                savedPaymentMethodSelectionId = savedPaymentMethodSelectionId,
                mobileSessionId = mobileSessionId,
                sellerDetails = intentConfiguration.toSellerDetails(),
                appId = appId,
                countryOverride = countryOverride,
                link = linkParams,
            )
        }

        is PaymentElementLoader.InitializationMode.CryptoOnramp -> {
            val intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(),
            )
            ElementsSessionParams.DeferredIntentType(
                deferredIntentParams = intentConfiguration.toDeferredIntentParams(),
                customPaymentMethods = customPaymentMethodIds,
                externalPaymentMethods = externalPaymentMethods,
                customerSessionClientSecret = customerSessionClientSecret,
                legacyCustomerEphemeralKey = legacyCustomerEphemeralKey,
                savedPaymentMethodSelectionId = savedPaymentMethodSelectionId,
                mobileSessionId = mobileSessionId,
                sellerDetails = intentConfiguration.toSellerDetails(),
                appId = appId,
                countryOverride = countryOverride,
                link = linkParams,
            )
        }

        is PaymentElementLoader.InitializationMode.CheckoutSession -> {
            throw IllegalStateException("ElementsSessionParams is from server when using CheckoutSession")
        }
    }
}

private fun List<PaymentSheet.CustomPaymentMethod>.toElementSessionParam(): List<String> {
    return map { customPaymentMethod ->
        customPaymentMethod.id
    }
}

@OptIn(SharedPaymentTokenSessionPreview::class)
private fun PaymentSheet.IntentConfiguration.toSellerDetails(): ElementsSessionParams.SellerDetails? {
    return when (intentBehavior) {
        is PaymentSheet.IntentConfiguration.IntentBehavior.SharedPaymentToken -> intentBehavior.sellerDetails?.run {
            ElementsSessionParams.SellerDetails(
                networkId = networkId,
                externalId = externalId,
                businessName = businessName,
                networkBusinessProfile = networkBusinessProfile,
            )
        }
        is PaymentSheet.IntentConfiguration.IntentBehavior.Default -> null
    }
}

private val PaymentSheet.CustomerConfiguration.customerSessionClientSecret: String?
    get() = when (accessType) {
        is PaymentSheet.CustomerAccessType.CustomerSession -> accessType.customerSessionClientSecret
        is PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey -> null
    }

private val PaymentSheet.CustomerConfiguration.legacyCustomerEphemeralKey: String?
    get() = when (accessType) {
        is PaymentSheet.CustomerAccessType.CustomerSession -> null
        is PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey -> accessType.ephemeralKeySecret
    }

private fun ElementsSessionParams.DeferredIntentType.toStripeIntent(options: ApiRequest.Options): StripeIntent {
    val deferredIntentParams = this.deferredIntentParams
    val now = Calendar.getInstance().timeInMillis
    return when (val deferredIntentMode = deferredIntentParams.mode) {
        is DeferredIntentParams.Mode.Payment -> PaymentIntent(
            id = deferredIntentParams.paymentMethodConfigurationId,
            paymentMethodTypes = deferredIntentParams.paymentMethodTypes,
            amount = deferredIntentMode.amount,
            clientSecret = this.clientSecret,
            countryCode = null,
            created = now,
            currency = deferredIntentParams.mode.currency,
            isLiveMode = options.apiKeyIsLiveMode,
            unactivatedPaymentMethods = emptyList(),
            paymentMethodOptionsJsonString = deferredIntentMode.paymentMethodOptionsJsonString,
            automaticPaymentMethodsEnabled = deferredIntentParams.paymentMethodTypes.isEmpty(),
        )
        is DeferredIntentParams.Mode.Setup -> SetupIntent(
            id = deferredIntentParams.paymentMethodConfigurationId,
            cancellationReason = null,
            countryCode = null,
            clientSecret = this.clientSecret,
            description = null,
            created = now,
            isLiveMode = options.apiKeyIsLiveMode,
            linkFundingSources = emptyList(),
            nextActionData = null,
            paymentMethodId = null,
            paymentMethodTypes = deferredIntentParams.paymentMethodTypes,
            status = null,
            unactivatedPaymentMethods = emptyList(),
            usage = null,
            automaticPaymentMethodsEnabled = deferredIntentParams.paymentMethodTypes.isEmpty(),
        )
    }
}

private inline fun <T> Result<T>.getResultOrElse(
    transform: (Throwable) -> Result<T>,
): Result<T> {
    return exceptionOrNull()?.let(transform) ?: this
}
