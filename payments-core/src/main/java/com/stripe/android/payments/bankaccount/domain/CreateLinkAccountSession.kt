package com.stripe.android.payments.bankaccount.domain

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.BankConnectionsLinkedAccountSession
import com.stripe.android.model.CreateLinkAccountSessionParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.StripeRepository
import javax.inject.Inject

internal class CreateLinkAccountSession @Inject constructor(
    private val stripeRepository: StripeRepository
) {

    suspend fun forPaymentIntent(
        publishableKey: String,
        clientSecret: String,
        customerName: String,
        customerEmail: String?,
    ): Result<BankConnectionsLinkedAccountSession> = kotlin.runCatching {
        stripeRepository.createPaymentIntentLinkAccountSession(
            paymentIntentId = PaymentIntent.ClientSecret(clientSecret).paymentIntentId,
            params = CreateLinkAccountSessionParams(
                clientSecret = clientSecret,
                customerName = customerName,
                customerEmailAddress = customerEmail
            ),
            requestOptions = requestOptions(publishableKey)
        )
    }.mapCatching { requireNotNull(it) }

    suspend fun forSetupIntent(
        publishableKey: String,
        clientSecret: String,
        customerName: String,
        customerEmail: String?,
    ): Result<BankConnectionsLinkedAccountSession> = kotlin.runCatching {
        stripeRepository.createSetupIntentLinkAccountSession(
            setupIntentId = SetupIntent.ClientSecret(clientSecret).setupIntentId,
            params = CreateLinkAccountSessionParams(
                clientSecret = clientSecret,
                customerName = customerName,
                customerEmailAddress = customerEmail
            ),
            requestOptions = requestOptions(publishableKey)
        )
    }.mapCatching { requireNotNull(it) }

    private fun requestOptions(publishableKey: String) = ApiRequest.Options(
        publishableKeyProvider = { publishableKey },
        stripeAccountIdProvider = { null }, // provide account id?
    )
}
