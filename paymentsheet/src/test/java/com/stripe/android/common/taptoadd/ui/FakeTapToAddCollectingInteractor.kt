package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

internal class FakeTapToAddCollectingInteractor : TapToAddCollectingInteractor {
    private val _onClose = Turbine<Unit>()
    val onClose: ReceiveTurbine<Unit> = _onClose

    override fun close() {
        _onClose.add(Unit)
    }

    fun validate() {
        _onClose.ensureAllEventsConsumed()
    }

    class Factory(
        val interactor: FakeTapToAddCollectingInteractor = FakeTapToAddCollectingInteractor()
    ) : TapToAddCollectingInteractor.Factory {
        private val _createCalls = Turbine<Unit>()
        val createCalls: ReceiveTurbine<Unit> = _createCalls

        override fun create(): TapToAddCollectingInteractor {
            _createCalls.add(Unit)

            return interactor
        }

        fun validate() {
            _createCalls.ensureAllEventsConsumed()
        }
    }
}
