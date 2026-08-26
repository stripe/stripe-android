package com.stripe.android.paymentsheet.state

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.common.model.CommonConfiguration

internal class FakeTapToAddConnectionStarter private constructor(
    private val isSupportedValue: Boolean = false,
) : TapToAddConnectionStarter {
    private val startCalls: Turbine<StartCall> = Turbine()

    override fun isSupported(publishableKey: String, isLiveMode: Boolean): Boolean = isSupportedValue

    override fun start(config: CommonConfiguration, publishableKey: String, isLiveMode: Boolean) {
        startCalls.add(StartCall(config, publishableKey))
    }

    fun ensureAllEventsConsumed() {
        startCalls.ensureAllEventsConsumed()
    }

    data class StartCall(
        val config: CommonConfiguration,
        val publishableKey: String,
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
