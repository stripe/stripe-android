package com.stripe.android.customersheet.analytics

import com.stripe.android.common.analytics.toAnalyticsMap
import com.stripe.android.common.analytics.toAnalyticsValue
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetIntegration
import com.stripe.android.model.CardBrand

internal sealed class CustomerSheetEvent : AnalyticsEvent {

    abstract val additionalParams: Map<String, Any?>

    class Init(
        private val configuration: CustomerSheet.Configuration,
        integrationType: CustomerSheetIntegration.Type,
    ) : CustomerSheetEvent() {
        override val eventName: String = when (integrationType) {
            CustomerSheetIntegration.Type.CustomerSession -> CS_INIT_WITH_CUSTOMER_SESSION
            CustomerSheetIntegration.Type.CustomerAdapter -> CS_INIT_WITH_CUSTOMER_ADAPTER
        }
        override val additionalParams: Map<String, Any?> = buildMap {
            val configurationMap = mapOf(
                FIELD_GOOGLE_PAY_ENABLED to configuration.googlePayEnabled,
                FIELD_BILLING to configuration.defaultBillingDetails.isFilledOut(),
                FIELD_APPEARANCE to configuration.appearance.toAnalyticsMap(),
                FIELD_ALLOWS_REMOVAL_OF_LAST_SAVED_PAYMENT_METHOD to
                    configuration.allowsRemovalOfLastSavedPaymentMethod,
                FIELD_PAYMENT_METHOD_ORDER to configuration.paymentMethodOrder,
                FIELD_BILLING_DETAILS_COLLECTION_CONFIGURATION to (
                    configuration.billingDetailsCollectionConfiguration.toAnalyticsMap()
                    ),
                FIELD_PREFERRED_NETWORKS to configuration.preferredNetworks.toAnalyticsValue(),
                FIELD_CARD_BRAND_ACCEPTANCE to configuration.cardBrandAcceptance.toAnalyticsValue(),
            )

            put(FIELD_CUSTOMER_SHEET_CONFIGURATION, configurationMap)
        }
    }

    class ScreenPresented(
        screen: CustomerSheetEventReporter.Screen,
    ) : CustomerSheetEvent() {
        override val additionalParams: Map<String, Any?> = mapOf()
        override val eventName: String = when (screen) {
            CustomerSheetEventReporter.Screen.AddPaymentMethod ->
                CS_ADD_PAYMENT_METHOD_SCREEN_PRESENTED
            CustomerSheetEventReporter.Screen.SelectPaymentMethod ->
                CS_SELECT_PAYMENT_METHOD_SCREEN_PRESENTED
            CustomerSheetEventReporter.Screen.EditPaymentMethod ->
                CS_SHOW_EDITABLE_PAYMENT_OPTION
        }
    }

    class ScreenHidden(
        screen: CustomerSheetEventReporter.Screen,
    ) : CustomerSheetEvent() {
        override val additionalParams: Map<String, Any?> = mapOf()
        override val eventName: String = when (screen) {
            CustomerSheetEventReporter.Screen.EditPaymentMethod ->
                CS_HIDE_EDITABLE_PAYMENT_OPTION
            else -> throw IllegalArgumentException(
                "${screen.name} has no supported event for hiding screen!"
            )
        }
    }

