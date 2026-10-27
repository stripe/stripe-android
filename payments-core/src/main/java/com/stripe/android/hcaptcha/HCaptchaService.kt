package com.stripe.android.hcaptcha

import androidx.annotation.RestrictTo
import androidx.fragment.app.FragmentActivity
import com.stripe.android.hcaptcha.analytics.CaptchaEventsReporter

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface HCaptchaService {
    suspend fun warmUp(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?,
    )

    suspend fun performPassiveHCaptcha(
        activity: FragmentActivity,
        siteKey: String,
        rqData: String?,
        tokenTimeoutSeconds: Int?
    ): Result

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Result {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Success(val token: String) : Result

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class Failure(val error: Throwable) : Result
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun create(
            reporter: CaptchaEventsReporter,
        ): HCaptchaService {
            return DefaultHCaptchaService(
                hCaptchaProvider = DefaultHCaptchaProvider(),
                captchaEventsReporter = reporter,
                webViewStarter = DefaultWebViewStarter
            )
        }
    }
}
