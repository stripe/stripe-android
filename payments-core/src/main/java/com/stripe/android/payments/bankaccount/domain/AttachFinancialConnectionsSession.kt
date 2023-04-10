package com.stripe.android.payments.bankaccount.domain

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.StripeRepository
import javax.inject.Inject

internal class AttachFinancialConnectionsSession @Inject constructor(
    private val stripeRepository: StripeRepository
) {

    /**
     * Attaches a LinkedAccountSession to a given PaymentIntent,
     * using the [linkedAccountSessionId] and the intent [clientSecret].
     *
     * @return [PaymentIntent] with attached linkedAccount.
     */
    suspend fun forPaymentIntent(
        publishableKey: String,
        linkedAccountSessionId: String,
        clientSecret: String,
        stripeAccountId: String?
    ): Result<PaymentIntent> =
        stripeRepository.attachFinancialConnectionsSessionToPaymentIntent(
            financialConnectionsSessionId = linkedAccountSessionId,
            clientSecret = clientSecret,
            paymentIntentId = PaymentIntent.ClientSecret(clientSecret).paymentIntentId,
            requestOptions = ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId
            ),
            expandFields = EXPAND_PAYMENT_METHOD
        )

    /**
     * Attaches a LinkedAccountSession to a given PaymentIntent,
     * using the [linkedAccountSessionId] and the intent [clientSecret].
     *
     * @return [SetupIntent] with attached linkedAccount.
     */
    suspend fun forSetupIntent(
        publishableKey: String,
        linkedAccountSessionId: String,
        clientSecret: String,
        stripeAccountId: String?
    ): Result<SetupIntent> =
        stripeRepository.attachFinancialConnectionsSessionToSetupIntent(
            financialConnectionsSessionId = linkedAccountSessionId,
            clientSecret = clientSecret,
            setupIntentId = SetupIntent.ClientSecret(clientSecret).setupIntentId,
            requestOptions = ApiRequest.Options(
                apiKey = publishableKey,
                stripeAccount = stripeAccountId
            ),
            expandFields = EXPAND_PAYMENT_METHOD
        )

    private companion object {
        private val EXPAND_PAYMENT_METHOD = listOf("payment_method")
    }
}
