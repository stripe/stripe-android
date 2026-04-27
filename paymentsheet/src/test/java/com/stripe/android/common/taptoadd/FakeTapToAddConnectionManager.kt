package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeTapToAddConnectionManager private constructor(
    override val isSupported: Boolean,
    connectResults: List<Result<Unit>>,
) : TapToAddConnectionManager {
    private val queuedConnectResults = connectResults.toMutableList()

    val connectCalls = Turbine<ConnectCall>()

    override suspend fun connect(config: TapToAddConnectionManager.ConnectionConfig) {
        connectCalls.add(ConnectCall(config))

        queuedConnectResults.removeFirst().getOrThrow()
    }

    data class ConnectCall(
        val config: TapToAddConnectionManager.ConnectionConfig,
    )

    class Scenario(
        val connectCalls: ReceiveTurbine<ConnectCall>,
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
