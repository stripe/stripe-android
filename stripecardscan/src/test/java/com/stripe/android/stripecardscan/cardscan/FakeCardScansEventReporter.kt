package com.stripe.android.stripecardscan.cardscan

import app.cash.turbine.Turbine
import com.stripe.android.stripecardscan.scanui.CancellationReason

class FakeCardScansEventReporter private constructor() : CardScanEventsReporter {
    private val scanStartedTurbine = Turbine<Unit>()
    private val scanSucceededTurbine = Turbine<Unit>()
    private val scanFailedTurbine = Turbine<Throwable?>()
    private val scanCancelledTurbine = Turbine<Unit>()
    private val scanMlKitFoundPanTurbine = Turbine<Unit>()
    private val scanMlKitFoundExpTurbine = Turbine<Unit>()
    private val scanDarkniteFoundPanTurbine = Turbine<Unit>()
    private val scanModelsDisagreeTurbine = Turbine<Unit>()

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

    override fun scanMlKitFoundPan() {
        scanMlKitFoundPanTurbine.add(Unit)
    }

    override fun scanMlKitFoundExp() {
        scanMlKitFoundExpTurbine.add(Unit)
    }

    override fun scanDarkniteFoundPan() {
        scanDarkniteFoundPanTurbine.add(Unit)
    }

    override fun scanModelsDisagree() {
        scanModelsDisagreeTurbine.add(Unit)
    }

    private fun ensureAllEventsConsumed() {
        scanStartedTurbine.ensureAllEventsConsumed()
        scanSucceededTurbine.ensureAllEventsConsumed()
        scanFailedTurbine.ensureAllEventsConsumed()
        scanCancelledTurbine.ensureAllEventsConsumed()
        scanMlKitFoundPanTurbine.ensureAllEventsConsumed()
        scanMlKitFoundExpTurbine.ensureAllEventsConsumed()
        scanDarkniteFoundPanTurbine.ensureAllEventsConsumed()
        scanModelsDisagreeTurbine.ensureAllEventsConsumed()
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

        suspend fun awaitMlKitFoundPan() {
            return eventsReporter.scanMlKitFoundPanTurbine.awaitItem()
        }

        suspend fun awaitMlKitFoundExp() {
            return eventsReporter.scanMlKitFoundExpTurbine.awaitItem()
        }

        suspend fun awaitDarkniteFoundPan() {
            return eventsReporter.scanDarkniteFoundPanTurbine.awaitItem()
        }

        suspend fun awaitModelsDisagree() {
            return eventsReporter.scanModelsDisagreeTurbine.awaitItem()
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
