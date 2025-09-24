package com.stripe.android.hcaptcha.analytics

internal object NoOpCaptchaEventsReporter : CaptchaEventsReporter {
    override fun init(siteKey: String) = Unit

    override fun execute(siteKey: String) = Unit

    override fun success(siteKey: String) = Unit

    override fun error(error: Throwable?, siteKey: String) = Unit

    override fun attachStart() = Unit

    override fun attachEnd(siteKey: String, isReady: Boolean) = Unit
}
