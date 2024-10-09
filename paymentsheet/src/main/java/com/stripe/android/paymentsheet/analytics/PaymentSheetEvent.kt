package com.stripe.android.paymentsheet.analytics

import com.stripe.android.common.analytics.getExternalPaymentMethodsAnalyticsValue
import com.stripe.android.common.analytics.toAnalyticsMap
import com.stripe.android.common.analytics.toAnalyticsValue
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkMode
import com.stripe.android.model.analyticsValue
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.asPaymentSheetLoadingException
import com.stripe.android.utils.filterNotNullValues
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal sealed class PaymentSheetEvent : AnalyticsEvent {

    val params: Map<String, Any?>
        get() = standardParams(isDeferred, linkEnabled, googlePaySupported) + additionalParams

    protected abstract val isDeferred: Boolean
    protected abstract val linkEnabled: Boolean
    protected abstract val googlePaySupported: Boolean
    protected abstract val additionalParams: Map<String, Any?>

    class LoadStarted(
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
        initializedViaCompose: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_load_started"
        override val additionalParams: Map<String, Any?> = mapOf(FIELD_COMPOSE to initializedViaCompose)
    }

    class LoadSucceeded(
        paymentSelection: PaymentSelection?,
        initializationMode: PaymentSheet.InitializationMode,
        orderedLpms: List<String>,
        duration: Duration?,
        linkMode: LinkMode?,
        override val isDeferred: Boolean,
        override val googlePaySupported: Boolean,
        requireCvcRecollection: Boolean = false
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_load_succeeded"
        override val linkEnabled: Boolean = linkMode != null
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_DURATION to duration?.asSeconds,
            FIELD_SELECTED_LPM to paymentSelection.defaultAnalyticsValue,
            FIELD_INTENT_TYPE to initializationMode.defaultAnalyticsValue,
            FIELD_ORDERED_LPMS to orderedLpms.joinToString(","),
            FIELD_REQUIRE_CVC_RECOLLECTION to requireCvcRecollection
        ).plus(
            linkMode?.let { mode ->
                mapOf(FIELD_LINK_MODE to mode.analyticsValue)
            }.orEmpty()
        )

        private val PaymentSelection?.defaultAnalyticsValue: String
            get() = when (this) {
                is PaymentSelection.GooglePay -> "google_pay"
                is PaymentSelection.Link -> "link"
                is PaymentSelection.Saved -> paymentMethod.type?.code ?: "saved"
                else -> "none"
            }

        private val PaymentSheet.InitializationMode.defaultAnalyticsValue: String
            get() = when (this) {
                is PaymentSheet.InitializationMode.DeferredIntent -> {
                    when (this.intentConfiguration.mode) {
                        is PaymentSheet.IntentConfiguration.Mode.Payment -> "deferred_payment_intent"
                        is PaymentSheet.IntentConfiguration.Mode.Setup -> "deferred_setup_intent"
                    }
                }
                is PaymentSheet.InitializationMode.PaymentIntent -> "payment_intent"
                is PaymentSheet.InitializationMode.SetupIntent -> "setup_intent"
            }
    }

    class LoadFailed(
        duration: Duration?,
        error: Throwable,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_load_failed"
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_DURATION to duration?.asSeconds,
            FIELD_ERROR_MESSAGE to error.asPaymentSheetLoadingException.type,
        ).plus(ErrorReporter.getAdditionalParamsFromError(error))
    }

    class ElementsSessionLoadFailed(
        error: Throwable,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_elements_session_load_failed"
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_ERROR_MESSAGE to error.asPaymentSheetLoadingException.type,
        ).plus(ErrorReporter.getAdditionalParamsFromError(error))
    }

    class Init(
        private val mode: EventReporter.Mode,
        private val configuration: PaymentSheet.Configuration,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
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

                @Suppress("DEPRECATION")
                val configurationMap = mapOf(
                    FIELD_CUSTOMER to (configuration.customer != null),
                    FIELD_CUSTOMER_ACCESS_PROVIDER to (configuration.customer?.accessType?.analyticsValue),
                    FIELD_GOOGLE_PAY to (configuration.googlePay != null),
                    FIELD_PRIMARY_BUTTON_COLOR to (configuration.primaryButtonColor != null),
                    FIELD_BILLING to (configuration.defaultBillingDetails?.isFilledOut() == true),
                    FIELD_DELAYED_PMS to configuration.allowsDelayedPaymentMethods,
                    FIELD_APPEARANCE to configuration.appearance.toAnalyticsMap(),
                    FIELD_PAYMENT_METHOD_ORDER to configuration.paymentMethodOrder,
                    FIELD_ALLOWS_PAYMENT_METHODS_REQUIRING_SHIPPING_ADDRESS to
                        configuration.allowsPaymentMethodsRequiringShippingAddress,
                    FIELD_ALLOWS_REMOVAL_OF_LAST_SAVED_PAYMENT_METHOD to
                        configuration.allowsRemovalOfLastSavedPaymentMethod,
                    FIELD_BILLING_DETAILS_COLLECTION_CONFIGURATION to (
                        configuration.billingDetailsCollectionConfiguration.toAnalyticsMap()
                        ),
                    FIELD_PREFERRED_NETWORKS to configuration.preferredNetworks.toAnalyticsValue(),
                    FIELD_EXTERNAL_PAYMENT_METHODS to configuration.getExternalPaymentMethodsAnalyticsValue(),
                    FIELD_PAYMENT_METHOD_LAYOUT to configuration.paymentMethodLayout.toAnalyticsValue(),
                    FIELD_CARD_BRAND_ACCEPTANCE to configuration.cardBrandAcceptance.toAnalyticsValue(),
                )
                return mapOf(
                    FIELD_MOBILE_PAYMENT_ELEMENT_CONFIGURATION to configurationMap,
                )
            }
    }

    class Dismiss(
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_dismiss"
        override val additionalParams: Map<String, Any> = emptyMap()
    }

    class ShowNewPaymentOptions(
        mode: EventReporter.Mode,
        currency: String?,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
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
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = formatEventName(mode, "sheet_savedpm_show")
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_CURRENCY to currency,
        )
    }

    class SelectPaymentMethod(
        code: String,
        currency: String?,
        linkContext: String?,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_carousel_payment_method_tapped"
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_CURRENCY to currency,
            FIELD_SELECTED_LPM to code,
            FIELD_LINK_CONTEXT to linkContext,
        )
    }

    class SelectPaymentOption(
        mode: EventReporter.Mode,
        paymentSelection: PaymentSelection?,
        currency: String?,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String =
            formatEventName(mode, "paymentoption_${analyticsValue(paymentSelection)}_select")
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_CURRENCY to currency,
        )
    }

    class ShowPaymentOptionForm(
        code: String,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_form_shown"
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_SELECTED_LPM to code
        )
    }

    class PaymentOptionFormInteraction(
        code: String,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_form_interacted"
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_SELECTED_LPM to code
        )
    }

    class CardNumberCompleted(
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_card_number_completed"
        override val additionalParams: Map<String, Any?> = mapOf()
    }

    class PressConfirmButton(
        currency: String?,
        duration: Duration?,
        selectedLpm: String?,
        linkContext: String?,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_confirm_button_tapped"
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_DURATION to duration?.asSeconds,
            FIELD_CURRENCY to currency,
            FIELD_SELECTED_LPM to selectedLpm,
            FIELD_LINK_CONTEXT to linkContext,
        ).filterNotNullValues()
    }

    class Payment(
        mode: EventReporter.Mode,
        private val result: Result,
        duration: Duration?,
        paymentSelection: PaymentSelection?,
        currency: String?,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
        private val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
    ) : PaymentSheetEvent() {

        override val eventName: String =
            formatEventName(mode, "payment_${analyticsValue(paymentSelection)}_${result.analyticsValue}")

        override val additionalParams: Map<String, Any?> =
            mapOf(
                FIELD_DURATION to duration?.asSeconds,
                FIELD_CURRENCY to currency,
            ) + buildDeferredIntentConfirmationType() + paymentSelection.paymentMethodInfo() + errorInfo()

        private fun buildDeferredIntentConfirmationType(): Map<String, String> {
            return deferredIntentConfirmationType?.let {
                mapOf(FIELD_DEFERRED_INTENT_CONFIRMATION_TYPE to it.value)
            }.orEmpty()
        }

        private fun errorInfo(): Map<String, String> {
            return when (result) {
                is Result.Success -> emptyMap()
                is Result.Failure -> mapOf(
                    FIELD_ERROR_MESSAGE to result.error.analyticsValue,
                    FIELD_ERROR_CODE to result.error.errorCode,
                ).filterNotNullValues()
            }
        }

        sealed interface Result {
            data object Success : Result
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
        override val googlePaySupported: Boolean,
        val errorMessage: String?
    ) : PaymentSheetEvent() {
        override val eventName: String = "luxe_serialize_failure"
        override val additionalParams: Map<String, Any?> = mapOf(FIELD_ERROR_MESSAGE to errorMessage)
    }

    class AutofillEvent(
        type: String,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
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
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_open_edit_screen"

        override val additionalParams: Map<String, Any?> = emptyMap()
    }

    class HideEditablePaymentOption(
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_cancel_edit_screen"

        override val additionalParams: Map<String, Any?> = emptyMap()
    }

    class ShowPaymentOptionBrands(
        source: Source,
        selectedBrand: CardBrand,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
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
        override val googlePaySupported: Boolean,
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
        override val googlePaySupported: Boolean,
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
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_update_card_failed"

        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_SELECTED_CARD_BRAND to selectedBrand.code,
            FIELD_ERROR_MESSAGE to error.message,
        ).plus(ErrorReporter.getAdditionalParamsFromError(error))
    }

    class CannotProperlyReturnFromLinkAndLPMs(
        mode: EventReporter.Mode,
    ) : PaymentSheetEvent() {
        override val linkEnabled: Boolean = false
        override val isDeferred: Boolean = false
        override val googlePaySupported: Boolean = false

        override val eventName: String = formatEventName(mode, "cannot_return_from_link_and_lpms")

        override val additionalParams: Map<String, Any?> = mapOf()
    }

    private fun standardParams(
        isDecoupled: Boolean,
        linkEnabled: Boolean,
        googlePaySupported: Boolean,
    ): Map<String, Any?> = mapOf(
        FIELD_IS_DECOUPLED to isDecoupled,
        FIELD_LINK_ENABLED to linkEnabled,
        FIELD_GOOGLE_PAY_ENABLED to googlePaySupported,
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
        const val FIELD_CUSTOMER_ACCESS_PROVIDER = "customer_access_provider"
        const val FIELD_GOOGLE_PAY = "googlepay"
        const val FIELD_GOOGLE_PAY_ENABLED = "google_pay_enabled"
        const val FIELD_PRIMARY_BUTTON_COLOR = "primary_button_color"
        const val FIELD_BILLING = "default_billing_details"
        const val FIELD_PREFERRED_NETWORKS = "preferred_networks"
        const val FIELD_DELAYED_PMS = "allows_delayed_payment_methods"
        const val FIELD_MOBILE_PAYMENT_ELEMENT_CONFIGURATION = "mpe_config"
        const val FIELD_APPEARANCE = "appearance"
        const val FIELD_ALLOWS_PAYMENT_METHODS_REQUIRING_SHIPPING_ADDRESS =
            "allows_payment_methods_requiring_shipping_address"
        const val FIELD_ALLOWS_REMOVAL_OF_LAST_SAVED_PAYMENT_METHOD =
            "allows_removal_of_last_saved_payment_method"
        const val FIELD_BILLING_DETAILS_COLLECTION_CONFIGURATION =
            "billing_details_collection_configuration"
        const val FIELD_PAYMENT_METHOD_ORDER = "payment_method_order"
        const val FIELD_IS_DECOUPLED = "is_decoupled"
        const val FIELD_DEFERRED_INTENT_CONFIRMATION_TYPE = "deferred_intent_confirmation_type"
        const val FIELD_DURATION = "duration"
        const val FIELD_LINK_ENABLED = "link_enabled"
        const val FIELD_CURRENCY = "currency"
        const val FIELD_SELECTED_LPM = "selected_lpm"
        const val FIELD_ERROR_MESSAGE = "error_message"
        const val FIELD_ERROR_CODE = "error_code"
        const val FIELD_CBC_EVENT_SOURCE = "cbc_event_source"
        const val FIELD_SELECTED_CARD_BRAND = "selected_card_brand"
        const val FIELD_LINK_CONTEXT = "link_context"
        const val FIELD_EXTERNAL_PAYMENT_METHODS = "external_payment_methods"
        const val FIELD_PAYMENT_METHOD_LAYOUT = "payment_method_layout"
        const val FIELD_COMPOSE = "compose"
        const val FIELD_INTENT_TYPE = "intent_type"
        const val FIELD_LINK_MODE = "link_mode"
        const val FIELD_ORDERED_LPMS = "ordered_lpms"
        const val FIELD_REQUIRE_CVC_RECOLLECTION = "require_cvc_recollection"
        const val FIELD_CARD_BRAND_ACCEPTANCE = "card_brand_acceptance"

        const val VALUE_EDIT_CBC_EVENT_SOURCE = "edit"
        const val VALUE_ADD_CBC_EVENT_SOURCE = "add"

        const val MAX_EXTERNAL_PAYMENT_METHODS = 10
    }
}

