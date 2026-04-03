package com.stripe.android.stripecardscan.cardscan

import app.cash.turbine.Turbine
import com.stripe.android.stripecardscan.scanui.CancellationReason

internal class FakeCardScansEventReporter private constructor() : CardScanEventsReporter {
    private val scanStartedTurbine = Turbine<Unit>()
    private val scanSucceededTurbine = Turbine<CardScanAnalyticsData?>()
    private val scanFailedTurbine = Turbine<Pair<Throwable?, CardScanAnalyticsData?>>()
    private val scanCancelledTurbine = Turbine<Pair<CancellationReason, CardScanAnalyticsData?>>()

    override fun scanStarted() {
        scanStartedTurbine.add(Unit)
    }

    override fun scanSucceeded(analyticsData: CardScanAnalyticsData?) {
        scanSucceededTurbine.add(analyticsData)
    }

    override fun scanFailed(error: Throwable?, analyticsData: CardScanAnalyticsData?) {
        scanFailedTurbine.add(Pair(error, analyticsData))
    }

    override fun scanCancelled(reason: CancellationReason, analyticsData: CardScanAnalyticsData?) {
        scanCancelledTurbine.add(Pair(reason, analyticsData))
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

        suspend fun awaitScanSucceeded(): CardScanAnalyticsData? {
            return eventsReporter.scanSucceededTurbine.awaitItem()
        }

        suspend fun awaitScanFailed(): Pair<Throwable?, CardScanAnalyticsData?> {
            return eventsReporter.scanFailedTurbine.awaitItem()
        }

        suspend fun awaitScanCancelled(): Pair<CancellationReason, CardScanAnalyticsData?> {
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
