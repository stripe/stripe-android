package com.stripe.android.utils

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.state.PaymentMethodRefresher

internal class FakePaymentMethodRefresher private constructor(
    val paymentMethodsResult: Result<List<PaymentMethod>>,
) : PaymentMethodRefresher {
    private val refreshCalls = Turbine<RefreshCall>()

    override suspend fun refresh(metadata: PaymentMethodMetadata): Result<List<PaymentMethod>> {
        refreshCalls.add(RefreshCall(metadata))
        return paymentMethodsResult
    }

    class RefreshCall(
        val metadata: PaymentMethodMetadata,
    )

    class Scenario(
        val refresher: FakePaymentMethodRefresher,
        val refreshCalls: ReceiveTurbine<RefreshCall>,
    )

    companion object {
        suspend fun test(
            paymentMethodsResult: Result<List<PaymentMethod>> = Result.success(emptyList()),
            block: suspend Scenario.() -> Unit
        ) {
            val refresher = FakePaymentMethodRefresher(paymentMethodsResult)

            block(Scenario(refresher, refresher.refreshCalls))

            refresher.refreshCalls.ensureAllEventsConsumed()
        }

        fun noOp(
            paymentMethodsResult: Result<List<PaymentMethod>> = Result.success(emptyList())
        ): FakePaymentMethodRefresher {
            return FakePaymentMethodRefresher(paymentMethodsResult)
        }
    }
}
