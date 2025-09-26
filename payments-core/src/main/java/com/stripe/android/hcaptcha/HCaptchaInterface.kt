@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.hcaptcha

import androidx.annotation.RestrictTo
import androidx.fragment.app.FragmentActivity
import com.stripe.hcaptcha.HCaptchaException

/**
 * Proxy to access hcaptcha android sdk code safely
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
suspend fun performPassiveHCaptcha(
    activity: FragmentActivity,
    siteKey: String,
    rqdata: String?,
    hCaptchaService: HCaptchaService
): String {
    val result = hCaptchaService.performPassiveHCaptcha(
        activity,
        siteKey,
        rqdata,
        timeout = null
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
