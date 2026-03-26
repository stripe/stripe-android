package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeTapToAddConnectionManager private constructor(
    override val isSupported: Boolean,
    connectResults: List<Result<Unit>>,
) : TapToAddConnectionManager {
    private val queuedConnectResults = connectResults.toMutableList()

    val connectCalls = Turbine<Unit>()

    override suspend fun connect() {
        connectCalls.add(Unit)

        queuedConnectResults.removeFirst().getOrThrow()
    }

    class Scenario(
        val connectCalls: ReceiveTurbine<Unit>,
        val tapToAddConnectionManager: TapToAddConnectionManager
    )

    companion object {
        suspend fun test(
            isSupported: Boolean,
            connectResult: Result<Unit> = Result.success(Unit),
            block: suspend Scenario.() -> Unit
        ) {
           test(isSupported, listOf(connectResult), block)
        }

        suspend fun test(
            isSupported: Boolean,
            connectResults: List<Result<Unit>> = listOf(Result.success(Unit)),
            block: suspend Scenario.() -> Unit
        ) {
            val tapToAddConnectionManager = FakeTapToAddConnectionManager(isSupported, connectResults)

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
            connectResult: Result<Unit> = Result.success(Unit),
        ): FakeTapToAddConnectionManager {
            return FakeTapToAddConnectionManager(isSupported, listOf(connectResult))
        }
    }
}
