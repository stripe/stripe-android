package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
internal class FakeStateHolder(
    state: TapToAddStateHolder.State?,
) : TapToAddStateHolder {
    private var _state: TapToAddStateHolder.State? = state
    override val state: TapToAddStateHolder.State?
        get() = _state

    private val _setStateCalls = Turbine<TapToAddStateHolder.State?>()
    val setStateCalls: ReceiveTurbine<TapToAddStateHolder.State?> = _setStateCalls

    override fun setState(state: TapToAddStateHolder.State?) {
        _state = state
        _setStateCalls.add(state)
    }

    fun validate() {
        _setStateCalls.ensureAllEventsConsumed()
    }
}
