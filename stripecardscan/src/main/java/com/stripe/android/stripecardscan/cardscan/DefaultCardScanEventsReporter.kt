package com.stripe.android.stripecardscan.cardscan

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.analytics.ErrorReporter
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class DefaultCardScanEventsReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val errorReporter: ErrorReporter,
    @IOContext private val workContext: CoroutineContext,
    private val durationProvider: DurationProvider,
): CardScanEventsReporter {
    override fun scanStarted() {
        TODO("Not yet implemented")
    }

    override fun scanSucceeded() {
        TODO("Not yet implemented")
    }

    override fun scanFailed(error: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun scanCancelled() {
        TODO("Not yet implemented")
    }
}