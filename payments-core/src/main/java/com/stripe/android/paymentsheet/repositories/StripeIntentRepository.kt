package com.stripe.android.paymentsheet.repositories

import androidx.core.os.LocaleListCompat
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import kotlinx.coroutines.withContext
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
    class Api(
        private val stripeRepository: StripeRepository,
        private val requestOptions: ApiRequest.Options,
        private val workContext: CoroutineContext,
        private val locale: String? =
            LocaleListCompat.getAdjustedDefault().takeUnless { it.isEmpty }?.get(0)?.toLanguageTag()
    ) : StripeIntentRepository() {
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
