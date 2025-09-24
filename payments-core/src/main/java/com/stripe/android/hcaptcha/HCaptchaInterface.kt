@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.hcaptcha

import androidx.annotation.RestrictTo
import androidx.fragment.app.FragmentActivity
import com.stripe.android.hcaptcha.analytics.NoOpCaptchaEventsReporter
import com.stripe.hcaptcha.HCaptchaException
import kotlin.time.Duration

/**
 * Proxy to access hcaptcha android sdk code safely
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
suspend fun performPassiveHCaptcha(
    activity: FragmentActivity,
    siteKey: String,
    rqdata: String?
): String {
    val hCaptchaService = DefaultHCaptchaService(
        hCaptchaProvider = DefaultHCaptchaProvider(),
        captchaEventsReporter = NoOpCaptchaEventsReporter
    )
    val result = hCaptchaService.performPassiveHCaptcha(
        activity,
        siteKey,
        rqdata,
        timeout = Duration.INFINITE
    )
    return when (result) {
        is HCaptchaService.Result.Failure -> {
            if (result.error is HCaptchaException) {
                result.error.hCaptchaError.name
            } else {
                result.error::class.simpleName.orEmpty()
            }
        }
        is HCaptchaService.Result.Success -> result.token
    }
}
