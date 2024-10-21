package com.stripe.android.link.analytics

import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.safeAnalyticsMessage
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.analytics.ErrorReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal class DefaultLinkEventsReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    private val errorReporter: ErrorReporter,
    @IOContext private val workContext: CoroutineContext,
    private val logger: Logger,
    private val durationProvider: DurationProvider,
) : LinkEventsReporter {
    override fun onInvalidSessionState(state: LinkEventsReporter.SessionState) {
        val params = mapOf(FIELD_SESSION_STATE to state.analyticsValue)

        errorReporter.report(ErrorReporter.UnexpectedErrorEvent.LINK_INVALID_SESSION_STATE)
        fireEvent(LinkEvent.SignUpFailureInvalidSessionState, params)
    }

    override fun onInlineSignupCheckboxChecked() {
        fireEvent(LinkEvent.SignUpCheckboxChecked)
    }

    override fun onSignupFlowPresented() {
        fireEvent(LinkEvent.SignUpFlowPresented)
    }

    override fun onSignupStarted(isInline: Boolean) {
        durationProvider.start(DurationProvider.Key.LinkSignup)
        fireEvent(LinkEvent.SignUpStart)
    }

    override fun onSignupCompleted(isInline: Boolean) {
        val duration = durationProvider.end(DurationProvider.Key.LinkSignup)
        fireEvent(LinkEvent.SignUpComplete, durationInSecondsFromStart(duration))
    }

    override fun onSignupFailure(isInline: Boolean, error: Throwable) {
        val preferredParams = if (error is APIException) {
            error.stripeError?.message?.let {
                mapOf(FIELD_ERROR_MESSAGE to it)
            }
        } else {
            null
        }

        val params = (preferredParams ?: mapOf(FIELD_ERROR_MESSAGE to error.safeAnalyticsMessage))
            .plus(ErrorReporter.getAdditionalParamsFromError(error))

        fireEvent(LinkEvent.SignUpFailure, params)
    }

    override fun onAccountLookupFailure(error: Throwable) {
        val params = mapOf(FIELD_ERROR_MESSAGE to error.safeAnalyticsMessage).plus(
            ErrorReporter.getAdditionalParamsFromError(error)
        )

        fireEvent(LinkEvent.AccountLookupFailure, params)
    }

    override fun on2FAStartFailure() {
        fireEvent(LinkEvent.TwoFAStartFailure)
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
        val params = mapOf(FIELD_ERROR_MESSAGE to error.safeAnalyticsMessage)

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

    private val LinkEventsReporter.SessionState.analyticsValue
        get() = when (this) {
            LinkEventsReporter.SessionState.RequiresSignUp -> VALUE_REQUIRES_SIGN_UP
            LinkEventsReporter.SessionState.RequiresVerification -> VALUE_REQUIRES_VERIFICATION
            LinkEventsReporter.SessionState.Verified -> VALUE_VERIFIED
        }

    private companion object {
        private const val FIELD_SESSION_STATE = "sessionState"
        private const val VALUE_REQUIRES_SIGN_UP = "requiresSignUp"
        private const val VALUE_REQUIRES_VERIFICATION = "requiresVerification"
        private const val VALUE_VERIFIED = "verified"

        private const val FIELD_ERROR_MESSAGE = "error_message"
    }
}
