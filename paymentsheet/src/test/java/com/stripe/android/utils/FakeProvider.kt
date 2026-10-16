package com.stripe.android.utils

import app.cash.turbine.Turbine
import javax.inject.Provider

internal class FakeProvider<T>(
    var value: T,
) : Provider<T> {
    val getCalls = Turbine<Unit>()

    override fun get(): T {
        getCalls.add(Unit)
        return value
    }

    fun ensureAllEventsConsumed() {
        getCalls.ensureAllEventsConsumed()
    }
}
