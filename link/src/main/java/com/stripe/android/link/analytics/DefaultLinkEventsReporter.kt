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

    override fun onSignupFlowPresented() {
        fireEvent(LinkEvent.SignUpFlowPresented)
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

    override fun on2FAStart() {
        fireEvent(LinkEvent.TwoFAStart)
    }

    override fun on2FAStartFailure() {
        fireEvent(LinkEvent.TwoFAStartFailure)
    }

    override fun on2FAComplete() {
        fireEvent(LinkEvent.TwoFAComplete)
    }

    override fun on2FAFailure() {
        fireEvent(LinkEvent.TwoFAFailure)
    }

    override fun on2FACancel() {
        fireEvent(LinkEvent.TwoFACancel)
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

    override fun onPopupError(exception: Throwable) {
        val params = mapOf("error" to (exception.message ?: exception.toString()))
        fireEvent(LinkEvent.PopupError, params)
    }

    override fun onPopupLogout() {
        fireEvent(LinkEvent.PopupLogout)
    }

    private fun durationInSecondsFromStart(start: Long?) = start?.let {
        System.currentTimeMillis() - it
    }?.takeIf { it > 0 }?.let {
        mapOf("duration" to it / 1000f)
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
