package com.stripe.android.paymentsheet.repositories

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

internal sealed class ElementsSessionRepository {

    abstract suspend fun get(
        clientSecret: ClientSecret
    ): ElementsSession

    /**
     * Retrieve the [StripeIntent] from a static source.
     */
    class Static(
        private val stripeIntent: StripeIntent
    ) : ElementsSessionRepository() {
        override suspend fun get(clientSecret: ClientSecret): ElementsSession {
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

        override suspend fun get(clientSecret: ClientSecret): ElementsSession {
            val params = when (clientSecret) {
                is PaymentIntentClientSecret -> {
                    ElementsSessionParams.PaymentIntentType(clientSecret.value)
                }
                is SetupIntentClientSecret -> {
                    ElementsSessionParams.SetupIntentType(clientSecret.value)
                }
            }

            val elementsSession = runCatching {
                stripeRepository.retrieveElementsSession(
                    params = params,
                    options = requestOptions,
                )
            }.getOrNull()

            return elementsSession ?: requireNotNull(fallback(clientSecret))
        }

        private suspend fun fallback(
            clientSecret: ClientSecret
        ): ElementsSession? = withContext(workContext) {
            when (clientSecret) {
                is PaymentIntentClientSecret -> {
                    stripeRepository.retrievePaymentIntent(
                        clientSecret = clientSecret.value,
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
                is SetupIntentClientSecret -> {
                    stripeRepository.retrieveSetupIntent(
                        clientSecret = clientSecret.value,
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
            }
        }
    }
}
