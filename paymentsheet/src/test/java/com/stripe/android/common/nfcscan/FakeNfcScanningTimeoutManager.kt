package com.stripe.android.common.nfcscan

import app.cash.turbine.Turbine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class FakeNfcScanningTimeoutManager(
    private val timeoutFlow: MutableSharedFlow<Unit> = MutableSharedFlow(extraBufferCapacity = 1),
) : NfcScanningTimeoutManager {
    val startCalls = Turbine<Unit>()
    val resetCalls = Turbine<Unit>()
    val cancelCalls = Turbine<Unit>()

    override val timeout: SharedFlow<Unit> = timeoutFlow.asSharedFlow()

    override fun start() {
        startCalls.add(Unit)
    }

    override fun reset() {
        resetCalls.add(Unit)
    }

    override fun cancel() {
        cancelCalls.add(Unit)
    }

    suspend fun emitTimeout() {
        timeoutFlow.emit(Unit)
    }

    fun ensureAllEventsConsumed() {
        startCalls.ensureAllEventsConsumed()
        resetCalls.ensureAllEventsConsumed()
        cancelCalls.ensureAllEventsConsumed()
    }
}
