package com.stripe.android.link.analytics

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal class DefaultLinkEventsReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    @IOContext private val workContext: CoroutineContext,
    private val logger: Logger,
    private val durationProvider: DurationProvider,
) : LinkEventsReporter {

    override fun onInlineSignupCheckboxChecked() {
        fireEvent(LinkEvent.SignUpCheckboxChecked)
    }

    override fun onSignupStarted(isInline: Boolean) {
        durationProvider.start(DurationProvider.Key.LinkSignup)
        fireEvent(LinkEvent.SignUpStart)
    }

    override fun onSignupCompleted(isInline: Boolean) {
        val duration = durationProvider.end(DurationProvider.Key.LinkSignup)
        fireEvent(LinkEvent.SignUpComplete, durationInSecondsFromStart(duration))
    }

    override fun onSignupFailure(isInline: Boolean) {
        fireEvent(LinkEvent.SignUpFailure)
    }

    override fun onAccountLookupFailure() {
        fireEvent(LinkEvent.AccountLookupFailure)
    }

    override fun onPopupShow() {
        fireEvent(LinkEvent.PopupShow)
    }

    override fun onPopupSuccess() {
        fireEvent(LinkEvent.PopupSuccess)
    }

    override fun onPopupCancel() {
        fireEvent(LinkEvent.PopupCancel)
    }

    override fun onPopupError(error: Throwable) {
        val params = mapOf("error" to (error.message ?: error.toString()))
        fireEvent(LinkEvent.PopupError, params)
    }

    override fun onPopupLogout() {
        fireEvent(LinkEvent.PopupLogout)
    }

    override fun onPopupSkipped() {
        fireEvent(LinkEvent.PopupSkipped)
    }

    private fun durationInSecondsFromStart(duration: Duration?): Map<String, Float>? {
        return duration?.let {
            mapOf("duration" to it.toDouble(DurationUnit.SECONDS).toFloat())
        }
    }

    private fun fireEvent(
        event: LinkEvent,
        additionalParams: Map<String, Any>? = null
    ) {
        logger.debug("Link event: ${event.eventName} $additionalParams")
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                paymentAnalyticsRequestFactory.createRequest(
                    event,
                    additionalParams ?: emptyMap()
                )
            )
        }
    }
}