    class SelectPaymentMethod(
        code: String,
    ) : CustomerSheetEvent() {
        override val eventName: String = CS_PAYMENT_METHOD_SELECTED
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_SELECTED_LPM to code,
        )
    }

    class ConfirmPaymentMethodSucceeded(
        type: String,
        syncDefaultEnabled: Boolean?,
    ) : CustomerSheetEvent() {
        override val additionalParams: Map<String, Any?> = buildMap {
            put(FIELD_PAYMENT_METHOD_TYPE, type)
            syncDefaultEnabled?.let { put(FIELD_SYNC_DEFAULT_ENABLED, it) }
        }
        override val eventName: String = CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_SUCCEEDED
    }

    class ConfirmPaymentMethodFailed(
        type: String,
        syncDefaultEnabled: Boolean?,
    ) : CustomerSheetEvent() {
        override val additionalParams: Map<String, Any?> = buildMap {
            put(FIELD_PAYMENT_METHOD_TYPE, type)
            syncDefaultEnabled?.let { put(FIELD_SYNC_DEFAULT_ENABLED, it) }
        }
        override val eventName: String = CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_FAILED
    }

    class EditTapped : CustomerSheetEvent() {
        override val additionalParams: Map<String, Any?> = mapOf()
        override val eventName: String = CS_SELECT_PAYMENT_METHOD_EDIT_TAPPED
    }

    class EditCompleted : CustomerSheetEvent() {
        override val additionalParams: Map<String, Any?> = mapOf()
        override val eventName: String = CS_SELECT_PAYMENT_METHOD_DONE_TAPPED
    }

    class RemovePaymentMethodSucceeded : CustomerSheetEvent() {
        override val additionalParams: Map<String, Any?> = mapOf()
        override val eventName: String = CS_SELECT_PAYMENT_METHOD_REMOVE_PM_SUCCEEDED
    }

    class RemovePaymentMethodFailed : CustomerSheetEvent() {
        override val additionalParams: Map<String, Any?> = mapOf()
        override val eventName: String = CS_SELECT_PAYMENT_METHOD_REMOVE_PM_FAILED
    }

    class AttachPaymentMethodSucceeded(
        style: CustomerSheetEventReporter.AddPaymentMethodStyle
    ) : CustomerSheetEvent() {
        override val additionalParams: Map<String, Any?> = mapOf()
        override val eventName: String = when (style) {
            CustomerSheetEventReporter.AddPaymentMethodStyle.SetupIntent ->
                CS_ADD_PAYMENT_METHOD_VIA_SETUP_INTENT_SUCCEEDED
            CustomerSheetEventReporter.AddPaymentMethodStyle.CreateAttach ->
                CS_ADD_PAYMENT_METHOD_VIA_CREATE_ATTACH_SUCCEEDED
        }
    }

    class AttachPaymentMethodCanceled : CustomerSheetEvent() {
        override val additionalParams: Map<String, Any?> = mapOf()
        override val eventName: String = CS_ADD_PAYMENT_METHOD_VIA_SETUP_INTENT_CANCELED
    }

    class AttachPaymentMethodFailed(
        style: CustomerSheetEventReporter.AddPaymentMethodStyle
    ) : CustomerSheetEvent() {
        override val additionalParams: Map<String, Any?> = mapOf()
        override val eventName: String = when (style) {
            CustomerSheetEventReporter.AddPaymentMethodStyle.SetupIntent ->
                CS_ADD_PAYMENT_METHOD_VIA_SETUP_INTENT_FAILED
            CustomerSheetEventReporter.AddPaymentMethodStyle.CreateAttach ->
                CS_ADD_PAYMENT_METHOD_VIA_CREATE_ATTACH_FAILED
        }
    }

    class ShowPaymentOptionBrands(
        source: Source,
        selectedBrand: CardBrand
    ) : CustomerSheetEvent() {
        override val eventName: String = CS_SHOW_PAYMENT_OPTION_BRANDS

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
        selectedBrand: CardBrand?
    ) : CustomerSheetEvent() {
        override val eventName: String = CS_HIDE_PAYMENT_OPTION_BRANDS

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
    ) : CustomerSheetEvent() {
        override val eventName: String = CS_UPDATE_PAYMENT_METHOD

        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_SELECTED_CARD_BRAND to selectedBrand.code
        )
    }

    class UpdatePaymentOptionFailed(
        selectedBrand: CardBrand,
        error: Throwable,
    ) : CustomerSheetEvent() {
        override val eventName: String = CS_UPDATE_PAYMENT_METHOD_FAILED

        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_SELECTED_CARD_BRAND to selectedBrand.code,
            FIELD_ERROR_MESSAGE to error.message,
        )
    }

    class CardBrandDisallowed(
        cardBrand: CardBrand,
    ) : CustomerSheetEvent() {
        override val eventName: String = CS_DISALLOWED_CARD_BRAND

        override val additionalParams: Map<String, Any?> = mapOf(
            VALUE_CARD_BRAND to cardBrand.code
        )
    }

    class CardNumberCompleted : CustomerSheetEvent() {
        override val eventName: String = CS_CARD_NUMBER_COMPLETED

        override val additionalParams: Map<String, Any?> = mapOf()
    }

    internal companion object {
        const val CS_INIT_WITH_CUSTOMER_ADAPTER = "cs_init_with_customer_adapter"
        const val CS_INIT_WITH_CUSTOMER_SESSION = "cs_init_with_customer_session"

        const val CS_ADD_PAYMENT_METHOD_SCREEN_PRESENTED =
            "cs_add_payment_method_screen_presented"
        const val CS_SELECT_PAYMENT_METHOD_SCREEN_PRESENTED =
            "cs_select_payment_method_screen_presented"

        const val CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_SUCCEEDED =
            "cs_select_payment_method_screen_confirmed_savedpm_success"
        const val CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_FAILED =
            "cs_select_payment_method_screen_confirmed_savedpm_failure"

        const val CS_PAYMENT_METHOD_SELECTED =
            "cs_carousel_payment_method_selected"

        const val CS_CARD_NUMBER_COMPLETED =
            "cs_card_number_completed"

        const val CS_SELECT_PAYMENT_METHOD_EDIT_TAPPED =
            "cs_select_payment_method_screen_edit_tapped"
        const val CS_SELECT_PAYMENT_METHOD_DONE_TAPPED =
            "cs_select_payment_method_screen_done_tapped"

        const val CS_SELECT_PAYMENT_METHOD_REMOVE_PM_SUCCEEDED =
            "cs_select_payment_method_screen_removepm_success"
        const val CS_SELECT_PAYMENT_METHOD_REMOVE_PM_FAILED =
            "cs_select_payment_method_screen_removepm_failure"

        const val CS_ADD_PAYMENT_METHOD_VIA_SETUP_INTENT_SUCCEEDED =
            "cs_add_payment_method_via_setup_intent_success"
        const val CS_ADD_PAYMENT_METHOD_VIA_SETUP_INTENT_CANCELED =
            "cs_add_payment_method_via_setupintent_canceled"
        const val CS_ADD_PAYMENT_METHOD_VIA_SETUP_INTENT_FAILED =
            "cs_add_payment_method_via_setup_intent_failure"

        const val CS_ADD_PAYMENT_METHOD_VIA_CREATE_ATTACH_SUCCEEDED =
            "cs_add_payment_method_via_createAttach_success"
        const val CS_ADD_PAYMENT_METHOD_VIA_CREATE_ATTACH_FAILED =
            "cs_add_payment_method_via_createAttach_failure"

        const val CS_SHOW_EDITABLE_PAYMENT_OPTION = "cs_open_edit_screen"
        const val CS_HIDE_EDITABLE_PAYMENT_OPTION = "cs_cancel_edit_screen"

        const val CS_SHOW_PAYMENT_OPTION_BRANDS = "cs_open_cbc_dropdown"
        const val CS_HIDE_PAYMENT_OPTION_BRANDS = "cs_close_cbc_dropdown"

        const val CS_UPDATE_PAYMENT_METHOD = "cs_update_card"
        const val CS_UPDATE_PAYMENT_METHOD_FAILED = "cs_update_card_failed"
        const val CS_DISALLOWED_CARD_BRAND = "cs_disallowed_card_brand"

        const val FIELD_GOOGLE_PAY_ENABLED = "google_pay_enabled"
        const val FIELD_BILLING = "default_billing_details"
        const val FIELD_PREFERRED_NETWORKS = "preferred_networks"
        const val FIELD_CUSTOMER_SHEET_CONFIGURATION = "cs_config"
        const val FIELD_APPEARANCE = "appearance"
        const val FIELD_PAYMENT_METHOD_ORDER = "payment_method_order"
        const val FIELD_BILLING_DETAILS_COLLECTION_CONFIGURATION = "billing_details_collection_configuration"
        const val FIELD_ALLOWS_REMOVAL_OF_LAST_SAVED_PAYMENT_METHOD = "allows_removal_of_last_saved_payment_method"
        const val FIELD_CBC_EVENT_SOURCE = "cbc_event_source"
        const val FIELD_SELECTED_CARD_BRAND = "selected_card_brand"
        const val FIELD_ERROR_MESSAGE = "error_message"
        const val FIELD_PAYMENT_METHOD_TYPE = "payment_method_type"
        const val FIELD_SYNC_DEFAULT_ENABLED = "sync_default_enabled"
        const val FIELD_SELECTED_LPM = "selected_lpm"
        const val FIELD_CARD_BRAND_ACCEPTANCE = "card_brand_acceptance"
        const val FIELD_CUSTOMER_ACCESS_PROVIDER = "customer_access_provider"

        const val VALUE_EDIT_CBC_EVENT_SOURCE = "edit"
        const val VALUE_ADD_CBC_EVENT_SOURCE = "add"
        const val VALUE_CARD_BRAND = "brand"
    }
}
