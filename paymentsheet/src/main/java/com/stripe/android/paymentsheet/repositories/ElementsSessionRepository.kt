package com.stripe.android.paymentsheet.repositories

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.DeferredIntentParams.Mode
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.PaymentIntent.CaptureMethod
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
    ): Result<ElementsSession>

    /**
     * Retrieve the [StripeIntent] from a static source.
     */
    class Static(
        private val stripeIntent: StripeIntent
    ) : ElementsSessionRepository() {
        override suspend fun get(
            initializationMode: PaymentSheet.InitializationMode,
        ): Result<ElementsSession> {
            return Result.success(
                ElementsSession(
                    linkSettings = null,
                    paymentMethodSpecs = null,
                    stripeIntent = stripeIntent,
                    merchantCountry = null,
                )
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
        ): Result<ElementsSession> {
            val params = initializationMode.toElementsSessionParams()

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
            when (params) {
                is ElementsSessionParams.PaymentIntentType -> {
                    stripeRepository.retrievePaymentIntent(
                        clientSecret = params.clientSecret,
                        options = requestOptions,
                        expandFields = listOf("payment_method")
                    ).map {
                        ElementsSession(
                            linkSettings = null,
                            paymentMethodSpecs = null,
                            stripeIntent = it,
                            merchantCountry = null,
                        )
                    }
                }
                is ElementsSessionParams.SetupIntentType -> {
                    stripeRepository.retrieveSetupIntent(
                        clientSecret = params.clientSecret,
                        options = requestOptions,
                        expandFields = listOf("payment_method")
                    ).map {
                        ElementsSession(
                            linkSettings = null,
                            paymentMethodSpecs = null,
                            stripeIntent = it,
                            merchantCountry = null,
                        )
                    }
                }
                is ElementsSessionParams.DeferredIntentType -> {
                    // We don't have a fallback endpoint for the deferred intent flow
                    Result.failure(elementsSessionFailure)
                }
            }
        }
    }
}

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
                    paymentMethodTypes = intentConfiguration.paymentMethodTypes,
                    onBehalfOf = intentConfiguration.onBehalfOf,
                ),
            )
        }
    }
}

private fun PaymentSheet.IntentConfiguration.Mode.toElementsSessionParam(): Mode {
    return when (this) {
        is PaymentSheet.IntentConfiguration.Mode.Payment -> {
            Mode.Payment(
                amount = amount,
                currency = currency,
                setupFutureUsage = setupFutureUse?.toElementsSessionParam(),
                captureMethod = captureMethod.toElementsSessionParam(),
            )
        }
        is PaymentSheet.IntentConfiguration.Mode.Setup -> {
            Mode.Setup(
                currency = currency,
                setupFutureUsage = setupFutureUse.toElementsSessionParam(),
            )
        }
    }
}

private fun PaymentSheet.IntentConfiguration.SetupFutureUse.toElementsSessionParam(): Usage {
    return when (this) {
        PaymentSheet.IntentConfiguration.SetupFutureUse.OnSession -> Usage.OnSession
        PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession -> Usage.OffSession
    }
}

private fun PaymentSheet.IntentConfiguration.CaptureMethod.toElementsSessionParam(): CaptureMethod {
    return when (this) {
        PaymentSheet.IntentConfiguration.CaptureMethod.Automatic -> CaptureMethod.Automatic
        PaymentSheet.IntentConfiguration.CaptureMethod.AutomaticAsync -> CaptureMethod.AutomaticAsync
        PaymentSheet.IntentConfiguration.CaptureMethod.Manual -> CaptureMethod.Manual
    }
}

private inline fun <T> Result<T>.getResultOrElse(
    transform: (Throwable) -> Result<T>,
): Result<T> {
    return exceptionOrNull()?.let(transform) ?: this
}
