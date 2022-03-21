package com.stripe.android.payments.bankaccount.domain

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.StripeRepository
import javax.inject.Inject


internal class AttachLinkAccountSession @Inject constructor(
    private val stripeRepository: StripeRepository
) {

    /**
     * Attaches a LinkedAccountSession to a given PaymentIntent,
     * using the [linkedAccountSessionId] and the intent [clientSecret].
     *
     * @return whether the operation succeeded or not.
     */
    suspend fun forPaymentIntent(
        publishableKey: String,
        linkedAccountSessionId: String,
        clientSecret: String,
    ): Result<Unit> = kotlin.runCatching {
        stripeRepository.attachLinkAccountSessionToPaymentIntent(
            linkAccountSessionId = linkedAccountSessionId,
            clientSecret = clientSecret,
            paymentIntentId = PaymentIntent.ClientSecret(clientSecret).paymentIntentId,
            requestOptions = ApiRequest.Options(publishableKey)
        )
    }.mapCatching { it ?: throw InternalError("Error attaching session to PaymentIntent") }

    /**
     * Attaches a LinkedAccountSession to a given PaymentIntent,
     * using the [linkedAccountSessionId] and the intent [clientSecret].
     *
     * @return whether the operation succeeded or not.
     */
    suspend fun forSetupIntent(
        publishableKey: String,
        linkedAccountSessionId: String,
        clientSecret: String,
    ): Result<Unit> = kotlin.runCatching {
        stripeRepository.attachLinkAccountSessionToSetupIntent(
            linkAccountSessionId = linkedAccountSessionId,
            clientSecret = clientSecret,
            setupIntentId = SetupIntent.ClientSecret(clientSecret).setupIntentId,
            requestOptions = ApiRequest.Options(publishableKey)
        )
    }.mapCatching { it ?: throw InternalError("Error attaching session to SetupIntent") }
}
