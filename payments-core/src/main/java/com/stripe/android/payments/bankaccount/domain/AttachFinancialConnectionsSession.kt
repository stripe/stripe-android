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
    ): Result<PaymentIntent> {
        val paymentIntentClientSecretResult = runCatching {
            PaymentIntent.ClientSecret(clientSecret)
        }

        return paymentIntentClientSecretResult.mapCatching { paymentIntentClientSecret ->
            stripeRepository.attachFinancialConnectionsSessionToPaymentIntent(
                financialConnectionsSessionId = linkedAccountSessionId,
                clientSecret = paymentIntentClientSecret.value,
                paymentIntentId = paymentIntentClientSecret.paymentIntentId,
                requestOptions = ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                ),
                expandFields = EXPAND_PAYMENT_METHOD
            ).getOrThrow()
        }
    }

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
    ): Result<SetupIntent> {
        val setupIntentClientSecretResult = runCatching {
            SetupIntent.ClientSecret(clientSecret)
        }

        return setupIntentClientSecretResult.mapCatching { setupIntentClientSecret ->
            stripeRepository.attachFinancialConnectionsSessionToSetupIntent(
                financialConnectionsSessionId = linkedAccountSessionId,
                clientSecret = setupIntentClientSecret.value,
                setupIntentId = setupIntentClientSecret.setupIntentId,
                requestOptions = ApiRequest.Options(
                    apiKey = publishableKey,
                    stripeAccount = stripeAccountId
                ),
                expandFields = EXPAND_PAYMENT_METHOD
            ).getOrThrow()
        }
    }

    private companion object {
        private val EXPAND_PAYMENT_METHOD = listOf("payment_method")
    }
}
