package com.stripe.android.stripecardscan.cardscan

import app.cash.turbine.Turbine
import com.stripe.android.stripecardscan.scanui.CancellationReason

class FakeCardScansEventReporter private constructor() : CardScanEventsReporter {
    private val scanStartedTurbine = Turbine<Unit>()
    private val scanSucceededTurbine = Turbine<Unit>()
    private val scanFailedTurbine = Turbine<Throwable?>()
    private val scanCancelledTurbine = Turbine<Unit>()

    override fun scanStarted() {
        scanStartedTurbine.add(Unit)
    }

    override fun scanSucceeded() {
        scanSucceededTurbine.add(Unit)
    }

    override fun scanFailed(error: Throwable?) {
        scanFailedTurbine.add(error)
    }

    override fun scanCancelled(reason: CancellationReason) {
        scanCancelledTurbine.add(Unit)
    }

    private fun ensureAllEventsConsumed() {
        scanStartedTurbine.ensureAllEventsConsumed()
        scanSucceededTurbine.ensureAllEventsConsumed()
        scanFailedTurbine.ensureAllEventsConsumed()
        scanCancelledTurbine.ensureAllEventsConsumed()
    }

    class Scenario(
        val eventsReporter: FakeCardScansEventReporter,
    ) {
        suspend fun awaitScanStarted() {
            return eventsReporter.scanStartedTurbine.awaitItem()
        }

        suspend fun awaitScanSucceeded() {
            return eventsReporter.scanSucceededTurbine.awaitItem()
        }

        suspend fun awaitScanFailed(): Throwable? {
            return eventsReporter.scanFailedTurbine.awaitItem()
        }

        suspend fun awaitScanCancelled() {
            return eventsReporter.scanCancelledTurbine.awaitItem()
        }
    }

    companion object {
        suspend fun test(
            block: suspend Scenario.() -> Unit
        ) {
            val eventsReporter = FakeCardScansEventReporter()
            val scenario = Scenario(eventsReporter)
            block(scenario)
            scenario.eventsReporter.ensureAllEventsConsumed()
        }
    }
}
