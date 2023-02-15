package com.stripe.android.paymentsheet.repositories

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.PaymentMethodPreference
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

internal sealed class StripeIntentRepository {

    abstract suspend fun get(params: ElementsSessionParams): ElementsSession

    abstract suspend fun get(
        clientSecret: ClientSecret
    ): PaymentMethodPreference

    /**
     * Retrieve the [StripeIntent] from a static source.
     */
    class Static(
        private val stripeIntent: StripeIntent
    ) : StripeIntentRepository() {
        override suspend fun get(params: ElementsSessionParams): ElementsSession {
            return ElementsSession(
                linkSettings = null,
                paymentMethodTypes = emptyList(),
                unactivatedPaymentMethodTypes = emptyList(),
                paymentMethodSpecs = null,
                stripeIntent = null,
            )
        }

        override suspend fun get(clientSecret: ClientSecret) = PaymentMethodPreference(stripeIntent)
    }

    /**
     * Retrieve the [StripeIntent] from the [StripeRepository].
     */
    class Api @Inject constructor(
        private val stripeRepository: StripeRepository,
        private val lazyPaymentConfig: Provider<PaymentConfiguration>,
        @IOContext private val workContext: CoroutineContext,
        private val locale: Locale?
    ) : StripeIntentRepository() {

        // The PaymentConfiguration can change after initialization, so this needs to get a new
        // request options each time requested.
        private val requestOptions
            get() = ApiRequest.Options(
                lazyPaymentConfig.get().publishableKey,
                lazyPaymentConfig.get().stripeAccountId
            )

        override suspend fun get(params: ElementsSessionParams): ElementsSession {

        }

        /**
         * Tries to retrieve the StripeIntent with ordered Payment Methods, falling back to
         * traditional GET if we don't have a locale or the call fails for any reason.
         */
        override suspend fun get(clientSecret: ClientSecret) = withContext(workContext) {
            when (clientSecret) {
                is PaymentIntentClientSecret -> {
                    requireNotNull(
                        locale?.let {
                            runCatching {
                                stripeRepository.retrievePaymentIntentWithOrderedPaymentMethods(
                                    clientSecret.value,
                                    requestOptions,
                                    it
                                )
                            }.getOrNull()
                        } ?: stripeRepository.retrievePaymentIntent(
                            clientSecret.value,
                            requestOptions,
                            expandFields = listOf("payment_method")
                        )?.let {
                            PaymentMethodPreference(
                                it
                            )
                        }
                    ) {
                        "Could not parse PaymentIntent."
                    }
                }
                is SetupIntentClientSecret -> {
                    requireNotNull(
                        locale?.let {
                            runCatching {
                                stripeRepository.retrieveSetupIntentWithOrderedPaymentMethods(
                                    clientSecret.value,
                                    requestOptions,
                                    locale
                                )
                            }.getOrNull()
                        } ?: stripeRepository.retrieveSetupIntent(
                            clientSecret.value,
                            requestOptions,
                            expandFields = listOf("payment_method")
                        )?.let {
                            PaymentMethodPreference(
                                it
                            )
                        }
                    ) {
                        "Could not parse SetupIntent."
                    }
                }
            }
        }
    }
}
