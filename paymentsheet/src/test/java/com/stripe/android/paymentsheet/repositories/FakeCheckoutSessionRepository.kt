package com.stripe.android.paymentsheet.repositories

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeCheckoutSessionRepository(
    var initResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
    var confirmResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
    var detachResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
) : CheckoutSessionRepository {

    private val _detachRequests = Turbine<DetachRequest>()
    val detachRequests: ReceiveTurbine<DetachRequest> = _detachRequests

    override suspend fun init(
        sessionId: String,
    ): Result<CheckoutSessionResponse> = initResult

    override suspend fun confirm(
        id: String,
        params: ConfirmCheckoutSessionParams,
    ): Result<CheckoutSessionResponse> = confirmResult

    override suspend fun detachPaymentMethod(
        sessionId: String,
        paymentMethodId: String,
    ): Result<CheckoutSessionResponse> {
        _detachRequests.add(
            DetachRequest(
                sessionId = sessionId,
                paymentMethodId = paymentMethodId,
            )
        )
        return detachResult
    }

    data class DetachRequest(
        val sessionId: String,
        val paymentMethodId: String,
    )
}
