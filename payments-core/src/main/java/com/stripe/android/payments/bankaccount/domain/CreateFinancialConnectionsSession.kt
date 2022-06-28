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
        customerEmail: String?
    ): Result<FinancialConnectionsSession> = kotlin.runCatching {
        stripeRepository.createPaymentIntentFinancialConnectionsSession(
            paymentIntentId = PaymentIntent.ClientSecret(clientSecret).paymentIntentId,
            params = CreateFinancialConnectionsSessionParams(
                clientSecret = clientSecret,
                customerName = customerName,
                customerEmailAddress = customerEmail
            ),
            requestOptions = ApiRequest.Options(publishableKey)
        )
    }.mapCatching { it ?: throw InternalError("Error creating session for PaymentIntent") }

    /**
     * Creates a [FinancialConnectionsSession] for the given [SetupIntent] secret.
     */
    suspend fun forSetupIntent(
        publishableKey: String,
        clientSecret: String,
        customerName: String,
        customerEmail: String?
    ): Result<FinancialConnectionsSession> = kotlin.runCatching {
        stripeRepository.createSetupIntentFinancialConnectionsSession(
            setupIntentId = SetupIntent.ClientSecret(clientSecret).setupIntentId,
            params = CreateFinancialConnectionsSessionParams(
                clientSecret = clientSecret,
                customerName = customerName,
                customerEmailAddress = customerEmail
            ),
            requestOptions = ApiRequest.Options(publishableKey)
        )
    }.mapCatching { it ?: throw InternalError("Error creating session for SetupIntent") }
}
