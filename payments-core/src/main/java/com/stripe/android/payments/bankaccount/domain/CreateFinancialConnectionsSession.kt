package com.stripe.android.payments.bankaccount.domain

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.CreateFinancialConnectionsSessionParams
import com.stripe.android.model.FinancialConnectionsSession
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.StripeRepository
import javax.inject.Inject

internal class CreateFinancialConnectionsSession @Inject constructor(
    private val stripeRepository: StripeRepository
) {

    /**
     * Creates a [FinancialConnectionsSession] for the given [PaymentIntent] secret.
     */
    suspend fun forPaymentIntent(
        publishableKey: String,
        clientSecret: String,
        customerName: String,
        customerEmail: String?,
        stripeAccountId: String?
    ): Result<FinancialConnectionsSession> {
        val paymentIntentClientSecretResult = runCatching {
            PaymentIntent.ClientSecret(clientSecret)
        }

        return paymentIntentClientSecretResult.mapCatching { paymentIntentClientSecret ->
            stripeRepository.createPaymentIntentFinancialConnectionsSession(
                paymentIntentId = paymentIntentClientSecret.paymentIntentId,
                params = CreateFinancialConnectionsSessionParams(
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmailAddress = customerEmail
                ),
                requestOptions = ApiRequest.Options(
                    publishableKey,
                    stripeAccountId
                )
            ).getOrThrow()
        }
    }

    /**
     * Creates a [FinancialConnectionsSession] for the given [SetupIntent] secret.
     */
    suspend fun forSetupIntent(
        publishableKey: String,
        clientSecret: String,
        customerName: String,
        customerEmail: String?,
        stripeAccountId: String?
    ): Result<FinancialConnectionsSession> {
        val setupIntentClientSecretResult = runCatching {
            SetupIntent.ClientSecret(clientSecret)
        }

        return setupIntentClientSecretResult.mapCatching { setupIntentClientSecret ->
            stripeRepository.createSetupIntentFinancialConnectionsSession(
                setupIntentId = setupIntentClientSecret.setupIntentId,
                params = CreateFinancialConnectionsSessionParams(
                    clientSecret = clientSecret,
                    customerName = customerName,
                    customerEmailAddress = customerEmail
                ),
                requestOptions = ApiRequest.Options(
                    publishableKey,
                    stripeAccountId
                )
            ).getOrThrow()
        }
    }
}
