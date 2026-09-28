package com.stripe.android.utils

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.state.PaymentMethodFilter

internal class FakePaymentMethodFilter private constructor(
    val filteredPaymentMethods: List<PaymentMethod>?,
) : PaymentMethodFilter {
    private val filterCalls = Turbine<FilterCall>()

    override suspend fun filter(
        paymentMethods: List<PaymentMethod>,
        params: PaymentMethodFilter.FilterParams
    ): List<PaymentMethod> {
        filterCalls.add(
            FilterCall(
                paymentMethods = paymentMethods,
                params = params,
            )
        )

        return filteredPaymentMethods ?: paymentMethods
    }

    class FilterCall(
        val paymentMethods: List<PaymentMethod>,
        val params: PaymentMethodFilter.FilterParams
    )

    class Scenario(
        val filterCalls: ReceiveTurbine<FilterCall>,
        val paymentMethodFilter: PaymentMethodFilter,
    )

    companion object {
        fun noOp(
            filteredPaymentMethods: List<PaymentMethod>? = null,
        ): PaymentMethodFilter = FakePaymentMethodFilter(filteredPaymentMethods)

        suspend fun test(
            filteredPaymentMethods: List<PaymentMethod>? = null,
            block: suspend Scenario.() -> Unit,
        ) {
            val paymentMethodFilter = FakePaymentMethodFilter(filteredPaymentMethods)

            block(Scenario(paymentMethodFilter.filterCalls, paymentMethodFilter))

            paymentMethodFilter.filterCalls.ensureAllEventsConsumed()
        }
    }
}
