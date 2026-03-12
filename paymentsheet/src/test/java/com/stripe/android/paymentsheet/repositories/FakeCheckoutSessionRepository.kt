package com.stripe.android.paymentsheet.repositories

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.checkout.Address
import com.stripe.android.paymentelement.CheckoutSessionPreview

internal class FakeCheckoutSessionRepository(
    var initResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
    var confirmResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
    var detachResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
    var applyPromotionCodeResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
    var updateLineItemQuantityResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
    var selectShippingRateResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
    var updateShippingAddressResult: Result<CheckoutSessionResponse> = Result.failure(NotImplementedError()),
) : CheckoutSessionRepository {

    private val _initRequests = Turbine<String>()
    val initRequests: ReceiveTurbine<String> = _initRequests

    private val _detachRequests = Turbine<DetachRequest>()
    val detachRequests: ReceiveTurbine<DetachRequest> = _detachRequests

    override suspend fun init(
        sessionId: String,
    ): Result<CheckoutSessionResponse> {
        _initRequests.add(sessionId)
        return initResult
    }

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

    override suspend fun applyPromotionCode(
        sessionId: String,
        promotionCode: String,
    ): Result<CheckoutSessionResponse> = applyPromotionCodeResult

    override suspend fun updateLineItemQuantity(
        sessionId: String,
        lineItemId: String,
        quantity: Int,
    ): Result<CheckoutSessionResponse> {
        return updateLineItemQuantityResult
    }

    override suspend fun selectShippingRate(
        sessionId: String,
        shippingRateId: String,
    ): Result<CheckoutSessionResponse> = selectShippingRateResult

    @OptIn(CheckoutSessionPreview::class)
    override suspend fun updateShippingAddress(
        sessionId: String,
        address: Address.State,
    ): Result<CheckoutSessionResponse> = updateShippingAddressResult

    data class DetachRequest(
        val sessionId: String,
        val paymentMethodId: String,
    )
}
