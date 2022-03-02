package com.stripe.android.paymentsheet.repositories

import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.StripeIntent
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.core.injection.IOContext
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import dagger.Lazy
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal sealed class StripeIntentRepository {
    abstract suspend fun get(
        clientSecret: ClientSecret
    ): StripeIntent

    /**
     * Retrieve the [StripeIntent] from a static source.
     */
    class Static(
        private val stripeIntent: StripeIntent
    ) : StripeIntentRepository() {
        override suspend fun get(clientSecret: ClientSecret) = stripeIntent
    }

    /**
     * Retrieve the [StripeIntent] from the [StripeRepository].
     */
    class Api @Inject constructor(
        private val stripeRepository: StripeRepository,
        private val lazyPaymentConfig: Lazy<PaymentConfiguration>,
        @IOContext private val workContext: CoroutineContext,
        private val locale: Locale?
    ) : StripeIntentRepository() {
        private val requestOptions by lazy {
            ApiRequest.Options(
                lazyPaymentConfig.get().publishableKey,
                lazyPaymentConfig.get().stripeAccountId
            )
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
                            stripeRepository.retrievePaymentIntentWithOrderedPaymentMethods(
                                clientSecret.value,
                                requestOptions,
                                it
                            )
                        } ?: stripeRepository.retrievePaymentIntent(
                            clientSecret.value,
                            requestOptions,
                            expandFields = listOf("payment_method")
                        )
                    ) {
                        "Could not parse PaymentIntent."
                    }
                }
                is SetupIntentClientSecret -> {
                    requireNotNull(
                        locale?.let {
                            stripeRepository.retrieveSetupIntentWithOrderedPaymentMethods(
                                clientSecret.value,
                                requestOptions,
                                locale
                            )
                        } ?: stripeRepository.retrieveSetupIntent(
                            clientSecret.value,
                            requestOptions,
                            expandFields = listOf("payment_method")
                        )
                    ) {
                        "Could not parse SetupIntent."
                    }
                }
            }
        }
    }
}
