package com.stripe.android.paymentsheet.analytics

import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.StripeThemeDefaults
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal sealed class PaymentSheetEvent : AnalyticsEvent {

    val params: Map<String, Any?>
        get() = standardParams(isDeferred, linkEnabled) + additionalParams

    protected abstract val isDeferred: Boolean
    protected abstract val linkEnabled: Boolean
    protected abstract val additionalParams: Map<String, Any?>

    class LoadStarted(
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_load_started"
        override val additionalParams: Map<String, Any?> = emptyMap()
    }

    class LoadSucceeded(
        duration: Duration?,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_load_succeeded"
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_DURATION to duration?.asSeconds,
        )
    }

    class LoadFailed(
        duration: Duration?,
        error: String,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_load_failed"
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_DURATION to duration?.asSeconds,
            FIELD_ERROR_MESSAGE to error,
        )
    }

    class ElementsSessionLoadFailed(
        error: String,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_elements_session_load_failed"
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_ERROR_MESSAGE to error,
        )
    }

    class Init(
        private val mode: EventReporter.Mode,
        private val configuration: PaymentSheet.Configuration,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {

        override val eventName: String
            get() {
                val configValue = listOfNotNull(
                    FIELD_CUSTOMER.takeIf { configuration.customer != null },
                    FIELD_GOOGLE_PAY.takeIf { configuration.googlePay != null }
                ).takeUnless { it.isEmpty() }?.joinToString(separator = "_") ?: "default"
                return formatEventName(mode, "init_$configValue")
            }

        override val additionalParams: Map<String, Any?>
            get() {
                val primaryButtonConfig = configuration.appearance.primaryButton
                val primaryButtonConfigMap = mapOf(
                    FIELD_COLORS_LIGHT to (
                        primaryButtonConfig.colorsLight
                            != PaymentSheet.PrimaryButtonColors.defaultLight
                        ),
                    FIELD_COLORS_DARK to (
                        primaryButtonConfig.colorsDark
                            != PaymentSheet.PrimaryButtonColors.defaultDark
                        ),
                    FIELD_CORNER_RADIUS to (primaryButtonConfig.shape.cornerRadiusDp != null),
                    FIELD_BORDER_WIDTH to (primaryButtonConfig.shape.borderStrokeWidthDp != null),
                    FIELD_FONT to (primaryButtonConfig.typography.fontResId != null)
                )
                val appearanceConfigMap = mutableMapOf(
                    FIELD_COLORS_LIGHT to (
                        configuration.appearance.colorsLight
                            != PaymentSheet.Colors.defaultLight
                        ),
                    FIELD_COLORS_DARK to (
                        configuration.appearance.colorsDark
                            != PaymentSheet.Colors.defaultDark
                        ),
                    FIELD_CORNER_RADIUS to (
                        configuration.appearance.shapes.cornerRadiusDp
                            != StripeThemeDefaults.shapes.cornerRadius
                        ),
                    FIELD_BORDER_WIDTH to (
                        configuration.appearance.shapes.borderStrokeWidthDp
                            != StripeThemeDefaults.shapes.borderStrokeWidth
                        ),
                    FIELD_FONT to (configuration.appearance.typography.fontResId != null),
                    FIELD_SIZE_SCALE_FACTOR to (
                        configuration.appearance.typography.sizeScaleFactor
                            != StripeThemeDefaults.typography.fontSizeMultiplier
                        ),
                    FIELD_PRIMARY_BUTTON to primaryButtonConfigMap
                )
                // We add a usage field to make queries easier.
                val usedPrimaryButtonApi = primaryButtonConfigMap.values.contains(true)
                val usedAppearanceApi = appearanceConfigMap
                    .values.filterIsInstance<Boolean>().contains(true)

                appearanceConfigMap[FIELD_APPEARANCE_USAGE] =
                    usedAppearanceApi || usedPrimaryButtonApi

                val billingDetailsCollectionConfigMap = mapOf(
                    FIELD_ATTACH_DEFAULTS to configuration
                        .billingDetailsCollectionConfiguration
                        .attachDefaultsToPaymentMethod,
                    FIELD_COLLECT_NAME to configuration
                        .billingDetailsCollectionConfiguration
                        .name
                        .name,
                    FIELD_COLLECT_EMAIL to configuration
                        .billingDetailsCollectionConfiguration
                        .email
                        .name,
                    FIELD_COLLECT_PHONE to configuration
                        .billingDetailsCollectionConfiguration
                        .phone
                        .name,
                    FIELD_COLLECT_ADDRESS to configuration
                        .billingDetailsCollectionConfiguration
                        .address
                        .name,
                )

                val preferredNetworks = configuration.preferredNetworks.takeIf { brands ->
                    brands.isNotEmpty()
                }?.joinToString { brand ->
                    brand.code
                }

                @Suppress("DEPRECATION")
                val configurationMap = mapOf(
                    FIELD_CUSTOMER to (configuration.customer != null),
                    FIELD_GOOGLE_PAY to (configuration.googlePay != null),
                    FIELD_PRIMARY_BUTTON_COLOR to (configuration.primaryButtonColor != null),
                    FIELD_BILLING to (configuration.defaultBillingDetails != null),
                    FIELD_DELAYED_PMS to (configuration.allowsDelayedPaymentMethods),
                    FIELD_APPEARANCE to appearanceConfigMap,
                    FIELD_BILLING_DETAILS_COLLECTION_CONFIGURATION to
                        billingDetailsCollectionConfigMap,
                    FIELD_PREFERRED_NETWORKS to preferredNetworks
                )
                return mapOf(
                    FIELD_MOBILE_PAYMENT_ELEMENT_CONFIGURATION to configurationMap,
                )
            }
    }

    class Dismiss(
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_dismiss"
        override val additionalParams: Map<String, Any> = emptyMap()
    }

    class ShowNewPaymentOptionForm(
        mode: EventReporter.Mode,
        currency: String?,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = formatEventName(mode, "sheet_newpm_show")
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_CURRENCY to currency,
        )
    }

    class ShowExistingPaymentOptions(
        mode: EventReporter.Mode,
        currency: String?,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = formatEventName(mode, "sheet_savedpm_show")
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_CURRENCY to currency,
        )
    }

    class SelectPaymentMethod(
        code: String,
        currency: String?,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_carousel_payment_method_tapped"
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_CURRENCY to currency,
            FIELD_SELECTED_LPM to code,
        )
    }

    class SelectPaymentOption(
        mode: EventReporter.Mode,
        paymentSelection: PaymentSelection?,
        currency: String?,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String =
            formatEventName(mode, "paymentoption_${analyticsValue(paymentSelection)}_select")
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_CURRENCY to currency,
        )
    }

    class PressConfirmButton(
        currency: String?,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_confirm_button_tapped"
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_CURRENCY to currency,
        )
    }

    class Payment(
        mode: EventReporter.Mode,
        private val result: Result,
        duration: Duration?,
        private val paymentSelection: PaymentSelection?,
        currency: String?,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        private val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
    ) : PaymentSheetEvent() {

        override val eventName: String =
            formatEventName(mode, "payment_${analyticsValue(paymentSelection)}_${result.analyticsValue}")

        override val additionalParams: Map<String, Any?> =
            mapOf(
                FIELD_DURATION to duration?.asSeconds,
                FIELD_CURRENCY to currency,
            ) + buildDeferredIntentConfirmationType() + selectedPaymentMethodType() + errorMessage()

        private fun buildDeferredIntentConfirmationType(): Map<String, String> {
            return deferredIntentConfirmationType?.let {
                mapOf(FIELD_DEFERRED_INTENT_CONFIRMATION_TYPE to it.value)
            }.orEmpty()
        }

        private fun selectedPaymentMethodType(): Map<String, String> {
            val code = when (paymentSelection) {
                is PaymentSelection.GooglePay -> "google_pay"
                is PaymentSelection.Link -> "link"
                is PaymentSelection.New -> paymentSelection.paymentMethodCreateParams.typeCode
                is PaymentSelection.Saved -> paymentSelection.paymentMethod.type?.code
                null -> null
            }

            return code?.let {
                mapOf(FIELD_SELECTED_LPM to it)
            }.orEmpty()
        }

        private fun errorMessage(): Map<String, String> {
            return when (result) {
                is Result.Success -> emptyMap()
                is Result.Failure -> mapOf(FIELD_ERROR_MESSAGE to result.error.analyticsValue)
            }
        }

        sealed interface Result {
            object Success : Result
            data class Failure(val error: PaymentSheetConfirmationError) : Result

            val analyticsValue: String
                get() = when (this) {
                    is Success -> "success"
                    is Failure -> "failure"
                }
        }
    }

    class LpmSerializeFailureEvent(
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "luxe_serialize_failure"
        override val additionalParams: Map<String, Any?> = emptyMap()
    }

    class AutofillEvent(
        type: String,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        private fun String.toSnakeCase() = replace(
            "(?<=.)(?=\\p{Upper})".toRegex(),
            "_"
        ).lowercase()

        override val eventName: String = "autofill_${type.toSnakeCase()}"
        override val additionalParams: Map<String, Any?> = emptyMap()
    }

    class ShowEditablePaymentOption(
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_open_edit_screen"

        override val additionalParams: Map<String, Any?> = emptyMap()
    }

    class HideEditablePaymentOption(
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_cancel_edit_screen"

        override val additionalParams: Map<String, Any?> = emptyMap()
    }

    class ShowPaymentOptionBrands(
        source: Source,
        selectedBrand: CardBrand,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_open_cbc_dropdown"

        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_CBC_EVENT_SOURCE to source.value,
            FIELD_SELECTED_CARD_BRAND to selectedBrand.code
        )

        enum class Source(val value: String) {
            Edit(VALUE_EDIT_CBC_EVENT_SOURCE), Add(VALUE_ADD_CBC_EVENT_SOURCE)
        }
    }

    class HidePaymentOptionBrands(
        source: Source,
        selectedBrand: CardBrand?,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_close_cbc_dropdown"

        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_CBC_EVENT_SOURCE to source.value,
            FIELD_SELECTED_CARD_BRAND to selectedBrand?.code
        )

        enum class Source(val value: String) {
            Edit(VALUE_EDIT_CBC_EVENT_SOURCE), Add(VALUE_ADD_CBC_EVENT_SOURCE)
        }
    }

    class UpdatePaymentOptionSucceeded(
        selectedBrand: CardBrand,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_update_card"

        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_SELECTED_CARD_BRAND to selectedBrand.code
        )
    }

    class UpdatePaymentOptionFailed(
        selectedBrand: CardBrand,
        error: Throwable,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_update_card_failed"

        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_SELECTED_CARD_BRAND to selectedBrand.code,
            FIELD_ERROR_MESSAGE to error.message,
        )
    }

    private fun standardParams(
        isDecoupled: Boolean,
        linkEnabled: Boolean,
    ): Map<String, Any?> = mapOf(
        FIELD_IS_DECOUPLED to isDecoupled,
        FIELD_LINK_ENABLED to linkEnabled,
    )

    internal companion object {
        private fun analyticsValue(
            paymentSelection: PaymentSelection?
        ) = when (paymentSelection) {
            PaymentSelection.GooglePay -> "googlepay"
            is PaymentSelection.Saved -> "savedpm"
            PaymentSelection.Link,
            is PaymentSelection.New.LinkInline -> "link"
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
        const val FIELD_PREFERRED_NETWORKS = "preferred_networks"
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
        const val FIELD_BILLING_DETAILS_COLLECTION_CONFIGURATION =
            "billing_details_collection_configuration"
        const val FIELD_IS_DECOUPLED = "is_decoupled"
        const val FIELD_DEFERRED_INTENT_CONFIRMATION_TYPE = "deferred_intent_confirmation_type"
        const val FIELD_ATTACH_DEFAULTS = "attach_defaults"
        const val FIELD_COLLECT_NAME = "name"
        const val FIELD_COLLECT_EMAIL = "email"
        const val FIELD_COLLECT_PHONE = "phone"
        const val FIELD_COLLECT_ADDRESS = "address"
        const val FIELD_DURATION = "duration"
        const val FIELD_LINK_ENABLED = "link_enabled"
        const val FIELD_CURRENCY = "currency"
        const val FIELD_SELECTED_LPM = "selected_lpm"
        const val FIELD_ERROR_MESSAGE = "error_message"
        const val FIELD_CBC_EVENT_SOURCE = "cbc_event_source"
        const val FIELD_SELECTED_CARD_BRAND = "selected_card_brand"

        const val VALUE_EDIT_CBC_EVENT_SOURCE = "edit"
        const val VALUE_ADD_CBC_EVENT_SOURCE = "add"
    }
}

private val Duration.asSeconds: Float
    get() = toDouble(DurationUnit.SECONDS).toFloat()
