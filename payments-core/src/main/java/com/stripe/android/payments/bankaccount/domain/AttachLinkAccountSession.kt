package com.stripe.android.payments.bankaccount.domain

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.StripeRepository
import javax.inject.Inject

internal class AttachLinkAccountSession @Inject constructor(
    private val stripeRepository: StripeRepository
) {

    suspend fun forPaymentIntent(
        publishableKey: String,
        linkedAccountSessionId: String,
        clientSecret: String,
    ): Result<ClientSecret> = kotlin.runCatching {
        stripeRepository.attachLinkAccountSessionToPaymentIntent(
            linkAccountSessionId = linkedAccountSessionId,
            clientSecret = clientSecret,
            paymentIntentId = PaymentIntent.ClientSecret(clientSecret).paymentIntentId,
            requestOptions = buildRequestOptions(publishableKey)
        )
    }.mapCatching { ClientSecret(requireNotNull(it!!.clientSecret)) }

    suspend fun forSetupIntent(
        publishableKey: String,
        linkedAccountSessionId: String,
        clientSecret: String,
    ): Result<ClientSecret> = kotlin.runCatching {
        stripeRepository.attachLinkAccountSessionToSetupIntent(
            linkAccountSessionId = linkedAccountSessionId,
            clientSecret = clientSecret,
            setupIntentId = SetupIntent.ClientSecret(clientSecret).setupIntentId,
            requestOptions = buildRequestOptions(publishableKey)
        )
    }.mapCatching { ClientSecret(requireNotNull(it!!.clientSecret)) }

    private fun buildRequestOptions(publishableKey: String) = ApiRequest.Options(
        publishableKeyProvider = { publishableKey },
        stripeAccountIdProvider = { null }, // provide account id?
    )

    @JvmInline
    value class ClientSecret(val value: String)
}
