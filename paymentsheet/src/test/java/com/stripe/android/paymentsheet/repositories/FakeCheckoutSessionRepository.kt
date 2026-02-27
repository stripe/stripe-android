package com.stripe.android.paymentsheet.repositories

internal class FakeCheckoutSessionRepository(
    var initResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
    var confirmResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
) : CheckoutSessionRepository {

    override suspend fun init(
        sessionId: String,
    ): Result<CheckoutSessionResponse> = initResult

    override suspend fun confirm(
        id: String,
        params: ConfirmCheckoutSessionParams,
    ): Result<CheckoutSessionResponse> = confirmResult
}