private val Duration.asSeconds: Float
    get() = toDouble(DurationUnit.SECONDS).toFloat()

internal fun PaymentSelection?.code(): String? {
    return when (this) {
        is PaymentSelection.GooglePay -> "google_pay"
        is PaymentSelection.Link -> "link"
        is PaymentSelection.New -> paymentMethodCreateParams.typeCode
        is PaymentSelection.Saved -> paymentMethod.type?.code
        is PaymentSelection.ExternalPaymentMethod -> type
        null -> null
    }
}

private fun PaymentSelection?.paymentMethodInfo(): Map<String, String> {
    return mapOf(
        PaymentSheetEvent.FIELD_SELECTED_LPM to code(),
        PaymentSheetEvent.FIELD_LINK_CONTEXT to linkContext(),
    ).filterNotNullValues()
}

internal fun PaymentSelection?.linkContext(): String? {
    return when (this) {
        is PaymentSelection.Link -> "wallet"
        is PaymentSelection.New.USBankAccount -> {
            instantDebits?.let {
                if (it.linkMode == LinkMode.LinkCardBrand) {
                    "link_card_brand"
                } else {
                    "instant_debits"
                }
            }
        }
        is PaymentSelection.GooglePay,
        is PaymentSelection.New,
        is PaymentSelection.Saved,
        is PaymentSelection.ExternalPaymentMethod,
        null -> null
    }
}
