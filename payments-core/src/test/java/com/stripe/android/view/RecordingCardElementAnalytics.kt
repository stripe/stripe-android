package com.stripe.android.view

import android.content.Context
import android.os.Bundle
import app.cash.turbine.Turbine

internal class RecordingCardElementAnalytics : CardElementAnalytics {
    private val shownCalls = Turbine<Unit>()
    private val interactionCalls = Turbine<Unit>()
    private val formCompletedCalls = Turbine<Unit>()

    override fun reportShown(context: Context) {
        shownCalls.add(Unit)
    }

    override fun reportInteraction(context: Context) {
        interactionCalls.add(Unit)
    }

    override fun reportFormCompleted(context: Context) {
        formCompletedCalls.add(Unit)
    }

    override fun saveState(outState: Bundle) {
        // No-op
    }

    override fun restoreState(savedState: Bundle) {
        // No-op
    }

    suspend fun awaitInteraction() {
        interactionCalls.awaitItem()
    }

    suspend fun awaitFormCompleted() {
        formCompletedCalls.awaitItem()
    }

    suspend fun awaitShown() {
        shownCalls.awaitItem()
    }

    fun ensureAllEventsConsumed() {
        shownCalls.ensureAllEventsConsumed()
        interactionCalls.ensureAllEventsConsumed()
        formCompletedCalls.ensureAllEventsConsumed()
    }
}
