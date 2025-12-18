@file:OptIn(PaymentMethodMessagingElementPreview::class)

package com.stripe.android.paymentmethodmessaging.element.analytics

import androidx.compose.ui.graphics.toArgb
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElement
import com.stripe.android.paymentmethodmessaging.element.PaymentMethodMessagingElementPreview
import com.stripe.android.uicore.StripeThemeDefaults
import kotlin.time.Duration

internal sealed class PaymentMethodMessagingEvent : AnalyticsEvent {
    abstract val additionalParams: Map<String, Any?>

    class Init : PaymentMethodMessagingEvent() {
        override val additionalParams: Map<String, Any?> = emptyMap()
        override val eventName: String = PMME_INIT
    }

    class LoadStarted(
        val configuration: PaymentMethodMessagingElement.Configuration.State
    ) : PaymentMethodMessagingEvent() {
        override val eventName: String = PMME_LOAD_STARTED
        override val additionalParams: Map<String, Any?> = buildMap {
            put(FIELD_REQUESTED_PAYMENT_METHODS, configuration.paymentMethodTypes?.joinToString(",") { it.code })
            put(FIELD_AMOUNT, configuration.amount)
            put(FIELD_CURRENCY, configuration.currency)
            put(FIELD_REQUESTED_LOCALE, configuration.locale)
            put(FIELD_COUNTRY_CODE, configuration.countryCode)
        }
    }

    class LoadSucceeded(
        val paymentMethods: List<String>,
        val contentType: ContentType,
        val duration: Duration?
    ) : PaymentMethodMessagingEvent() {
        override val eventName: String = PMME_LOAD_SUCCEEDED
        override val additionalParams: Map<String, Any?> = buildMap {
            put(FIELD_PAYMENT_METHODS, paymentMethods.joinToString(","))
            put(FIELD_CONTENT_TYPE, contentType.type)
            put(FIELD_DURATION, duration)
        }
    }

    class LoadFailed(
        val error: Throwable,
        val duration: Duration?
    ) : PaymentMethodMessagingEvent() {
        override val eventName: String = PMME_LOAD_FAILED
        override val additionalParams: Map<String, Any?> = buildMap {
            put(FIELD_DURATION, duration)
            put(FIELD_ERROR_MESSAGE, error.message)
        }
    }

    class ElementDisplayed(
        val appearance: PaymentMethodMessagingElement.Appearance.State
    ) : PaymentMethodMessagingEvent() {
        override val eventName: String = PMME_DISPLAYED
        override val additionalParams: Map<String, Any?> = buildMap {
            put(FIELD_APPEARANCE, appearance.toAnalyticsValue())
        }
    }

    class ElementTapped : PaymentMethodMessagingEvent() {
        override val eventName: String = PMME_TAPPED
        override val additionalParams: Map<String, Any?> = emptyMap()
    }

    internal fun PaymentMethodMessagingElement.Appearance.State.toAnalyticsValue(): Map<String, Any?> {
        val setFont = this.font != null
        val setTextColor = this.colors.textColor != null
        val setIconColor = this.colors.infoIconColor != null
        val setTheme = this.theme != PaymentMethodMessagingElement.Appearance.Theme.LIGHT
        return buildMap {
            put(FIELD_FONT, setFont)
            put(FIELD_TEXT_COLOR, setTextColor)
            put(FIELD_ICON_COLOR, setIconColor)
            put(FIELD_STYLE, setTheme)
        }
    }

    internal companion object {
        const val PMME_INIT = "payment_method_messaging_element_init"
        const val PMME_LOAD_STARTED = "payment_method_messaging_element_load_started"
        const val PMME_LOAD_SUCCEEDED = "payment_method_messaging_element_load_succeeded"
        const val PMME_LOAD_FAILED = "payment_method_messaging_element_load_failed"
        const val PMME_DISPLAYED = "payment_method_messaging_element_displayed"
        const val PMME_TAPPED = "payment_method_messaging_element_tapped"
        const val FIELD_REQUESTED_PAYMENT_METHODS = "requested_payment_methods"
        const val FIELD_PAYMENT_METHODS = "payment_methods"
        const val FIELD_AMOUNT = "amount"
        const val FIELD_CURRENCY = "currency"
        const val FIELD_REQUESTED_LOCALE = "requested_locale"
        const val FIELD_COUNTRY_CODE = "country_code"
        const val FIELD_CONTENT_TYPE = "content_type"
        const val FIELD_DURATION = "duration"
        const val FIELD_ERROR_MESSAGE = "error_message"
        const val FIELD_APPEARANCE = "appearance"
        const val FIELD_FONT = "font"
        const val FIELD_TEXT_COLOR = "text_color"
        const val FIELD_ICON_COLOR = "info_icon_color"
        const val FIELD_STYLE = "style"
    }
}

internal enum class ContentType(val type: String) {
    SINGLE_PARTNER("single_partner"),
    MULTI_PARTNER("multi_partner"),
    NO_CONTENT("no_content")
}
