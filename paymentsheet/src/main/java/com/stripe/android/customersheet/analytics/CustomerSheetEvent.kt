package com.stripe.android.customersheet.analytics

import com.stripe.android.core.networking.AnalyticsEvent

internal sealed class CustomerSheetEvent : AnalyticsEvent {

    abstract val additionalParams: Map<String, Any?>

    class ScreenPresented(
        screen: CustomerSheetEventReporter.Screen,
    ) : CustomerSheetEvent() {
        override val additionalParams: Map<String, Any?> = mapOf()
        override val eventName: String = when (screen) {
            CustomerSheetEventReporter.Screen.AddPaymentMethod ->
                CS_ADD_PAYMENT_METHOD_SCREEN_PRESENTED
            CustomerSheetEventReporter.Screen.SelectPaymentMethod ->
                CS_SELECT_PAYMENT_METHOD_SCREEN_PRESENTED
        }
    }

    class ConfirmPaymentMethodSucceeded(
        type: String,
    ) : CustomerSheetEvent() {
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_PAYMENT_METHOD_TYPE to type
        )
        override val eventName: String = CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_SUCCEEDED
    }

    class ConfirmPaymentMethodFailed(
        type: String,
    ) : CustomerSheetEvent() {
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_PAYMENT_METHOD_TYPE to type
        )
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

    internal companion object {
        const val CS_ADD_PAYMENT_METHOD_SCREEN_PRESENTED =
            "cs_add_payment_method_screen_presented"
        const val CS_SELECT_PAYMENT_METHOD_SCREEN_PRESENTED =
            "cs_select_payment_method_screen_presented"

        const val CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_SUCCEEDED =
            "cs_select_payment_method_screen_confirmed_savedpm_success"
        const val CS_SELECT_PAYMENT_METHOD_CONFIRMED_SAVED_PM_FAILED =
            "cs_select_payment_method_screen_confirmed_savedpm_failure"

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

        const val FIELD_PAYMENT_METHOD_TYPE = "payment_method_type"
    }
}
