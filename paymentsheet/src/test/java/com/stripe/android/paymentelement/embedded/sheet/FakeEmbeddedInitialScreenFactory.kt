package com.stripe.android.paymentelement.embedded.sheet

import app.cash.turbine.Turbine

internal class FakeEmbeddedInitialScreenFactory(
    var screens: List<EmbeddedNavigator.Screen> = emptyList(),
) : EmbeddedInitialScreenFactory {
    val createCalls = Turbine<Unit>()

    override fun create(): List<EmbeddedNavigator.Screen> {
        createCalls.add(Unit)
        return screens
    }

    fun validate() {
        createCalls.ensureAllEventsConsumed()
    }
}
