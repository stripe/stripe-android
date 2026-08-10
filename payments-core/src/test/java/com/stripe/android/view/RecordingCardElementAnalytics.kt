package com.stripe.android.view

import android.content.Context
import android.os.Bundle
import app.cash.turbine.Turbine

internal class RecordingCardElementAnalytics : CardElementAnalytics {
    private val shownCalls = Turbine<Unit>()

    override fun reportShown(context: Context) {
        shownCalls.add(Unit)
    }

    override fun saveState(outState: Bundle) {
        // No-op
    }

    override fun restoreState(savedState: Bundle) {
        // No-op
    }

    suspend fun awaitShown() {
        shownCalls.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        shownCalls.ensureAllEventsConsumed()
    }
}
