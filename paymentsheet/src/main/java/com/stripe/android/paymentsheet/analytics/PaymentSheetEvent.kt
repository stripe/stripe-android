package com.stripe.android.paymentsheet.analytics

import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.utils.mapOfDurationInSeconds
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkMode
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.isLink
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormViewModel.AnalyticsEvent.Finished
import com.stripe.android.paymentsheet.state.asPaymentSheetLoadingException
import com.stripe.android.paymentsheet.utils.getSetAsDefaultPaymentMethodFromPaymentSelection
import com.stripe.android.utils.filterNotNullValues
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal sealed class PaymentSheetEvent : AnalyticsEvent {

    open val params: Map<String, Any?> = emptyMap()

    class LoadStarted(initializedViaCompose: Boolean) : PaymentSheetEvent() {
        override val eventName: String = "mc_load_started"
        override val params: Map<String, Any?> = mapOf("compose" to initializedViaCompose)
    }

    class LoadSucceeded(
        paymentSelection: PaymentSelection?,
        orderedLpms: List<String>,
        duration: Duration?,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_load_succeeded"
        override val params: Map<String, Any?> = buildMap {
            put(FIELD_DURATION, duration?.asSeconds)
            put(FIELD_SELECTED_LPM, paymentSelection.defaultAnalyticsValue)
            put(FIELD_ORDERED_LPMS, orderedLpms.joinToString(","))
        }

        private val PaymentSelection?.defaultAnalyticsValue: String
            get() = when (this) {
                is PaymentSelection.GooglePay -> "google_pay"
                is PaymentSelection.Link -> "link"
                is PaymentSelection.Saved -> paymentMethod.type?.code ?: "saved"
                else -> "none"
            }
    }

    class LoadFailed(
        duration: Duration?,
        error: Throwable,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_load_failed"
        override val params: Map<String, Any?> = mapOf(
            FIELD_DURATION to duration?.asSeconds,
            FIELD_ERROR_MESSAGE to error.asPaymentSheetLoadingException.type,
        ).plus(ErrorReporter.getAdditionalParamsFromError(error))
    }

    class ElementsSessionLoadFailed(
        error: Throwable,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_elements_session_load_failed"
        override val params: Map<String, Any?> = mapOf(
            FIELD_ERROR_MESSAGE to error.asPaymentSheetLoadingException.type,
        ).plus(ErrorReporter.getAdditionalParamsFromError(error))
    }

    class Init(
        private val mode: EventReporter.Mode,
    ) : PaymentSheetEvent() {

        override val eventName: String
            get() = formatEventName(mode, "init")
    }

    class Dismiss : PaymentSheetEvent() {
        override val eventName: String = "mc_dismiss"
    }

    class ShowNewPaymentOptions(
        mode: EventReporter.Mode,
    ) : PaymentSheetEvent() {
        override val eventName: String = formatEventName(mode, "sheet_newpm_show")
    }

    class ShowExistingPaymentOptions(
        mode: EventReporter.Mode,
    ) : PaymentSheetEvent() {
        override val eventName: String = formatEventName(mode, "sheet_savedpm_show")
    }

    class ShowManagePaymentMethods(
        mode: EventReporter.Mode,
    ) : PaymentSheetEvent() {
        override val eventName: String = formatEventName(mode, "manage_savedpm_show")
    }

    class SelectPaymentMethod(
        code: String,
        linkContext: String?,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_carousel_payment_method_tapped"
        override val params: Map<String, Any?> = mapOf(
            FIELD_SELECTED_LPM to code,
            FIELD_LINK_CONTEXT to linkContext,
        )
    }

    class RemovePaymentOption(
        mode: EventReporter.Mode,
        code: String,
    ) : PaymentSheetEvent() {
        override val eventName: String =
            formatEventName(mode, "paymentoption_removed")
        override val params: Map<String, Any?> = mapOf(
            FIELD_SELECTED_LPM to code,
        )
    }

    class SelectPaymentOption(
        mode: EventReporter.Mode,
        paymentSelection: PaymentSelection?,
    ) : PaymentSheetEvent() {
        override val eventName: String =
            formatEventName(mode, "paymentoption_${analyticsValue(paymentSelection)}_select")
    }

    class ShowPaymentOptionForm(
        code: String,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_form_shown"
        override val params: Map<String, Any?> = mapOf(
            FIELD_SELECTED_LPM to code
        )
    }

    class PaymentOptionFormInteraction(
        code: String,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_form_interacted"
        override val params: Map<String, Any?> = mapOf(
            FIELD_SELECTED_LPM to code
        )
    }

    class PaymentMethodFormCompleted(
        code: String,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_form_completed"
        override val params: Map<String, Any?> = mapOf(
            FIELD_SELECTED_LPM to code
        )
    }

    class CardNumberCompleted : PaymentSheetEvent() {
        override val eventName: String = "mc_card_number_completed"
    }

    class CardBrandDisallowed(
        cardBrand: CardBrand,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_disallowed_card_brand"

        override val params: Map<String, Any?> = mapOf(
            VALUE_CARD_BRAND to cardBrand.code
        )
    }

    class PressConfirmButton(
        duration: Duration?,
        selectedLpm: String?,
        linkContext: String?,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_confirm_button_tapped"
        override val params: Map<String, Any?> = mapOf(
            FIELD_DURATION to duration?.asSeconds,
            FIELD_SELECTED_LPM to selectedLpm,
            FIELD_LINK_CONTEXT to linkContext,
        ).filterNotNullValues()
    }

    class Payment(
        mode: EventReporter.Mode,
        private val result: Result,
        duration: Duration?,
        paymentSelection: PaymentSelection,
        private val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        private val intentId: String?,
    ) : PaymentSheetEvent() {

        override val eventName: String =
            if (mode == EventReporter.Mode.Embedded) {
                formatEventName(mode, "payment_${result.analyticsValue}")
            } else {
                formatEventName(mode, "payment_${analyticsValue(paymentSelection)}_${result.analyticsValue}")
            }

        override val params: Map<String, Any?> = buildMap {
            put(FIELD_DURATION, duration?.asSeconds)
            deferredIntentConfirmationType?.let { type ->
                put(FIELD_DEFERRED_INTENT_CONFIRMATION_TYPE, type.value)
            }
            intentId?.let { id ->
                put(INTENT_ID, id)
            }
            if (result is Result.Failure) {
                put(FIELD_ERROR_MESSAGE, result.error.analyticsValue)
                result.error.errorCode?.let { errorCode ->
                    put(FIELD_ERROR_CODE, errorCode)
                }
            }
            put(FIELD_SELECTED_LPM, paymentSelection.code())
            paymentSelection.linkContext()?.let { linkContext ->
                put(FIELD_LINK_CONTEXT, linkContext)
            }
            paymentSelection.getSetAsDefaultPaymentMethodFromPaymentSelection()?.let { setAsDefault ->
                put(FIELD_SET_AS_DEFAULT, setAsDefault)
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
        val errorMessage: String?
    ) : PaymentSheetEvent() {
        override val eventName: String = "luxe_serialize_failure"
        override val params: Map<String, Any?> = mapOf(FIELD_ERROR_MESSAGE to errorMessage)
    }

    class AutofillEvent(
        type: String,
    ) : PaymentSheetEvent() {
        private fun String.toSnakeCase() = replace(
            "(?<=.)(?=\\p{Upper})".toRegex(),
            "_"
        ).lowercase()

        override val eventName: String = "autofill_${type.toSnakeCase()}"
    }

    class ShowEditablePaymentOption : PaymentSheetEvent() {
        override val eventName: String = "mc_open_edit_screen"
    }

    class HideEditablePaymentOption : PaymentSheetEvent() {
        override val eventName: String = "mc_cancel_edit_screen"
    }

    class CardBrandSelected(
        source: Source,
        selectedBrand: CardBrand,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_cbc_selected"

        override val params: Map<String, Any?> = mapOf(
            FIELD_CBC_EVENT_SOURCE to source.value,
            FIELD_SELECTED_CARD_BRAND to selectedBrand.code
        )

        enum class Source(val value: String) {
            Edit(VALUE_EDIT_CBC_EVENT_SOURCE), Add(VALUE_ADD_CBC_EVENT_SOURCE)
        }
    }

    class SetAsDefaultPaymentMethodSucceeded(
        val paymentMethodType: String?,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_set_default_payment_method"

        override val params: Map<String, Any?> = mapOf(
            FIELD_PAYMENT_METHOD_TYPE to paymentMethodType,
        )
    }

    class SetAsDefaultPaymentMethodFailed(
        error: Throwable,
        paymentMethodType: String?,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_set_default_payment_method_failed"

        override val params: Map<String, Any?> = mapOf(
            FIELD_ERROR_MESSAGE to error.message,
            FIELD_PAYMENT_METHOD_TYPE to paymentMethodType,
        ).plus(ErrorReporter.getAdditionalParamsFromError(error))
    }

    class UpdatePaymentOptionSucceeded(
        selectedBrand: CardBrand?,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_update_card"

        override val params: Map<String, Any?> = buildMap {
            if (selectedBrand != null) {
                put(FIELD_SELECTED_CARD_BRAND, selectedBrand.code)
            }
        }
    }

    class UpdatePaymentOptionFailed(
        selectedBrand: CardBrand?,
        error: Throwable,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_update_card_failed"

        override val params: Map<String, Any?> = buildMap {
            if (selectedBrand != null) {
                put(FIELD_SELECTED_CARD_BRAND, selectedBrand.code)
            }
            put(FIELD_ERROR_MESSAGE, error.message)
        }.plus(ErrorReporter.getAdditionalParamsFromError(error))
    }

    class CannotProperlyReturnFromLinkAndLPMs(
        mode: EventReporter.Mode,
    ) : PaymentSheetEvent() {
        override val eventName: String = formatEventName(mode, "cannot_return_from_link_and_lpms")
    }

    class BankAccountCollectorStarted : PaymentSheetEvent() {
        override val eventName: String = "stripe_android.bankaccountcollector.started"
    }

    class BankAccountCollectorFinished(
        event: Finished,
    ) :
        PaymentSheetEvent() {
        override val eventName: String = "stripe_android.bankaccountcollector.finished"

        override val params: Map<String, Any?> = mapOf(
            INTENT_ID to event.intent?.id,
            LINK_ACCOUNT_SESSION_ID to event.linkAccountSessionId,
            FC_SDK_RESULT to event.result
        )
    }

    class ExperimentExposure(
        experiment: LoggableExperiment,
        mode: EventReporter.Mode
    ) : PaymentSheetEvent() {
        override val eventName: String = "elements.experiment_exposure"
        override val params: Map<String, Any?> = mapOf(
            "experiment_retrieved" to experiment.experiment.experimentValue,
            "arb_id" to experiment.arbId,
            "assignment_group" to experiment.group
        ) + experiment.dimensions.mapKeys { "dimensions-${it.key}" } + ("dimensions-integration_shape" to mode.code)
    }

    class ShopPayWebviewLoadAttempt : PaymentSheetEvent() {
        override val eventName: String = "mc_shoppay_webview_load_attempt"
    }

    class ShopPayWebviewConfirmSuccess : PaymentSheetEvent() {
        override val eventName: String = "mc_shoppay_webview_confirm_success"
    }

    class ShopPayWebviewCancelled(
        didReceiveECEClick: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_shoppay_webview_cancelled"
        override val params: Map<String, Any?> = mapOf(
            "did_receive_ece_click" to didReceiveECEClick
        )
    }

    class CardScanStarted(
        implementation: String,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_cardscan_scan_started"
        override val params: Map<String, Any?> = mapOf(
            "implementation" to implementation
        )
    }

    class CardScanSucceeded(
        implementation: String,
        duration: Duration?,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_cardscan_success"
        override val params: Map<String, Any?> =
            duration.mapOfDurationInSeconds() +
                mapOf(
                    "implementation" to implementation
                )
    }

    class CardScanFailed(
        implementation: String,
        duration: Duration?,
        error: Throwable?,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_cardscan_failed"
        override val params: Map<String, Any?> =
            duration.mapOfDurationInSeconds() +
                mapOf(
                    "implementation" to implementation,
                    FIELD_ERROR_MESSAGE to error?.javaClass?.simpleName
                )
    }

    class CardScanCancelled(
        implementation: String,
        duration: Duration?,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_cardscan_cancel"
        override val params: Map<String, Any?> =
            duration.mapOfDurationInSeconds() +
                mapOf(
                    "implementation" to implementation
                )
    }

    class CardScanApiCheckSucceeded(
        implementation: String,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_cardscan_api_check_succeeded"
        override val params: Map<String, Any?> = mapOf(
            "implementation" to implementation
        )
    }

    class CardScanApiCheckFailed(
        implementation: String,
        error: Throwable?,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_cardscan_api_check_failed"
        override val params: Map<String, Any?> = mapOf(
            "implementation" to implementation,
            FIELD_ERROR_MESSAGE to error?.javaClass?.simpleName
        )
    }

    class InitialDisplayedPaymentMethods(
        visiblePaymentMethods: List<String>,
        hiddenPaymentMethods: List<String>,
        isVerticalLayout: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_initial_displayed_payment_methods"
        override val params: Map<String, Any?> = buildMap {
            put(FIELD_VISIBLE_PAYMENT_METHODS, visiblePaymentMethods.joinToString(","))
            put(FIELD_HIDDEN_PAYMENT_METHODS, hiddenPaymentMethods.joinToString(","))
            put(FIELD_PAYMENT_METHOD_LAYOUT, if (isVerticalLayout) "vertical" else "horizontal")
        }
    }

    internal companion object {
        private fun analyticsValue(
            paymentSelection: PaymentSelection?
        ) = when (paymentSelection) {
            is PaymentSelection.GooglePay -> "googlepay"
            is PaymentSelection.Saved -> "savedpm"
            is PaymentSelection.Link,
            is PaymentSelection.ExternalPaymentMethod,
            is PaymentSelection.CustomPaymentMethod,
            is PaymentSelection.New -> {
                if (paymentSelection.isLink) {
                    "link"
                } else {
                    "newpm"
                }
            }
            null -> "unknown"
            is PaymentSelection.ShopPay -> "shop_pay"
        }

        private fun formatEventName(mode: EventReporter.Mode, eventName: String): String {
            return "mc_${mode}_$eventName"
        }

        const val FIELD_DEFERRED_INTENT_CONFIRMATION_TYPE = "deferred_intent_confirmation_type"
        const val FIELD_DURATION = "duration"
        const val FIELD_SELECTED_LPM = "selected_lpm"
        const val FIELD_ERROR_MESSAGE = "error_message"
        const val FIELD_ERROR_CODE = "error_code"
        const val FIELD_CBC_EVENT_SOURCE = "cbc_event_source"
        const val FIELD_PAYMENT_METHOD_TYPE = "payment_method_type"
        const val FIELD_SELECTED_CARD_BRAND = "selected_card_brand"
        const val FIELD_SET_AS_DEFAULT = "set_as_default"
        const val FIELD_LINK_CONTEXT = "link_context"
        const val FIELD_PAYMENT_METHOD_LAYOUT = "payment_method_layout"
        const val FIELD_ORDERED_LPMS = "ordered_lpms"
        const val INTENT_ID = "intent_id"
        const val LINK_ACCOUNT_SESSION_ID = "link_account_session_id"
        const val FC_SDK_RESULT = "fc_sdk_result"
        const val FIELD_VISIBLE_PAYMENT_METHODS = "visible_payment_methods"
        const val FIELD_HIDDEN_PAYMENT_METHODS = "hidden_payment_methods"

        const val VALUE_EDIT_CBC_EVENT_SOURCE = "edit"
        const val VALUE_ADD_CBC_EVENT_SOURCE = "add"
        const val VALUE_CARD_BRAND = "brand"

        const val MAX_EXTERNAL_PAYMENT_METHODS = 10
    }
}

private val Duration.asSeconds: Float
    get() = toDouble(DurationUnit.SECONDS).toFloat()

internal fun PaymentSelection.code(): String {
    return when (this) {
        is PaymentSelection.GooglePay -> "google_pay"
        is PaymentSelection.Link -> "link"
        is PaymentSelection.ShopPay -> "shop_pay"
        is PaymentSelection.New -> paymentMethodCreateParams.typeCode
        is PaymentSelection.Saved -> paymentMethod.type?.code ?: "saved"
        is PaymentSelection.ExternalPaymentMethod -> type
        is PaymentSelection.CustomPaymentMethod -> id
    }
}

internal fun PaymentSelection.linkContext(): String? {
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
        is PaymentSelection.CustomPaymentMethod,
        is PaymentSelection.ExternalPaymentMethod,
        is PaymentSelection.ShopPay -> null
    }
}
