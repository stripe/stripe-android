package com.stripe.android.networking

import com.stripe.android.AlipayAuthenticator
import com.stripe.android.ApiRequest
import com.stripe.android.model.AlipayAuthResult
import com.stripe.android.model.StripeIntent

internal interface AlipayRepository {
    suspend fun authenticate(
        intent: StripeIntent,
        authenticator: AlipayAuthenticator,
        requestOptions: ApiRequest.Options
    ): AlipayAuthResult
}
