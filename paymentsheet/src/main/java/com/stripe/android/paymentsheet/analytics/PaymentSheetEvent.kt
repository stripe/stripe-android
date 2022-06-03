package com.stripe.android.paymentsheet.analytics

import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.ui.core.PaymentsThemeDefaults

internal sealed class PaymentSheetEvent : AnalyticsEvent {
    abstract val additionalParams: Map<String, Any>

    class Init(
        private val mode: EventReporter.Mode,
        private val configuration: PaymentSheet.Configuration?
    ) : PaymentSheetEvent() {
        override val eventName: String
            get() {
                val configValue = listOfNotNull(
                    FIELD_CUSTOMER.takeIf { configuration?.customer != null },
                    FIELD_GOOGLE_PAY.takeIf { configuration?.googlePay != null }
                ).takeUnless { it.isEmpty() }?.joinToString(separator = "_") ?: "default"
                return formatEventName(mode, "init_$configValue")
            }
        override val additionalParams: Map<String, Any>
            get() {
                val primaryButtonConfig = configuration?.appearance?.primaryButton
                val primaryButtonConfigMap = mapOf(
                    FIELD_COLORS_LIGHT to (
                        primaryButtonConfig?.colorsLight
                            != PaymentSheet.PrimaryButtonColors.defaultLight
                        ),
                    FIELD_COLORS_DARK to (
                        primaryButtonConfig?.colorsDark
                            != PaymentSheet.PrimaryButtonColors.defaultDark
                        ),
                    FIELD_CORNER_RADIUS to (primaryButtonConfig?.shape?.cornerRadiusDp != null),
                    FIELD_BORDER_WIDTH to (primaryButtonConfig?.shape?.borderStrokeWidthDp != null),
                    FIELD_FONT to (primaryButtonConfig?.typography?.fontResId != null)
                )
                val appearanceConfigMap = mutableMapOf(
                    FIELD_COLORS_LIGHT to (
                        configuration?.appearance?.colorsLight
                            != PaymentSheet.Colors.defaultLight
                        ),
                    FIELD_COLORS_DARK to (
                        configuration?.appearance?.colorsDark
                            != PaymentSheet.Colors.defaultDark
                        ),
                    FIELD_CORNER_RADIUS to (
                        configuration?.appearance?.shapes?.cornerRadiusDp
                            != PaymentsThemeDefaults.shapes.cornerRadius
                        ),
                    FIELD_BORDER_WIDTH to (
                        configuration?.appearance?.shapes?.borderStrokeWidthDp
                            != PaymentsThemeDefaults.shapes.borderStrokeWidth
                        ),
                    FIELD_FONT to (configuration?.appearance?.typography?.fontResId != null),
                    FIELD_SIZE_SCALE_FACTOR to (
                        configuration?.appearance?.typography?.sizeScaleFactor
                            != PaymentsThemeDefaults.typography.fontSizeMultiplier
                        ),
                    FIELD_PRIMARY_BUTTON to primaryButtonConfigMap
                )
                // We add a usage field to make queries easier.
                val usedPrimaryButtonApi = primaryButtonConfigMap.values.contains(true)
                val usedAppearanceApi = appearanceConfigMap
                    .values.filterIsInstance<Boolean>().contains(true)

                appearanceConfigMap[FIELD_APPEARANCE_USAGE] =
                    usedAppearanceApi || usedPrimaryButtonApi

                val configurationMap = mapOf(
                    FIELD_CUSTOMER to (configuration?.customer != null),
                    FIELD_GOOGLE_PAY to (configuration?.googlePay != null),
                    FIELD_PRIMARY_BUTTON_COLOR to (configuration?.primaryButtonColor != null),
                    FIELD_BILLING to (configuration?.defaultBillingDetails != null),
                    FIELD_DELAYED_PMS to (configuration?.allowsDelayedPaymentMethods),
                    FIELD_APPEARANCE to appearanceConfigMap
                )
                return mapOf(FIELD_MOBILE_PAYMENT_ELEMENT_CONFIGURATION to configurationMap)
            }
    }

    class Dismiss(
        mode: EventReporter.Mode
    ) : PaymentSheetEvent() {
        override val eventName: String = formatEventName(mode, "dismiss")
        override val additionalParams: Map<String, Any> = mapOf()
    }

    class ShowNewPaymentOptionForm(
        mode: EventReporter.Mode
    ) : PaymentSheetEvent() {
        override val eventName: String = formatEventName(mode, "sheet_newpm_show")
        override val additionalParams: Map<String, Any> = mapOf()
    }

    class ShowExistingPaymentOptions(
        mode: EventReporter.Mode
    ) : PaymentSheetEvent() {
        override val eventName: String = formatEventName(mode, "sheet_savedpm_show")
        override val additionalParams: Map<String, Any> = mapOf()
    }

    class SelectPaymentOption(
        mode: EventReporter.Mode,
        paymentSelection: PaymentSelection?
    ) : PaymentSheetEvent() {
        override val eventName: String =
            formatEventName(mode, "paymentoption_${analyticsValue(paymentSelection)}_select")
        override val additionalParams: Map<String, Any> = mapOf()
    }

    class Payment(
        mode: EventReporter.Mode,
        result: Result,
        paymentSelection: PaymentSelection?
    ) : PaymentSheetEvent() {
        override val eventName: String =
            formatEventName(mode, "payment_${analyticsValue(paymentSelection)}_$result")
        override val additionalParams: Map<String, Any> = mapOf()

        enum class Result(private val code: String) {
            Success("success"),
            Failure("failure");

            override fun toString(): String = code
        }
    }

    internal companion object {
        private fun analyticsValue(
            paymentSelection: PaymentSelection?
        ) = when (paymentSelection) {
            PaymentSelection.GooglePay -> "googlepay"
            is PaymentSelection.Saved -> "savedpm"
            is PaymentSelection.New -> "newpm"
            else -> "unknown"
        }

        private fun formatEventName(mode: EventReporter.Mode, eventName: String): String {
            return "mc_${mode}_$eventName"
        }

        const val FIELD_CUSTOMER = "customer"
        const val FIELD_GOOGLE_PAY = "googlepay"
        const val FIELD_PRIMARY_BUTTON_COLOR = "primary_button_color"
        const val FIELD_BILLING = "default_billing_details"
        const val FIELD_DELAYED_PMS = "allows_delayed_payment_methods"
        const val FIELD_MOBILE_PAYMENT_ELEMENT_CONFIGURATION = "mpe_config"
        const val FIELD_APPEARANCE = "appearance"
        const val FIELD_APPEARANCE_USAGE = "usage"
        const val FIELD_COLORS_LIGHT = "colorsLight"
        const val FIELD_COLORS_DARK = "colorsDark"
        const val FIELD_CORNER_RADIUS = "corner_radius"
        const val FIELD_BORDER_WIDTH = "border_width"
        const val FIELD_FONT = "font"
        const val FIELD_SIZE_SCALE_FACTOR = "size_scale_factor"
        const val FIELD_PRIMARY_BUTTON = "primary_button"
    }
}
