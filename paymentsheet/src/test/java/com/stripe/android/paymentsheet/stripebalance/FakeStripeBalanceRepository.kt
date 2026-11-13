package com.stripe.android.paymentsheet.stripebalance

import app.cash.turbine.Turbine
import com.stripe.android.core.ApiConfiguration

internal class FakeStripeBalanceRepository(
    var result: suspend () -> Result<List<StripeBalanceFinancialAccount>> = { Result.success(emptyList()) },
) : StripeBalanceRepository {
    val listFinancialAccountsCalls = Turbine<ListFinancialAccountsCall>()

    override suspend fun listFinancialAccounts(
        customerSessionClientSecret: String,
        ephemeralKeySecret: String,
        amount: Long?,
        currency: String?,
        apiConfiguration: ApiConfiguration.State,
    ): Result<List<StripeBalanceFinancialAccount>> {
        listFinancialAccountsCalls.add(
            ListFinancialAccountsCall(
                customerSessionClientSecret = customerSessionClientSecret,
                ephemeralKeySecret = ephemeralKeySecret,
                amount = amount,
                currency = currency,
            )
        )
        return result()
    }

    fun ensureAllEventsConsumed() {
        listFinancialAccountsCalls.ensureAllEventsConsumed()
    }

    data class ListFinancialAccountsCall(
        val customerSessionClientSecret: String,
        val ephemeralKeySecret: String,
        val amount: Long?,
        val currency: String?,
    )
}
