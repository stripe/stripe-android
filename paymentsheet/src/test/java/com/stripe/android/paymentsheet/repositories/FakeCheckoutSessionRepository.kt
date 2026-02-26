package com.stripe.android.paymentsheet.repositories

import com.stripe.android.core.networking.ApiRequest

internal class FakeCheckoutSessionRepository(
    var initResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
    var confirmResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
) : CheckoutSessionRepository {

    override suspend fun init(
        sessionId: String,
        options: ApiRequest.Options,
    ): Result<CheckoutSessionResponse> = initResult

    override suspend fun confirm(
        id: String,
        params: ConfirmCheckoutSessionParams,
        options: ApiRequest.Options,
    ): Result<CheckoutSessionResponse> = confirmResult
}
