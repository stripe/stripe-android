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
        resultImmediatelyAvailable: Boolean,
        override val siteKey: String
    ) : CaptchaAnalyticsEvent {
        override val eventName = "elements.captcha.passive.error"

        override val additionalParams = mapOf(
            FIELD_ERROR_MESSAGE to error?.message,
            FIELD_RESULT_IMMEDIATELY_AVAILABLE to resultImmediatelyAvailable
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Success(
        resultImmediatelyAvailable: Boolean,
        override val siteKey: String,
    ) : CaptchaAnalyticsEvent {
        override val eventName = "elements.captcha.passive.success"

        override val additionalParams = mapOf(
            FIELD_RESULT_IMMEDIATELY_AVAILABLE to resultImmediatelyAvailable
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Init(override val siteKey: String) : CaptchaAnalyticsEvent {
        override val eventName = "elements.captcha.passive.init"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Execute(override val siteKey: String) : CaptchaAnalyticsEvent {
        override val eventName = "elements.captcha.passive.execute"
    }

    companion object {
        internal const val FIELD_ERROR_MESSAGE = "error_message"
        internal const val FIELD_SITE_KEY = "site_key"
        internal const val FIELD_RESULT_IMMEDIATELY_AVAILABLE = "result_immediately_available"
    }
}
