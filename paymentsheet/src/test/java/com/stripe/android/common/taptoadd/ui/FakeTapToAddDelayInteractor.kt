package com.stripe.android.common.taptoadd.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.model.CardBrand

internal class FakeTapToAddDelayInteractor(
    override val cardBrand: CardBrand = CardBrand.Visa,
    override val last4: String? = "4242",
) : TapToAddDelayInteractor {
    private val _onClose = Turbine<Unit>()
    val onClose: ReceiveTurbine<Unit> = _onClose

    override fun close() {
        _onClose.add(Unit)
    }

    fun ensureAllEventsConsumed() {
        _onClose.ensureAllEventsConsumed()
    }
}
