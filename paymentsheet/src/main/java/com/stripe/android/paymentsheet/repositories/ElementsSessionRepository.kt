package com.stripe.android.paymentsheet.repositories

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

internal sealed class ElementsSessionRepository {

    abstract suspend fun get(
        initializationMode: PaymentSheet.InitializationMode
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

private fun PaymentSheet.InitializationMode.toElementsSessionParams(): ElementsSessionParams {
    return when (this) {
        is PaymentSheet.InitializationMode.PaymentIntent -> {
            ElementsSessionParams.PaymentIntentType(clientSecret = clientSecret)
        }
        is PaymentSheet.InitializationMode.SetupIntent -> {
            ElementsSessionParams.SetupIntentType(clientSecret = clientSecret)
        }
    }
}
