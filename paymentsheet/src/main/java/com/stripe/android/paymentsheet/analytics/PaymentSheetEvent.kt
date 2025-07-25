package com.stripe.android.paymentsheet.analytics

import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.common.analytics.getCustomPaymentMethodsAnalyticsValue
import com.stripe.android.common.analytics.getExternalPaymentMethodsAnalyticsValue
import com.stripe.android.common.analytics.toAnalyticsMap
import com.stripe.android.common.analytics.toAnalyticsValue
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.analyticsValue
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormViewModel.AnalyticsEvent.Finished
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.asPaymentSheetLoadingException
import com.stripe.android.paymentsheet.utils.getSetAsDefaultPaymentMethodFromPaymentSelection
import com.stripe.android.utils.filterNotNullValues
import kotlin.time.Duration
import kotlin.time.DurationUnit

internal sealed class PaymentSheetEvent : AnalyticsEvent {

    sealed interface ConfigurationSpecificPayload {
        val payload: Map<String, Any?>

        data class PaymentSheet(
            private val configuration: PaymentSheet.Configuration,
        ) : ConfigurationSpecificPayload {
            override val payload: Map<String, Any?>
                get() = buildMap {
                    put(FIELD_PAYMENT_METHOD_LAYOUT, configuration.paymentMethodLayout.toAnalyticsValue())
                }
        }

        data class Embedded(
            private val isRowSelectionImmediateAction: Boolean,
            private val configuration: EmbeddedPaymentElement.Configuration,
        ) : ConfigurationSpecificPayload {
            override val payload: Map<String, Any?>
                get() = buildMap {
                    put(
                        "form_sheet_action",
                        when (configuration.formSheetAction) {
                            EmbeddedPaymentElement.FormSheetAction.Continue -> "continue"
                            EmbeddedPaymentElement.FormSheetAction.Confirm -> "confirm"
                        }
                    )
                    put(
                        FIELD_ROW_SELECTION_BEHAVIOR,
                        if (isRowSelectionImmediateAction) "immediate_action" else "default"
                    )
                    put("embedded_view_displays_mandate_text", configuration.embeddedViewDisplaysMandateText)
                }
        }
    }

    val params: Map<String, Any?>
        get() = standardParams(isDeferred, isSpt, linkEnabled, googlePaySupported) + additionalParams

    protected abstract val isDeferred: Boolean
    protected abstract val isSpt: Boolean
    protected abstract val linkEnabled: Boolean
    protected abstract val googlePaySupported: Boolean
    protected abstract val additionalParams: Map<String, Any?>

    class LoadStarted(
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
        initializedViaCompose: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_load_started"
        override val additionalParams: Map<String, Any?> = mapOf(FIELD_COMPOSE to initializedViaCompose)
    }

    class LoadSucceeded(
        paymentSelection: PaymentSelection?,
        initializationMode: PaymentElementLoader.InitializationMode,
        orderedLpms: List<String>,
        duration: Duration?,
        linkMode: LinkMode?,
        override val linkEnabled: Boolean,
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val googlePaySupported: Boolean,
        linkDisplay: PaymentSheet.LinkConfiguration.Display,
        financialConnectionsAvailability: FinancialConnectionsAvailability?,
        requireCvcRecollection: Boolean = false,
        hasDefaultPaymentMethod: Boolean? = null,
        setAsDefaultEnabled: Boolean? = null,
        setupFutureUsage: StripeIntent.Usage? = null,
        paymentMethodOptionsSetupFutureUsage: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_load_succeeded"
        override val additionalParams: Map<String, Any?> = buildMap {
            put(FIELD_DURATION, duration?.asSeconds)
            put(FIELD_SELECTED_LPM, paymentSelection.defaultAnalyticsValue)
            put(FIELD_INTENT_TYPE, initializationMode.defaultAnalyticsValue)
            put(FIELD_ORDERED_LPMS, orderedLpms.joinToString(","))
            put(FIELD_REQUIRE_CVC_RECOLLECTION, requireCvcRecollection)
            put(FC_SDK_AVAILABILITY, financialConnectionsAvailability.toAnalyticsParam())
            put(FIELD_PAYMENT_METHOD_OPTIONS_SETUP_FUTURE_USAGE, paymentMethodOptionsSetupFutureUsage)
            put(FIELD_SETUP_FUTURE_USAGE, setupFutureUsage?.code)
            linkMode?.let { mode ->
                put(FIELD_LINK_MODE, mode.analyticsValue)
            }
            setAsDefaultEnabled?.let {
                put(FIELD_SET_AS_DEFAULT_ENABLED, it)
            }
            put(FIELD_LINK_DISPLAY, linkDisplay.analyticsValue)
            if (setAsDefaultEnabled == true && hasDefaultPaymentMethod != null) {
                put(FIELD_HAS_DEFAULT_PAYMENT_METHOD, hasDefaultPaymentMethod)
            }
        }

        private val PaymentSelection?.defaultAnalyticsValue: String
            get() = when (this) {
                is PaymentSelection.GooglePay -> "google_pay"
                is PaymentSelection.Link -> "link"
                is PaymentSelection.Saved -> paymentMethod.type?.code ?: "saved"
                else -> "none"
            }

        private val PaymentElementLoader.InitializationMode.defaultAnalyticsValue: String
            get() = when (this) {
                is PaymentElementLoader.InitializationMode.DeferredIntent -> {
                    when (this.intentConfiguration.mode) {
                        is PaymentSheet.IntentConfiguration.Mode.Payment -> "deferred_payment_intent"
                        is PaymentSheet.IntentConfiguration.Mode.Setup -> "deferred_setup_intent"
                    }
                }
                is PaymentElementLoader.InitializationMode.PaymentIntent -> "payment_intent"
                is PaymentElementLoader.InitializationMode.SetupIntent -> "setup_intent"
            }
    }

