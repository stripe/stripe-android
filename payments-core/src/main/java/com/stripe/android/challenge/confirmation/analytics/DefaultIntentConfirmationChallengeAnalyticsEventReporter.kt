package com.stripe.android.challenge.confirmation.analytics

import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.utils.DurationProvider
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal class DefaultIntentConfirmationChallengeAnalyticsEventReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val durationProvider: DurationProvider,
) : IntentConfirmationChallengeAnalyticsEventReporter {

    override fun onStart(captchaVendorName: String) {
        durationProvider.start(DurationProvider.Key.IntentConfirmationChallenge)
        durationProvider.start(DurationProvider.Key.IntentConfirmationChallengeWebViewLoaded)
        fireEvent(IntentConfirmationChallengeAnalyticsEvent.Start(captchaVendorName))
    }

    override fun onSuccess(captchaVendorName: String) {
        val duration = durationProvider.end(DurationProvider.Key.IntentConfirmationChallenge)
        fireEvent(IntentConfirmationChallengeAnalyticsEvent.Success(durationInMs(duration), captchaVendorName))
    }

    override fun onError(
        errorType: String?,
        errorCode: String?,
        fromBridge: Boolean,
        captchaVendorName: String
    ) {
        val duration = durationProvider.end(DurationProvider.Key.IntentConfirmationChallenge)
        fireEvent(
            event = IntentConfirmationChallengeAnalyticsEvent.Error(
                duration = durationInMs(duration),
                errorType = errorType,
                errorCode = errorCode,
                fromBridge = fromBridge,
                captchaVendorName = captchaVendorName,
            )
        )
    }

    override fun onCancel(captchaVendorName: String) {
        val duration = durationProvider.end(DurationProvider.Key.IntentConfirmationChallenge)
        fireEvent(IntentConfirmationChallengeAnalyticsEvent.Cancel(durationInMs(duration), captchaVendorName))
    }

    override fun onWebViewLoaded(captchaVendorName: String) {
        val duration = durationProvider.end(DurationProvider.Key.IntentConfirmationChallengeWebViewLoaded)
        fireEvent(IntentConfirmationChallengeAnalyticsEvent.WebViewLoaded(durationInMs(duration), captchaVendorName))
    }

    private fun fireEvent(event: IntentConfirmationChallengeAnalyticsEvent) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(
                event = event,
                additionalParams = event.params
            )
        )
    }

    private fun durationInMs(duration: Duration?) = duration?.toDouble(DurationUnit.MILLISECONDS)?.toFloat() ?: 0f
}
