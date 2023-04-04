package com.stripe.android.paymentsheet.repositories

import com.stripe.android.ExperimentalPaymentSheetDecouplingApi
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.DeferredIntentParams.CaptureMethod
import com.stripe.android.model.DeferredIntentParams.Mode
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.Usage
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

internal sealed class ElementsSessionRepository {

    abstract suspend fun get(
        initializationMode: PaymentSheet.InitializationMode,
    ): ElementsSession

    /**
     * Retrieve the [StripeIntent] from a static source.
     */
    class Static(
        private val stripeIntent: StripeIntent
    ) : ElementsSessionRepository() {
        override suspend fun get(
            initializationMode: PaymentSheet.InitializationMode,
        ): ElementsSession {
            return ElementsSession(
                linkSettings = null,
                paymentMethodSpecs = null,
                stripeIntent = stripeIntent,
            )
        }
    }

    /**
     * Retrieve the [StripeIntent] from the [StripeRepository].
     */
    class Api @Inject constructor(
        private val stripeRepository: StripeRepository,
        private val lazyPaymentConfig: Provider<PaymentConfiguration>,
        @IOContext private val workContext: CoroutineContext,
    ) : ElementsSessionRepository() {

        // The PaymentConfiguration can change after initialization, so this needs to get a new
        // request options each time requested.
        private val requestOptions: ApiRequest.Options
            get() = ApiRequest.Options(
                apiKey = lazyPaymentConfig.get().publishableKey,
                stripeAccount = lazyPaymentConfig.get().stripeAccountId,
            )

        override suspend fun get(
            initializationMode: PaymentSheet.InitializationMode,
        ): ElementsSession {
            val params = initializationMode.toElementsSessionParams()

            val elementsSession = runCatching {
                stripeRepository.retrieveElementsSession(
                    params = params,
                    options = requestOptions,
                )
            }.getOrNull()

            return elementsSession ?: requireNotNull(fallback(params))
        }

        private suspend fun fallback(
            params: ElementsSessionParams,
        ): ElementsSession? = withContext(workContext) {
            when (params) {
                is ElementsSessionParams.PaymentIntentType -> {
                    stripeRepository.retrievePaymentIntent(
                        clientSecret = params.clientSecret,
                        options = requestOptions,
                        expandFields = listOf("payment_method")
                    )?.let {
                        ElementsSession(
                            linkSettings = null,
                            paymentMethodSpecs = null,
                            stripeIntent = it,
                        )
                    }
                }
                is ElementsSessionParams.SetupIntentType -> {
                    stripeRepository.retrieveSetupIntent(
                        clientSecret = params.clientSecret,
                        options = requestOptions,
                        expandFields = listOf("payment_method")
                    )?.let {
                        ElementsSession(
                            linkSettings = null,
                            paymentMethodSpecs = null,
                            stripeIntent = it,
                        )
                    }
                }
                is ElementsSessionParams.DeferredIntentType -> {
                    // We don't have a fallback endpoint for the deferred intent flow
                    null
                }
            }
        }
    }
}

@OptIn(ExperimentalPaymentSheetDecouplingApi::class)
internal fun PaymentSheet.InitializationMode.toElementsSessionParams(): ElementsSessionParams {
    return when (this) {
        is PaymentSheet.InitializationMode.PaymentIntent -> {
            ElementsSessionParams.PaymentIntentType(clientSecret = clientSecret)
        }
        is PaymentSheet.InitializationMode.SetupIntent -> {
            ElementsSessionParams.SetupIntentType(clientSecret = clientSecret)
        }
        is PaymentSheet.InitializationMode.DeferredIntent -> {
            ElementsSessionParams.DeferredIntentType(
                deferredIntentParams = DeferredIntentParams(
                    mode = intentConfiguration.mode.toElementsSessionParam(),
                    setupFutureUsage = intentConfiguration.setupFutureUse?.toElementsSessionParam(),
                    captureMethod = intentConfiguration.captureMethod?.toElementsSessionParam(),
                    paymentMethodTypes = intentConfiguration.paymentMethodTypes.toSet(),
                ),
            )
        }
    }
}

@OptIn(ExperimentalPaymentSheetDecouplingApi::class)
private fun PaymentSheet.IntentConfiguration.Mode.toElementsSessionParam(): Mode {
    return when (this) {
        is PaymentSheet.IntentConfiguration.Mode.Payment -> Mode.Payment(amount, currency)
        is PaymentSheet.IntentConfiguration.Mode.Setup -> Mode.Setup(currency)
    }
}

@OptIn(ExperimentalPaymentSheetDecouplingApi::class)
private fun PaymentSheet.IntentConfiguration.SetupFutureUse.toElementsSessionParam(): Usage {
    return when (this) {
        PaymentSheet.IntentConfiguration.SetupFutureUse.OnSession -> Usage.OnSession
        PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession -> Usage.OffSession
    }
}

@OptIn(ExperimentalPaymentSheetDecouplingApi::class)
private fun PaymentSheet.IntentConfiguration.CaptureMethod.toElementsSessionParam(): CaptureMethod {
    return when (this) {
        PaymentSheet.IntentConfiguration.CaptureMethod.Automatic -> CaptureMethod.Automatic
        PaymentSheet.IntentConfiguration.CaptureMethod.Manual -> CaptureMethod.Manual
    }
}
