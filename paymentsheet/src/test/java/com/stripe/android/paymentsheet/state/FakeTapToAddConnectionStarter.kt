package com.stripe.android.paymentsheet.state

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeTapToAddConnectionStarter private constructor(
    override val isSupported: Boolean = false,
    val startCalls: Turbine<Unit> = Turbine(),
) : TapToAddConnectionStarter {

    override fun start() {
        startCalls.add(Unit)
    }

    fun ensureAllEventsConsumed() {
        startCalls.ensureAllEventsConsumed()
    }

    class Scenario(
        val connectionStarter: TapToAddConnectionStarter,
        val startCalls: ReceiveTurbine<Unit>,
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
