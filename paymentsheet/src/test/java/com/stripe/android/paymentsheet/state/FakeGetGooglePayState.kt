package com.stripe.android.paymentsheet.state

import app.cash.turbine.Turbine
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.model.ElementsSession
import kotlinx.coroutines.Deferred

internal class FakeGetGooglePayState(
    private val result: GooglePayState,
) : GetGooglePayState {
    val calls = Turbine<Call>()

    override suspend fun invoke(
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
        initializationMode: PaymentElementLoader.InitializationMode,
        isGooglePaySupportedOnDevice: Deferred<Boolean>,
        isGooglePaySupportedByConfiguration: Deferred<Boolean>,
    ): GooglePayState {
        calls.add(
            Call(
                configuration = configuration,
                elementsSession = elementsSession,
                initializationMode = initializationMode,
                isGooglePaySupportedOnDevice = isGooglePaySupportedOnDevice,
                isGooglePaySupportedByConfiguration = isGooglePaySupportedByConfiguration,
            )
        )
        return result
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }

    data class Call(
        val configuration: CommonConfiguration,
        val elementsSession: ElementsSession,
        val initializationMode: PaymentElementLoader.InitializationMode,
        val isGooglePaySupportedOnDevice: Deferred<Boolean>,
        val isGooglePaySupportedByConfiguration: Deferred<Boolean>,
    )
}
