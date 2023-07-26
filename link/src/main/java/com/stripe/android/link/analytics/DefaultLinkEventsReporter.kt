package com.stripe.android.link.analytics

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class DefaultLinkEventsReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    @IOContext private val workContext: CoroutineContext,
    private val logger: Logger
) : LinkEventsReporter {
    private var signupStartMillis: Long? = null

    override fun onInlineSignupCheckboxChecked() {
        fireEvent(LinkEvent.SignUpCheckboxChecked)
    }

    override fun onSignupStarted(isInline: Boolean) {
        signupStartMillis = System.currentTimeMillis()
        fireEvent(LinkEvent.SignUpStart)
    }

    override fun onSignupCompleted(isInline: Boolean) {
        fireEvent(LinkEvent.SignUpComplete, durationInSecondsFromStart(signupStartMillis))
        signupStartMillis = null
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

    private fun durationInSecondsFromStart(start: Long?): Map<String, String>? {
        return start?.let {
            System.currentTimeMillis() - it
        }?.takeIf { it > 0 }?.let {
            mapOf("duration" to (it / 1000f).toString())
        }
    }

    private fun fireEvent(
        event: LinkEvent,
        additionalParams: Map<String, String>? = null
    ) {
        logger.debug("Link event: ${event.eventName} $additionalParams")
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                paymentAnalyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = additionalParams.orEmpty(),
                )
            )
        }
    }
}
