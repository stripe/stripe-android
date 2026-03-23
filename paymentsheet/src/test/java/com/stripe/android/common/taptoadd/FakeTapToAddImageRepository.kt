package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

internal class FakeTapToAddImageRepository(
    private val onGet: TapToAddImageRepository.CardArt? = null,
    private val onLoad: Deferred<TapToAddImageRepository.CardArt?> = CompletableDeferred(null),
) : TapToAddImageRepository {
    private val _getCalls = Turbine<CardBrand>()
    val getCalls: ReceiveTurbine<CardBrand>
        get() = _getCalls

    private val _loadCalls = Turbine<CardBrand>()
    val loadCalls: ReceiveTurbine<CardBrand>
        get() = _loadCalls

    override fun get(cardBrand: CardBrand): TapToAddImageRepository.CardArt? {
        _getCalls.add(cardBrand)

        return onGet
    }

    override suspend fun load(cardBrand: CardBrand): Deferred<TapToAddImageRepository.CardArt?> {
        _loadCalls.add(cardBrand)

        return onLoad
    }

    fun validate() {
        _getCalls.ensureAllEventsConsumed()
        _loadCalls.ensureAllEventsConsumed()
    }
}
