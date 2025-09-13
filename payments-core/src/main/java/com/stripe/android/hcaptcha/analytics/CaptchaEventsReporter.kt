package com.stripe.android.hcaptcha.analytics

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CaptchaEventsReporter {
    fun init(siteKey: String)

    fun execute(siteKey: String)

    fun success(siteKey: String)

    fun error(error: Throwable?, siteKey: String)

    fun attachStart()

    fun attachEnd(siteKey: String, isReady: Boolean)
}
