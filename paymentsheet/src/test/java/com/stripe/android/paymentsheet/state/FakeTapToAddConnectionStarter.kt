package com.stripe.android.paymentsheet.state

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.core.ApiConfiguration

internal class FakeTapToAddConnectionStarter private constructor(
    private val isSupportedValue: Boolean = false,
) : TapToAddConnectionStarter {
    private val startCalls: Turbine<StartCall> = Turbine()

    override fun isSupported(apiConfiguration: ApiConfiguration.State): Boolean = isSupportedValue

    override fun start(config: CommonConfiguration, apiConfiguration: ApiConfiguration.State) {
        startCalls.add(StartCall(config, apiConfiguration))
    }

    fun ensureAllEventsConsumed() {
        startCalls.ensureAllEventsConsumed()
    }

    data class StartCall(
        val config: CommonConfiguration,
        val apiConfiguration: ApiConfiguration.State,
    )

    class Scenario(
        val connectionStarter: TapToAddConnectionStarter,
        val startCalls: ReceiveTurbine<StartCall>,
    )

    companion object {
        suspend fun test(
            isSupported: Boolean = false,
            block: suspend Scenario.() -> Unit,
        ) {
            val starter = FakeTapToAddConnectionStarter(isSupported)

            block(
                Scenario(
                    connectionStarter = starter,
                    startCalls = starter.startCalls,
                )
            )

            starter.startCalls.ensureAllEventsConsumed()
        }

        fun create(
            isSupported: Boolean = false,
        ): FakeTapToAddConnectionStarter = FakeTapToAddConnectionStarter(isSupportedValue = isSupported)
    }
}
