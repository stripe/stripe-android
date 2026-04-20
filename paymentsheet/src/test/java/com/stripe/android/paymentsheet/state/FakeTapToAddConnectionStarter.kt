package com.stripe.android.paymentsheet.state

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.common.model.CommonConfiguration

internal class FakeTapToAddConnectionStarter private constructor(
    override val isSupported: Boolean = false,
) : TapToAddConnectionStarter {
    private val startCalls: Turbine<StartCall> = Turbine()

    override fun start(config: CommonConfiguration) {
        startCalls.add(StartCall(config))
    }

    fun ensureAllEventsConsumed() {
        startCalls.ensureAllEventsConsumed()
    }

    data class StartCall(
        val config: CommonConfiguration
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
        ): FakeTapToAddConnectionStarter = FakeTapToAddConnectionStarter(isSupported = isSupported)
    }
}