    class LoadFailed(
        duration: Duration?,
        error: Throwable,
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val isSpt: Boolean,
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
        override val isSpt: Boolean,
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
        private val configuration: CommonConfiguration,
        private val appearance: PaymentSheet.Appearance,
        private val primaryButtonColor: Boolean?,
        private val configurationSpecificPayload: ConfigurationSpecificPayload,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        private val isStripeCardScanAvailable: Boolean,
        private val isAnalyticEventCallbackSet: Boolean,
    ) : PaymentSheetEvent() {

        override val eventName: String
            get() {
                return if (mode == EventReporter.Mode.Embedded) {
                    formatEventName(mode, "init")
                } else {
                    val configValue = listOfNotNull(
                        FIELD_CUSTOMER.takeIf { configuration.customer != null },
                        FIELD_GOOGLE_PAY.takeIf { configuration.googlePay != null }
                    ).takeUnless { it.isEmpty() }?.joinToString(separator = "_") ?: "default"
                    formatEventName(mode, "init_$configValue")
                }
            }

        override val additionalParams: Map<String, Any?>
            get() {

                val configurationMap = mapOf(
                    FIELD_CUSTOMER to (configuration.customer != null),
                    FIELD_CUSTOMER_ACCESS_PROVIDER to (configuration.customer?.accessType?.analyticsValue),
                    FIELD_GOOGLE_PAY to (configuration.googlePay != null),
                    FIELD_PRIMARY_BUTTON_COLOR to primaryButtonColor,
                    FIELD_BILLING to (configuration.defaultBillingDetails?.isFilledOut() == true),
                    FIELD_DELAYED_PMS to configuration.allowsDelayedPaymentMethods,
                    FIELD_APPEARANCE to appearance.toAnalyticsMap(mode == EventReporter.Mode.Embedded),
                    FIELD_PAYMENT_METHOD_ORDER to configuration.paymentMethodOrder,
                    FIELD_ALLOWS_PAYMENT_METHODS_REQUIRING_SHIPPING_ADDRESS to
                        configuration.allowsPaymentMethodsRequiringShippingAddress,
                    FIELD_ALLOWS_REMOVAL_OF_LAST_SAVED_PAYMENT_METHOD to
                        configuration.allowsRemovalOfLastSavedPaymentMethod,
                    FIELD_BILLING_DETAILS_COLLECTION_CONFIGURATION to
                        configuration.billingDetailsCollectionConfiguration.toAnalyticsMap(),
                    FIELD_PREFERRED_NETWORKS to configuration.preferredNetworks.toAnalyticsValue(),
                    FIELD_CUSTOM_PAYMENT_METHODS to configuration.getCustomPaymentMethodsAnalyticsValue(),
                    FIELD_EXTERNAL_PAYMENT_METHODS to configuration.getExternalPaymentMethodsAnalyticsValue(),
                    FIELD_CARD_BRAND_ACCEPTANCE to configuration.cardBrandAcceptance.toAnalyticsValue(),
                    FIELD_CARD_SCAN_AVAILABLE to isStripeCardScanAvailable,
                    FIELD_ANALYTIC_CALLBACK_SET to isAnalyticEventCallbackSet,
                ).plus(configurationSpecificPayload.payload)
                return mapOf(
                    FIELD_MOBILE_PAYMENT_ELEMENT_CONFIGURATION to configurationMap,
                )
            }
    }

