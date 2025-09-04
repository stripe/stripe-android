package com.stripe.android.hcaptcha.analytics

import androidx.annotation.RestrictTo
import kotlin.time.Duration

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CaptchaEventsReporter {
    fun init(siteKey: String)

    fun execute(siteKey: String)

    fun success(
        siteKey: String,
        resultImmediatelyAvailable: Boolean,
        duration: Duration?
    )

    fun error(
        error: Throwable?,
        siteKey: String,
        resultImmediatelyAvailable: Boolean,
        duration: Duration?
    )
}
