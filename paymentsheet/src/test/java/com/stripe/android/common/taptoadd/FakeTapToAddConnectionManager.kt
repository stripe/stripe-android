package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeTapToAddConnectionManager private constructor(
    override val isSupported: Boolean,
    override val isConnected: Boolean,
    val awaitResult: Result<Boolean>,
) : TapToAddConnectionManager {
    private val connectCalls = Turbine<Unit>()
    private val awaitCalls = Turbine<Unit>()

    override fun connect() {
        connectCalls.add(Unit)
    }

    override suspend fun await(): Result<Boolean> {
        awaitCalls.add(Unit)

        return awaitResult
    }

    class Scenario(
        val connectCalls: ReceiveTurbine<Unit>,
        val awaitCalls: ReceiveTurbine<Unit>,
        val tapToAddConnectionManager: TapToAddConnectionManager
    )

    companion object {
        suspend fun test(
            isSupported: Boolean,
            isConnected: Boolean,
            awaitResult: Result<Boolean> = Result.success(false),
            block: suspend Scenario.() -> Unit
        ) {
            val tapToAddConnectionManager = FakeTapToAddConnectionManager(isSupported, isConnected, awaitResult)

            block(
                Scenario(
                    connectCalls = tapToAddConnectionManager.connectCalls,
                    awaitCalls = tapToAddConnectionManager.awaitCalls,
                    tapToAddConnectionManager = tapToAddConnectionManager,
                )
            )

            tapToAddConnectionManager.connectCalls.ensureAllEventsConsumed()
            tapToAddConnectionManager.awaitCalls.ensureAllEventsConsumed()
        }

        fun noOp(
            isSupported: Boolean,
            isConnected: Boolean,
            awaitResult: Result<Boolean> = Result.success(false),
        ) = FakeTapToAddConnectionManager(isSupported, isConnected, awaitResult)
    }
}
