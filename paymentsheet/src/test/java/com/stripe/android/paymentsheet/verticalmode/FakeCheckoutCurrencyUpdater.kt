package com.stripe.android.paymentsheet.verticalmode

import app.cash.turbine.Turbine
import com.stripe.android.paymentsheet.PaymentSheet

internal class FakeCheckoutCurrencyUpdater(
    private val result: Result<CheckoutCurrencyUpdater.CurrencyUpdateResult> =
        Result.failure(IllegalStateException("No result configured")),
) : CheckoutCurrencyUpdater {

    val calls = Turbine<UpdateCurrencyCall>()

    override suspend fun updateCurrency(
        instancesKey: String,
        sessionId: String,
        currencyCode: String,
        config: PaymentSheet.Configuration,
        initializedViaCompose: Boolean,
    ): Result<CheckoutCurrencyUpdater.CurrencyUpdateResult> {
        calls.add(
            UpdateCurrencyCall(
                instancesKey = instancesKey,
                sessionId = sessionId,
                currencyCode = currencyCode,
                config = config,
                initializedViaCompose = initializedViaCompose,
            )
        )
        return result
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    data class UpdateCurrencyCall(
        val instancesKey: String,
        val sessionId: String,
        val currencyCode: String,
        val config: PaymentSheet.Configuration,
        val initializedViaCompose: Boolean,
    )
}
