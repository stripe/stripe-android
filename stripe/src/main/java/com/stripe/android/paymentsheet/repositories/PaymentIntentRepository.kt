package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.PaymentIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal sealed class PaymentIntentRepository {
    abstract suspend fun get(
        clientSecret: String
    ): PaymentIntent

    /**
     * Retrieve the [PaymentIntent] from a static source.
     */
    class Static(
        private val paymentIntent: PaymentIntent
    ) : PaymentIntentRepository() {
        override suspend fun get(clientSecret: String): PaymentIntent = paymentIntent
    }

    /**
     * Retrieve the [PaymentIntent] from the API.
     */
    class Api(
        private val stripeRepository: StripeRepository,
        private val requestOptions: ApiRequest.Options,
        private val workContext: CoroutineContext
    ) : PaymentIntentRepository() {
        override suspend fun get(clientSecret: String) = withContext(workContext) {
            val paymentIntent = stripeRepository.retrievePaymentIntent(
                clientSecret,
                requestOptions
            )
            requireNotNull(paymentIntent) {
                "Could not parse PaymentIntent."
            }
        }
    }
}
