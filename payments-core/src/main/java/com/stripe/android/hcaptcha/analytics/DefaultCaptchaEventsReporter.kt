package com.stripe.android.hcaptcha.analytics

import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.hcaptcha.HCaptchaException
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal class DefaultCaptchaEventsReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val errorReporter: ErrorReporter
) : CaptchaEventsReporter {

    override fun init(siteKey: String) {
        fireEvent(CaptchaAnalyticsEvent.Init(siteKey))
    }

    override fun execute(siteKey: String) {
        fireEvent(CaptchaAnalyticsEvent.Execute(siteKey))
    }

    override fun success(
        siteKey: String,
        resultImmediatelyAvailable: Boolean,
        duration: Duration?
    ) {
        fireEvent(
            event = CaptchaAnalyticsEvent.Success(resultImmediatelyAvailable, siteKey),
            additionalParams = durationInSecondsFromStart(duration)
        )
    }

    override fun error(
        error: Throwable?,
        siteKey: String,
        resultImmediatelyAvailable: Boolean,
        duration: Duration?
    ) {
        when (error) {
            is HCaptchaException -> {
                errorReporter.report(ErrorReporter.ExpectedErrorEvent.HCAPTCHA_FAILURE)
            }
            else -> {
                errorReporter.report(ErrorReporter.UnexpectedErrorEvent.HCAPTCHA_UNEXPECTED_FAILURE)
            }
        }

        fireEvent(
            event = CaptchaAnalyticsEvent.Error(error, resultImmediatelyAvailable, siteKey),
            additionalParams = durationInSecondsFromStart(duration)
        )
    }

    private fun fireEvent(
        event: CaptchaAnalyticsEvent,
        additionalParams: Map<String, Any> = emptyMap()
    ) {
        analyticsRequestExecutor.executeAsync(
            analyticsRequestFactory.createRequest(
                event = event,
                additionalParams = event.params + additionalParams
            )
        )
    }

    private fun durationInSecondsFromStart(duration: Duration?): Map<String, Float> {
        return duration?.let {
            mapOf("duration" to it.toDouble(DurationUnit.MILLISECONDS).toFloat())
        } ?: emptyMap()
    }
}
