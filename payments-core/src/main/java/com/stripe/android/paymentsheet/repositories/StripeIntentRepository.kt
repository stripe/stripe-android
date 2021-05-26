package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.PaymentIntent
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
    ): PaymentIntent

    /**
     * Retrieve the [PaymentIntent] from a static source.
     */
    class Static(
        private val paymentIntent: PaymentIntent
    ) : StripeIntentRepository() {
        override suspend fun get(clientSecret: ClientSecret): PaymentIntent = paymentIntent
    }

    /**
     * Retrieve the [PaymentIntent] from the API.
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
                    throw IllegalArgumentException("SetupIntents not supported")
                }
            }
        }
    }
}
