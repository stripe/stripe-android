package com.stripe.android.networking

import com.stripe.android.AlipayAuthenticator
import com.stripe.android.core.networking.RequestCredentials
import com.stripe.android.model.AlipayAuthResult
import com.stripe.android.model.PaymentIntent

internal interface AlipayRepository {
    suspend fun authenticate(
        paymentIntent: PaymentIntent,
        authenticator: AlipayAuthenticator,
        requestOptions: RequestCredentials,
        idempotencyKey: String?,
    ): AlipayAuthResult
}
