package com.stripe.android.paymentsheet.repositories

import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.di.APPLICATION_ID
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
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
        externalPaymentMethods: List<String>,
        savedPaymentMethodSelectionId: String?,
    ): Result<ElementsSession>
}

/**
 * Retrieve the [StripeIntent] from the [StripeRepository].
 */
internal class RealElementsSessionRepository @Inject constructor(
    private val stripeRepository: StripeRepository,
    private val lazyPaymentConfig: Provider<PaymentConfiguration>,
    @IOContext private val workContext: CoroutineContext,
    @Named(APPLICATION_ID) private val appId: String
) : ElementsSessionRepository {

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
        externalPaymentMethods: List<String>,
        savedPaymentMethodSelectionId: String?,
    ): Result<ElementsSession> {
        val params = initializationMode.toElementsSessionParams(
            customer = customer,
            externalPaymentMethods = externalPaymentMethods,
            savedPaymentMethodSelectionId = savedPaymentMethodSelectionId,
            appId = appId
        )

        val elementsSession = stripeRepository.retrieveElementsSession(
            params = params,
            options = requestOptions,
        )

        return elementsSession.getResultOrElse { elementsSessionFailure ->
            fallback(params, elementsSessionFailure)
        }
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
    externalPaymentMethods: List<String>,
    savedPaymentMethodSelectionId: String?,
    appId: String
): ElementsSessionParams {
    val customerSessionClientSecret = customer?.toElementSessionParam()

    return when (this) {
        is PaymentElementLoader.InitializationMode.PaymentIntent -> {
            ElementsSessionParams.PaymentIntentType(
                clientSecret = clientSecret,
                customerSessionClientSecret = customerSessionClientSecret,
                externalPaymentMethods = externalPaymentMethods,
                savedPaymentMethodSelectionId = savedPaymentMethodSelectionId,
                appId = appId
            )
        }

        is PaymentElementLoader.InitializationMode.SetupIntent -> {
            ElementsSessionParams.SetupIntentType(
                clientSecret = clientSecret,
                customerSessionClientSecret = customerSessionClientSecret,
                externalPaymentMethods = externalPaymentMethods,
                savedPaymentMethodSelectionId = savedPaymentMethodSelectionId,
                appId = appId
            )
        }

        is PaymentElementLoader.InitializationMode.DeferredIntent -> {
            ElementsSessionParams.DeferredIntentType(
                deferredIntentParams = intentConfiguration.toDeferredIntentParams(),
                externalPaymentMethods = externalPaymentMethods,
                customerSessionClientSecret = customerSessionClientSecret,
                savedPaymentMethodSelectionId = savedPaymentMethodSelectionId,
                appId = appId
            )
        }
    }
}

private fun PaymentSheet.CustomerConfiguration.toElementSessionParam(): String? {
    return when (accessType) {
        is PaymentSheet.CustomerAccessType.CustomerSession -> accessType.customerSessionClientSecret
        is PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey -> null
    }
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
        )
    }
}

private inline fun <T> Result<T>.getResultOrElse(
    transform: (Throwable) -> Result<T>,
): Result<T> {
    return exceptionOrNull()?.let(transform) ?: this
}
