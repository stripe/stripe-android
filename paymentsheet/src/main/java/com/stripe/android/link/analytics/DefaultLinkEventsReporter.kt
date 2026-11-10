package com.stripe.android.link.analytics

import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.safeAnalyticsMessage
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.payments.core.analytics.ErrorReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * @param checkoutSessionId attached to every event. Events that can fire from the singleton reporter, which is shared
 * across Link configurations, also take it per call; a non-null per-call value takes precedence.
 */
internal class DefaultLinkEventsReporter(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    private val errorReporter: ErrorReporter,
    private val workContext: CoroutineContext,
    private val logger: Logger,
    private val durationProvider: DurationProvider,
    private val checkoutSessionId: String?,
) : LinkEventsReporter {
    override fun onInvalidSessionState(state: LinkEventsReporter.SessionState) {
        val params = mapOf(FIELD_SESSION_STATE to state.analyticsValue)

        errorReporter.report(ErrorReporter.UnexpectedErrorEvent.LINK_INVALID_SESSION_STATE)
        fireEvent(LinkEvent.SignUpFailureInvalidSessionState, params)
    }

    override fun onInlineSignupCheckboxChecked(checkoutSessionId: String?) {
        fireEvent(LinkEvent.SignUpCheckboxChecked, checkoutSessionId = checkoutSessionId)
    }

    override fun onSignupFlowPresented() {
        fireEvent(LinkEvent.SignUpFlowPresented)
    }

    override fun onSignupStarted(isInline: Boolean, checkoutSessionId: String?) {
        durationProvider.start(DurationProvider.Key.LinkSignup)
        fireEvent(LinkEvent.SignUpStart, checkoutSessionId = checkoutSessionId)
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

    override fun onEmailSuggestionAccepted() {
        fireEvent(LinkEvent.EmailSuggestionAccepted)
    }

    override fun onAccountLookupFailure(error: Throwable) {
        val params = mapOf(FIELD_ERROR_MESSAGE to error.safeAnalyticsMessage).plus(
            ErrorReporter.getAdditionalParamsFromError(error)
        )

        fireEvent(LinkEvent.AccountLookupFailure, params)
    }

    override fun onAccountLookupComplete() {
        fireEvent(LinkEvent.AccountLookupComplete)
    }

    override fun onAccountRefreshFailure(error: Throwable) {
        val params = mapOf(FIELD_ERROR_MESSAGE to error.safeAnalyticsMessage).plus(
            ErrorReporter.getAdditionalParamsFromError(error)
        )

        fireEvent(LinkEvent.AccountRefreshFailure, params)
    }

    override fun on2FAStart(verificationType: String) {
        fireEvent(LinkEvent.TwoFAStart, mapOf("verification_type" to verificationType))
    }

    override fun on2FAStartFailure(verificationType: String) {
        fireEvent(LinkEvent.TwoFAStartFailure, mapOf("verification_type" to verificationType))
    }

    override fun on2FAComplete(verificationType: String) {
        fireEvent(LinkEvent.TwoFAComplete, mapOf("verification_type" to verificationType))
    }

    override fun on2FAFailure(verificationType: String) {
        fireEvent(LinkEvent.TwoFAFailure, mapOf("verification_type" to verificationType))
    }

    override fun on2FACancel() {
        fireEvent(LinkEvent.TwoFACancel)
    }

    override fun on2FAResendCode(verificationType: String, checkoutSessionId: String?) {
        fireEvent(
            LinkEvent.TwoFAResendCode(verificationType),
            mapOf("verification_type" to verificationType),
            checkoutSessionId,
        )
    }

    override fun onPopupShow(checkoutSessionId: String?) {
        fireEvent(LinkEvent.PopupShow, checkoutSessionId = checkoutSessionId)
    }

    override fun onPopupSuccess(checkoutSessionId: String?) {
        fireEvent(LinkEvent.PopupSuccess, checkoutSessionId = checkoutSessionId)
    }

    override fun onPopupCancel(checkoutSessionId: String?) {
        fireEvent(LinkEvent.PopupCancel, checkoutSessionId = checkoutSessionId)
    }

    override fun onPopupError(error: Throwable, checkoutSessionId: String?) {
        val params = mapOf(FIELD_ERROR_MESSAGE to error.safeAnalyticsMessage)

        fireEvent(LinkEvent.PopupError, params, checkoutSessionId)
    }

    override fun onPopupLogout(checkoutSessionId: String?) {
        fireEvent(LinkEvent.PopupLogout, checkoutSessionId = checkoutSessionId)
    }

    override fun onPopupSkipped(checkoutSessionId: String?) {
        fireEvent(LinkEvent.PopupSkipped, checkoutSessionId = checkoutSessionId)
    }

    private fun durationInSecondsFromStart(duration: Duration?): Map<String, Float>? {
        return duration?.let {
            mapOf("duration" to it.toDouble(DurationUnit.SECONDS).toFloat())
        }
    }

    private fun fireEvent(
        event: LinkEvent,
        additionalParams: Map<String, Any>? = null,
        checkoutSessionId: String? = null,
    ) {
        val params = (additionalParams ?: emptyMap()) +
            (checkoutSessionId ?: this.checkoutSessionId)?.let { mapOf(FIELD_CHECKOUT_SESSION_ID to it) }.orEmpty()
        logger.debug("Link event: ${event.eventName} $params")
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                paymentAnalyticsRequestFactory.createRequest(
                    event,
                    params
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
        private const val FIELD_CHECKOUT_SESSION_ID = "checkout_session_id"
    }
}