    class Dismiss(
        override val isDeferred: Boolean,
        override val linkEnabled: Boolean,
        override val isSpt: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_dismiss"
        override val additionalParams: Map<String, Any> = emptyMap()
    }

    class ShowNewPaymentOptions(
        mode: EventReporter.Mode,
        currency: String?,
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
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
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = formatEventName(mode, "sheet_savedpm_show")
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_CURRENCY to currency,
        )
    }

    class ShowManagePaymentMethods(
        mode: EventReporter.Mode,
        currency: String?,
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = formatEventName(mode, "manage_savedpm_show")
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_CURRENCY to currency,
        )
    }

    class SelectPaymentMethod(
        code: String,
        currency: String?,
        linkContext: String?,
        financialConnectionsAvailability: FinancialConnectionsAvailability?,
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_carousel_payment_method_tapped"
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_CURRENCY to currency,
            FIELD_SELECTED_LPM to code,
            FIELD_LINK_CONTEXT to linkContext,
            FC_SDK_AVAILABILITY to financialConnectionsAvailability.toAnalyticsParam()
        )
    }

    class RemovePaymentOption(
        mode: EventReporter.Mode,
        code: String,
        currency: String?,
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String =
            formatEventName(mode, "paymentoption_removed")
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
        override val isSpt: Boolean,
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
        override val isSpt: Boolean,
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
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_form_interacted"
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_SELECTED_LPM to code
        )
    }

    class PaymentMethodFormCompleted(
        code: String,
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_form_completed"
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_SELECTED_LPM to code
        )
    }

    class CardNumberCompleted(
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_card_number_completed"
        override val additionalParams: Map<String, Any?> = mapOf()
    }

    class CardBrandDisallowed(
        cardBrand: CardBrand,
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_disallowed_card_brand"

        override val additionalParams: Map<String, Any?> = mapOf(
            VALUE_CARD_BRAND to cardBrand.code
        )
    }

    class PressConfirmButton(
        currency: String?,
        duration: Duration?,
        selectedLpm: String?,
        linkContext: String?,
        financialConnectionsAvailability: FinancialConnectionsAvailability?,
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_confirm_button_tapped"
        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_DURATION to duration?.asSeconds,
            FIELD_CURRENCY to currency,
            FIELD_SELECTED_LPM to selectedLpm,
            FIELD_LINK_CONTEXT to linkContext,
            FC_SDK_AVAILABILITY to financialConnectionsAvailability.toAnalyticsParam()
        ).filterNotNullValues()
    }

    class Payment(
        mode: EventReporter.Mode,
        private val result: Result,
        duration: Duration?,
        paymentSelection: PaymentSelection,
        currency: String?,
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
        private val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
    ) : PaymentSheetEvent() {

        override val eventName: String =
            if (mode == EventReporter.Mode.Embedded) {
                formatEventName(mode, "payment_${result.analyticsValue}")
            } else {
                formatEventName(mode, "payment_${analyticsValue(paymentSelection)}_${result.analyticsValue}")
            }

        override val additionalParams: Map<String, Any?> = buildMap {
            put(FIELD_DURATION, duration?.asSeconds)
            put(FIELD_CURRENCY, currency)
            deferredIntentConfirmationType?.let { type ->
                put(FIELD_DEFERRED_INTENT_CONFIRMATION_TYPE, type.value)
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
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
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
        override val isSpt: Boolean,
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
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_open_edit_screen"

        override val additionalParams: Map<String, Any?> = emptyMap()
    }

    class HideEditablePaymentOption(
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_cancel_edit_screen"

        override val additionalParams: Map<String, Any?> = emptyMap()
    }

    class CardBrandSelected(
        source: Source,
        selectedBrand: CardBrand,
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_cbc_selected"

        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_CBC_EVENT_SOURCE to source.value,
            FIELD_SELECTED_CARD_BRAND to selectedBrand.code
        )

        enum class Source(val value: String) {
            Edit(VALUE_EDIT_CBC_EVENT_SOURCE), Add(VALUE_ADD_CBC_EVENT_SOURCE)
        }
    }

    class SetAsDefaultPaymentMethodSucceeded(
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
        val paymentMethodType: String?,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_set_default_payment_method"

        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_PAYMENT_METHOD_TYPE to paymentMethodType,
        )
    }

    class SetAsDefaultPaymentMethodFailed(
        error: Throwable,
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
        paymentMethodType: String?,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_set_default_payment_method_failed"

        override val additionalParams: Map<String, Any?> = mapOf(
            FIELD_ERROR_MESSAGE to error.message,
            FIELD_PAYMENT_METHOD_TYPE to paymentMethodType,
        ).plus(ErrorReporter.getAdditionalParamsFromError(error))
    }

    class UpdatePaymentOptionSucceeded(
        selectedBrand: CardBrand?,
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_update_card"

        override val additionalParams: Map<String, Any?> = buildMap {
            if (selectedBrand != null) {
                put(FIELD_SELECTED_CARD_BRAND, selectedBrand.code)
            }
        }
    }

    class UpdatePaymentOptionFailed(
        selectedBrand: CardBrand?,
        error: Throwable,
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_update_card_failed"

        override val additionalParams: Map<String, Any?> = buildMap {
            if (selectedBrand != null) {
                put(FIELD_SELECTED_CARD_BRAND, selectedBrand.code)
            }
            put(FIELD_ERROR_MESSAGE, error.message)
        }.plus(ErrorReporter.getAdditionalParamsFromError(error))
    }

    class CannotProperlyReturnFromLinkAndLPMs(
        mode: EventReporter.Mode,
    ) : PaymentSheetEvent() {
        override val linkEnabled: Boolean = false
        override val isDeferred: Boolean = false
        override val googlePaySupported: Boolean = false
        override val isSpt: Boolean = false

        override val eventName: String = formatEventName(mode, "cannot_return_from_link_and_lpms")

        override val additionalParams: Map<String, Any?> = mapOf()
    }

    class BankAccountCollectorStarted(
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
        financialConnectionsAvailability: FinancialConnectionsAvailability?
    ) :
        PaymentSheetEvent() {
        override val eventName: String = "stripe_android.bankaccountcollector.started"
        override val additionalParams: Map<String, Any?> = mapOf(
            FC_SDK_AVAILABILITY to financialConnectionsAvailability.toAnalyticsParam()
        )
    }

    class BankAccountCollectorFinished(
        event: Finished,
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
        financialConnectionsAvailability: FinancialConnectionsAvailability?
    ) :
        PaymentSheetEvent() {
        override val eventName: String = "stripe_android.bankaccountcollector.finished"

        override val additionalParams: Map<String, Any?> = mapOf(
            FC_SDK_AVAILABILITY to financialConnectionsAvailability.toAnalyticsParam(),
            INTENT_ID to event.intent?.id,
            INTENT_TYPE to event.paymentIntentType(),
            LINK_ACCOUNT_SESSION_ID to event.linkAccountSessionId,
            FC_SDK_RESULT to event.result
        )

        private fun Finished.paymentIntentType(): String? = when {
            isDeferred -> "deferred"
            intent is PaymentIntent -> "payment"
            intent is SetupIntent -> "setup"
            else -> null
        }
    }

    class ExperimentExposure(
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
        experiment: LoggableExperiment
    ) : PaymentSheetEvent() {
        override val eventName: String = "elements.experiment_exposure"
        override val additionalParams: Map<String, Any?> = mapOf(
            "experiment_retrieved" to experiment.experiment.experimentValue,
            "arb_id" to experiment.arbId,
            "assignment_group" to experiment.group
        ) + experiment.dimensions.mapKeys { "dimensions-${it.key}" }
    }

    class ShopPayWebviewLoadAttempt(
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_shoppay_webview_load_attempt"
        override val additionalParams: Map<String, Any?> = emptyMap()
    }

    class ShopPayWebviewConfirmSuccess(
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_shoppay_webview_confirm_success"
        override val additionalParams: Map<String, Any?> = emptyMap()
    }

    class ShopPayWebviewCancelled(
        override val isDeferred: Boolean,
        override val isSpt: Boolean,
        override val linkEnabled: Boolean,
        override val googlePaySupported: Boolean,
        didReceiveECEClick: Boolean,
    ) : PaymentSheetEvent() {
        override val eventName: String = "mc_shoppay_webview_cancelled"
        override val additionalParams: Map<String, Any?> = mapOf(
            "did_receive_ece_click" to didReceiveECEClick
        )
    }

    private fun standardParams(
        isDecoupled: Boolean,
        isSpt: Boolean,
        linkEnabled: Boolean,
        googlePaySupported: Boolean,
    ): Map<String, Any?> = mapOf(
        FIELD_IS_DECOUPLED to isDecoupled,
        FIELD_IS_SPT to isSpt,
        FIELD_LINK_ENABLED to linkEnabled,
        FIELD_GOOGLE_PAY_ENABLED to googlePaySupported,
    )

    internal companion object {
        private fun analyticsValue(
            paymentSelection: PaymentSelection?
        ) = when (paymentSelection) {
            is PaymentSelection.GooglePay -> "googlepay"
            is PaymentSelection.Saved -> "savedpm"
            is PaymentSelection.Link,
            is PaymentSelection.New.LinkInline -> "link"
            is PaymentSelection.ExternalPaymentMethod,
            is PaymentSelection.CustomPaymentMethod,
            is PaymentSelection.New -> "newpm"
            null -> "unknown"
            is PaymentSelection.ShopPay -> "shop_pay"
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
        const val FIELD_CUSTOM_PAYMENT_METHODS = "custom_payment_methods"
        const val FIELD_PAYMENT_METHOD_ORDER = "payment_method_order"
        const val FIELD_IS_DECOUPLED = "is_decoupled"
        const val FIELD_IS_SPT = "is_spt"
        const val FIELD_DEFERRED_INTENT_CONFIRMATION_TYPE = "deferred_intent_confirmation_type"
        const val FIELD_DURATION = "duration"
        const val FIELD_LINK_ENABLED = "link_enabled"
        const val FIELD_CURRENCY = "currency"
        const val FIELD_SELECTED_LPM = "selected_lpm"
        const val FIELD_ERROR_MESSAGE = "error_message"
        const val FIELD_ERROR_CODE = "error_code"
        const val FIELD_CBC_EVENT_SOURCE = "cbc_event_source"
        const val FIELD_PAYMENT_METHOD_TYPE = "payment_method_type"
        const val FIELD_SET_AS_DEFAULT_ENABLED = "set_as_default_enabled"
        const val FIELD_HAS_DEFAULT_PAYMENT_METHOD = "has_default_payment_method"
        const val FIELD_SELECTED_CARD_BRAND = "selected_card_brand"
        const val FIELD_SET_AS_DEFAULT = "set_as_default"
        const val FIELD_LINK_CONTEXT = "link_context"
        const val FIELD_EXTERNAL_PAYMENT_METHODS = "external_payment_methods"
        const val FIELD_PAYMENT_METHOD_LAYOUT = "payment_method_layout"
        const val FIELD_COMPOSE = "compose"
        const val FIELD_INTENT_TYPE = "intent_type"
        const val FIELD_LINK_MODE = "link_mode"
        const val FIELD_ORDERED_LPMS = "ordered_lpms"
        const val FIELD_REQUIRE_CVC_RECOLLECTION = "require_cvc_recollection"
        const val FC_SDK_AVAILABILITY = "fc_sdk_availability"
        const val INTENT_ID = "intent_id"
        const val INTENT_TYPE = "intent_type"
        const val LINK_ACCOUNT_SESSION_ID = "link_account_session_id"
        const val FC_SDK_RESULT = "fc_sdk_result"
        const val FIELD_CARD_BRAND_ACCEPTANCE = "card_brand_acceptance"
        const val FIELD_CARD_SCAN_AVAILABLE = "card_scan_available"
        const val FIELD_ANALYTIC_CALLBACK_SET = "analytic_callback_set"
        const val FIELD_LINK_DISPLAY = "link_display"
        const val FIELD_PAYMENT_METHOD_OPTIONS_SETUP_FUTURE_USAGE = "payment_method_options_setup_future_usage"
        const val FIELD_SETUP_FUTURE_USAGE = "setup_future_usage"
        const val FIELD_ROW_SELECTION_BEHAVIOR = "row_selection_behavior"

        const val VALUE_EDIT_CBC_EVENT_SOURCE = "edit"
        const val VALUE_ADD_CBC_EVENT_SOURCE = "add"
        const val VALUE_CARD_BRAND = "brand"

        const val MAX_EXTERNAL_PAYMENT_METHODS = 10
    }
}

private fun FinancialConnectionsAvailability?.toAnalyticsParam(): String = when (this) {
    FinancialConnectionsAvailability.Full -> "FULL"
    FinancialConnectionsAvailability.Lite -> "LITE"
    null -> "NONE"
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

@Suppress("DEPRECATION")
internal fun PaymentSheet.Configuration.primaryButtonColorUsage(): Boolean = primaryButtonColor != null
