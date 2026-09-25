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
        reportInvalidSessionState(state, publishableKey = null)
    }

    override fun onInvalidSessionState(state: LinkEventsReporter.SessionState, publishableKey: String) {
        reportInvalidSessionState(state, publishableKey)
    }

    private fun reportInvalidSessionState(state: LinkEventsReporter.SessionState, publishableKey: String?) {
        val params = mapOf(FIELD_SESSION_STATE to state.analyticsValue)

        errorReporter.report(ErrorReporter.UnexpectedErrorEvent.LINK_INVALID_SESSION_STATE)
        fireEvent(LinkEvent.SignUpFailureInvalidSessionState, params, publishableKey)
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
        reportSignupCompleted(publishableKey = null)
    }

    override fun onSignupCompleted(isInline: Boolean, publishableKey: String) {
        reportSignupCompleted(publishableKey)
    }

    private fun reportSignupCompleted(publishableKey: String?) {
        val duration = durationProvider.end(DurationProvider.Key.LinkSignup)
        fireEvent(LinkEvent.SignUpComplete, durationInSecondsFromStart(duration), publishableKey)
    }

    override fun onSignupFailure(isInline: Boolean, error: Throwable) {
        reportSignupFailure(error, publishableKey = null)
    }

    override fun onSignupFailure(isInline: Boolean, error: Throwable, publishableKey: String) {
        reportSignupFailure(error, publishableKey)
    }

    private fun reportSignupFailure(error: Throwable, publishableKey: String?) {
        val preferredParams = if (error is APIException) {
            error.stripeError?.message?.let {
                mapOf(FIELD_ERROR_MESSAGE to it)
            }
        } else {
            null
        }

        val params = (preferredParams ?: mapOf(FIELD_ERROR_MESSAGE to error.safeAnalyticsMessage))
            .plus(ErrorReporter.getAdditionalParamsFromError(error))

        fireEvent(LinkEvent.SignUpFailure, params, publishableKey)
    }

    override fun onEmailSuggestionAccepted() {
        fireEvent(LinkEvent.EmailSuggestionAccepted)
    }

    override fun onAccountLookupFailure(error: Throwable) {
        reportAccountLookupFailure(error, publishableKey = null)
    }

    override fun onAccountLookupFailure(error: Throwable, publishableKey: String) {
        reportAccountLookupFailure(error, publishableKey)
    }

    private fun reportAccountLookupFailure(error: Throwable, publishableKey: String?) {
        val params = mapOf(FIELD_ERROR_MESSAGE to error.safeAnalyticsMessage).plus(
            ErrorReporter.getAdditionalParamsFromError(error)
        )

        fireEvent(LinkEvent.AccountLookupFailure, params, publishableKey)
    }

    override fun onAccountLookupComplete() {
        fireEvent(LinkEvent.AccountLookupComplete, publishableKey = null)
    }

    override fun onAccountLookupComplete(publishableKey: String) {
        fireEvent(LinkEvent.AccountLookupComplete, publishableKey = publishableKey)
    }

    override fun onAccountRefreshFailure(error: Throwable) {
        reportAccountRefreshFailure(error, publishableKey = null)
    }

    override fun onAccountRefreshFailure(error: Throwable, publishableKey: String) {
        reportAccountRefreshFailure(error, publishableKey)
    }

    private fun reportAccountRefreshFailure(error: Throwable, publishableKey: String?) {
        val params = mapOf(FIELD_ERROR_MESSAGE to error.safeAnalyticsMessage).plus(
            ErrorReporter.getAdditionalParamsFromError(error)
        )

        fireEvent(LinkEvent.AccountRefreshFailure, params, publishableKey)
    }

    override fun on2FAStart() {
        fireEvent(LinkEvent.TwoFAStart, publishableKey = null)
    }

    override fun on2FAStart(publishableKey: String) {
        fireEvent(LinkEvent.TwoFAStart, publishableKey = publishableKey)
    }

    override fun on2FAStartFailure() {
        fireEvent(LinkEvent.TwoFAStartFailure, publishableKey = null)
    }

    override fun on2FAStartFailure(publishableKey: String) {
        fireEvent(LinkEvent.TwoFAStartFailure, publishableKey = publishableKey)
    }

    override fun on2FAComplete() {
        fireEvent(LinkEvent.TwoFAComplete, publishableKey = null)
    }

    override fun on2FAComplete(publishableKey: String) {
        fireEvent(LinkEvent.TwoFAComplete, publishableKey = publishableKey)
    }

    override fun on2FAFailure() {
        fireEvent(LinkEvent.TwoFAFailure, publishableKey = null)
    }

    override fun on2FAFailure(publishableKey: String) {
        fireEvent(LinkEvent.TwoFAFailure, publishableKey = publishableKey)
    }

    override fun on2FACancel() {
        fireEvent(LinkEvent.TwoFACancel)
    }

    override fun on2FAResendCode(verificationType: String) {
        fireEvent(
            LinkEvent.TwoFAResendCode(verificationType),
            mapOf("verification_type" to verificationType)
        )
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
        additionalParams: Map<String, Any>? = null,
        publishableKey: String? = null,
    ) {
        logger.debug("Link event: ${event.eventName} $additionalParams")
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                paymentAnalyticsRequestFactory.createRequest(
                    event,
                    additionalParams ?: emptyMap(),
                    publishableKeyOverride = publishableKey,
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
