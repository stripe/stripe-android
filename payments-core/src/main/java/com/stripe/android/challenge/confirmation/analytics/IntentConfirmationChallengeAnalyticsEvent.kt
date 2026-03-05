package com.stripe.android.challenge.confirmation.analytics

import com.stripe.android.core.networking.AnalyticsEvent

internal sealed interface IntentConfirmationChallengeAnalyticsEvent : AnalyticsEvent {
    val params: Map<String, Any?>

    class Start(val captchaVendorName: String) : IntentConfirmationChallengeAnalyticsEvent {
        override val params: Map<String, Any?>
            get() = mapOf(FIELD_CAPTCHA_VENDOR_NAME to captchaVendorName)
        override val eventName = "elements.intent_confirmation_challenge.start"
    }

    class Success(val duration: Float, val captchaVendorName: String) : IntentConfirmationChallengeAnalyticsEvent {
        override val params: Map<String, Any?>
            get() = mapOf(
                FIELD_DURATION to duration,
                FIELD_CAPTCHA_VENDOR_NAME to captchaVendorName
            )
        override val eventName = "elements.intent_confirmation_challenge.success"
    }

    class Error(
        val duration: Float,
        val errorType: String?,
        val errorCode: String?,
        val fromBridge: Boolean,
        val captchaVendorName: String
    ) : IntentConfirmationChallengeAnalyticsEvent {
        override val params: Map<String, Any?>
            get() = mapOf(
                FIELD_DURATION to duration,
                FIELD_ERROR_TYPE to errorType,
                FIELD_ERROR_CODE to errorCode,
                FIELD_FROM_BRIDGE to fromBridge,
                FIELD_CAPTCHA_VENDOR_NAME to captchaVendorName
            )
        override val eventName = "elements.intent_confirmation_challenge.error"
    }

    class Cancel(val duration: Float, val captchaVendorName: String) : IntentConfirmationChallengeAnalyticsEvent {
        override val params: Map<String, Any?>
            get() = mapOf(
                FIELD_DURATION to duration,
                FIELD_CAPTCHA_VENDOR_NAME to captchaVendorName
            )
        override val eventName = "elements.intent_confirmation_challenge.cancel"
    }

    class WebViewLoaded(val duration: Float, val captchaVendorName: String) : IntentConfirmationChallengeAnalyticsEvent {
        override val params: Map<String, Any?>
            get() = mapOf(
                FIELD_DURATION to duration,
                FIELD_CAPTCHA_VENDOR_NAME to captchaVendorName
            )
        override val eventName = "elements.intent_confirmation_challenge.web_view_loaded"
    }

    companion object {
        internal const val FIELD_DURATION = "duration"
        internal const val FIELD_ERROR_TYPE = "error_type"
        internal const val FIELD_ERROR_CODE = "error_code"
        internal const val FIELD_FROM_BRIDGE = "from_bridge"
        internal const val FIELD_CAPTCHA_VENDOR_NAME = "captcha_vendor_name"
    }
}
