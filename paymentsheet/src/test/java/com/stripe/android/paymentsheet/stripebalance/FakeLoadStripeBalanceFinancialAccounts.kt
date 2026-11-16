package com.stripe.android.paymentsheet.stripebalance

import app.cash.turbine.Turbine
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal class FakeLoadStripeBalanceFinancialAccounts(
    private val result: StripeBalanceFinancialAccounts? = null,
) : LoadStripeBalanceFinancialAccounts {
    val calls = Turbine<Call>()

    override suspend fun invoke(
        initializationMode: PaymentElementLoader.InitializationMode,
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
        apiConfiguration: ApiConfiguration.State,
    ): StripeBalanceFinancialAccounts? {
        calls.add(
            Call(
                initializationMode = initializationMode,
                elementsSession = elementsSession,
                customerMetadata = customerMetadata,
                apiConfiguration = apiConfiguration,
            )
        )
        return result
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    data class Call(
        val initializationMode: PaymentElementLoader.InitializationMode,
        val elementsSession: ElementsSession,
        val customerMetadata: CustomerMetadata?,
        val apiConfiguration: ApiConfiguration.State,
    )
}
