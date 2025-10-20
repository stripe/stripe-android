package com.stripe.android.hcaptcha.analytics

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.AnalyticsEvent

internal sealed interface CaptchaAnalyticsEvent : AnalyticsEvent {
    val siteKey: String

    val params: Map<String, Any?>
        get() = mapOf(FIELD_SITE_KEY to siteKey) + additionalParams

    val additionalParams: Map<String, Any?>
        get() = emptyMap()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Error(
        error: Throwable?,
        override val siteKey: String
    ) : CaptchaAnalyticsEvent {
        override val eventName = "elements.captcha.passive.error"

        override val additionalParams = mapOf(
            FIELD_ERROR_MESSAGE to error?.message
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Success(override val siteKey: String) : CaptchaAnalyticsEvent {
        override val eventName = "elements.captcha.passive.success"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Init(override val siteKey: String) : CaptchaAnalyticsEvent {
        override val eventName = "elements.captcha.passive.init"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Execute(override val siteKey: String) : CaptchaAnalyticsEvent {
        override val eventName = "elements.captcha.passive.execute"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Attach(
        val isReady: Boolean,
        override val siteKey: String
    ) : CaptchaAnalyticsEvent {
        override val eventName = "elements.captcha.passive.attach"

        override val additionalParams = mapOf(
            FIELD_IS_READY to isReady
        )
    }

    companion object {
        internal const val FIELD_ERROR_MESSAGE = "error_message"
        internal const val FIELD_SITE_KEY = "site_key"
        internal const val FIELD_IS_READY = "is_ready"
    }
}
