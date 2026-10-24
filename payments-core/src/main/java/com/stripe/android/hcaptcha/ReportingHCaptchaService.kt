package com.stripe.android.hcaptcha

import androidx.fragment.app.FragmentActivity
import com.stripe.android.hcaptcha.analytics.CaptchaEventsReporter
import com.stripe.android.hcaptcha.analytics.ObservableCaptchaEventsReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal class ReportingHCaptchaService(
    private val service: HCaptchaService,
    private val observableCaptchaEventsReporter: ObservableCaptchaEventsReporter,
    private val captchaEventsReporter: CaptchaEventsReporter,
) : HCaptchaService {
    override suspend fun warmUp(activity: FragmentActivity, siteKey: String, rqData: String?) {
        reportEvents {
            service.warmUp(activity, siteKey, rqData)
        }
    }

    override suspend fun performPassiveHCaptcha(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?,
        tokenTimeoutSeconds: Int?,
    ): HCaptchaService.Result = reportEvents {
        service.performPassiveHCaptcha(activity, siteKey, rqData, tokenTimeoutSeconds)
    }

    private suspend fun <T> reportEvents(block: suspend () -> T): T = coroutineScope {
        val collection = launch(Dispatchers.Unconfined) {
            observableCaptchaEventsReporter.events.collect {
                observableCaptchaEventsReporter.reportTo(captchaEventsReporter)
            }
        }

        try {
            block()
        } finally {
            collection.cancel()
            observableCaptchaEventsReporter.reportTo(captchaEventsReporter)
        }
    }
}
