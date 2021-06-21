package com.stripe.android.paymentsheet.repositories

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
        private val workContext: CoroutineContext
    ) : StripeIntentRepository() {
        override suspend fun get(clientSecret: ClientSecret) = withContext(workContext) {
            when (clientSecret) {
                is PaymentIntentClientSecret -> {
                    val paymentIntent = stripeRepository.retrievePaymentIntent(
                        clientSecret.value,
                        requestOptions,
                        expandFields = listOf("payment_method")
                    )
                    requireNotNull(paymentIntent) {
                        "Could not parse PaymentIntent."
                    }
                }
                is SetupIntentClientSecret -> {
                    val setupIntent = stripeRepository.retrieveSetupIntent(
                        clientSecret.value,
                        requestOptions,
                        expandFields = listOf("payment_method")
                    )
                    requireNotNull(setupIntent) {
                        "Could not parse SetupIntent."
                    }
                }
            }
        }
    }
}
