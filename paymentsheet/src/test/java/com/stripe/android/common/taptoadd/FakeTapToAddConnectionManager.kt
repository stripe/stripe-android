package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeTapToAddConnectionManager private constructor(
    override val isSupported: Boolean,
    override val isConnected: Boolean
) : TapToAddConnectionManager {
    private val connectCalls = Turbine<Unit>()

    override fun connect() {
        connectCalls.add(Unit)
    }

    class Scenario(
        val connectCalls: ReceiveTurbine<Unit>,
        val tapToAddConnectionManager: TapToAddConnectionManager
    )

    companion object {
        suspend fun test(
            isSupported: Boolean,
            isConnected: Boolean,
            block: suspend Scenario.() -> Unit
        ) {
            val tapToAddConnectionManager = FakeTapToAddConnectionManager(isSupported, isConnected)

            block(
                Scenario(
                    connectCalls = tapToAddConnectionManager.connectCalls,
                    tapToAddConnectionManager = tapToAddConnectionManager,
                )
            )

            tapToAddConnectionManager.connectCalls.ensureAllEventsConsumed()
        }

        fun noOp(
            isSupported: Boolean,
            isConnected: Boolean,
        ) = FakeTapToAddConnectionManager(isSupported, isConnected)
    }
}
