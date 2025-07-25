package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormViewModel
import com.stripe.android.paymentsheet.state.PaymentElementLoader

@Suppress("EmptyFunctionBlock")
internal class FakeEventReporter : EventReporter {
    private val _paymentFailureCalls = Turbine<PaymentFailureCall>()
    val paymentFailureCalls: ReceiveTurbine<PaymentFailureCall> = _paymentFailureCalls

    private val _paymentSuccessCalls = Turbine<PaymentSuccessCall>()
    val paymentSuccessCalls: ReceiveTurbine<PaymentSuccessCall> = _paymentSuccessCalls

    private val _updatePaymentMethodSucceededCalls = Turbine<UpdatePaymentMethodSucceededCall>()
    val updatePaymentMethodSucceededCalls: ReceiveTurbine<UpdatePaymentMethodSucceededCall> =
        _updatePaymentMethodSucceededCalls

    private val _updatePaymentMethodFailedCalls = Turbine<UpdatePaymentMethodFailedCall>()
    val updatePaymentMethodFailedCalls: ReceiveTurbine<UpdatePaymentMethodFailedCall> =
        _updatePaymentMethodFailedCalls

    private val _setAsDefaultPaymentMethodFailedCalls = Turbine<SetAsDefaultPaymentMethodFailedCall>()
    val setAsDefaultPaymentMethodFailedCalls: ReceiveTurbine<SetAsDefaultPaymentMethodFailedCall> =
        _setAsDefaultPaymentMethodFailedCalls

    private val _setAsDefaultPaymentMethodSucceededCalls = Turbine<SetAsDefaultPaymentMethodSucceededCall>()
    val setAsDefaultPaymentMethodSucceededCalls: ReceiveTurbine<SetAsDefaultPaymentMethodSucceededCall> =
        _setAsDefaultPaymentMethodSucceededCalls

    private val _showEditablePaymentOptionCalls = Turbine<Unit>()
    val showEditablePaymentOptionCalls: ReceiveTurbine<Unit> = _showEditablePaymentOptionCalls

    private val _hideEditablePaymentOptionCalls = Turbine<Unit>()
    val hideEditablePaymentOptionCalls: ReceiveTurbine<Unit> = _hideEditablePaymentOptionCalls

    private val _cannotProperlyReturnFromLinkAndOtherLPMsCalls = Turbine<Unit>()
    val cannotProperlyReturnFromLinkAndOtherLPMsCalls: ReceiveTurbine<Unit> =
        _cannotProperlyReturnFromLinkAndOtherLPMsCalls

    private val _showNewPaymentOptionsCalls = Turbine<Unit>()
    val showNewPaymentOptionsCalls: ReceiveTurbine<Unit> = _showNewPaymentOptionsCalls

    private val _showManageSavedPaymentMethods = Turbine<Unit>()
    val showManageSavedPaymentMethods: ReceiveTurbine<Unit> = _showManageSavedPaymentMethods

    private val _experimentExposureCalls = Turbine<ExperimentExposureCall>()
    val experimentExposureCalls: ReceiveTurbine<ExperimentExposureCall> = _experimentExposureCalls

    private val _removePaymentMethodCalls = Turbine<RemovePaymentMethodCall>()
    val removePaymentMethodCalls: ReceiveTurbine<RemovePaymentMethodCall> = _removePaymentMethodCalls

    private val _formCompletedCalls = Turbine<FormCompletedCall>()
    val formCompletedCalls: ReceiveTurbine<FormCompletedCall> = _formCompletedCalls

    private val _pressConfirmButtonCalls = Turbine<PaymentSelection>()
    val pressConfirmButtonCalls: ReceiveTurbine<PaymentSelection> = _pressConfirmButtonCalls

    private val _usBankAccountFormEventCalls = Turbine<USBankAccountFormViewModel.AnalyticsEvent>()
    val usBankAccountFormEventCalls: ReceiveTurbine<USBankAccountFormViewModel.AnalyticsEvent> =
        _usBankAccountFormEventCalls

    fun validate() {
        _paymentFailureCalls.ensureAllEventsConsumed()
        _paymentSuccessCalls.ensureAllEventsConsumed()
        _updatePaymentMethodSucceededCalls.ensureAllEventsConsumed()
        _updatePaymentMethodFailedCalls.ensureAllEventsConsumed()
        _setAsDefaultPaymentMethodFailedCalls.ensureAllEventsConsumed()
        _setAsDefaultPaymentMethodSucceededCalls.ensureAllEventsConsumed()
        _showEditablePaymentOptionCalls.ensureAllEventsConsumed()
        _hideEditablePaymentOptionCalls.ensureAllEventsConsumed()
        _cannotProperlyReturnFromLinkAndOtherLPMsCalls.ensureAllEventsConsumed()
        _showNewPaymentOptionsCalls.ensureAllEventsConsumed()
        _showManageSavedPaymentMethods.ensureAllEventsConsumed()
        _experimentExposureCalls.ensureAllEventsConsumed()
        _removePaymentMethodCalls.ensureAllEventsConsumed()
        _formCompletedCalls.ensureAllEventsConsumed()
        _pressConfirmButtonCalls.ensureAllEventsConsumed()
        _usBankAccountFormEventCalls.ensureAllEventsConsumed()
    }

    override fun onInit(
        commonConfiguration: CommonConfiguration,
        appearance: PaymentSheet.Appearance,
        primaryButtonColor: Boolean?,
        configurationSpecificPayload: PaymentSheetEvent.ConfigurationSpecificPayload,
        isDeferred: Boolean
    ) {
    }

    override fun onLoadStarted(initializedViaCompose: Boolean) {
    }

    override fun onLoadSucceeded(
        paymentSelection: PaymentSelection?,
        linkEnabled: Boolean,
        linkMode: LinkMode?,
        googlePaySupported: Boolean,
        linkDisplay: PaymentSheet.LinkConfiguration.Display,
        currency: String?,
        initializationMode: PaymentElementLoader.InitializationMode,
        financialConectionsAvailability: FinancialConnectionsAvailability?,
        orderedLpms: List<String>,
        requireCvcRecollection: Boolean,
        hasDefaultPaymentMethod: Boolean?,
        setAsDefaultEnabled: Boolean?,
        paymentMethodOptionsSetupFutureUsage: Boolean,
        setupFutureUsage: StripeIntent.Usage?
    ) {
    }

    override fun onLoadFailed(error: Throwable) {
    }

    override fun onElementsSessionLoadFailed(error: Throwable) {
    }

    override fun onDismiss() {
    }

    override fun onShowExistingPaymentOptions() {
    }

    override fun onShowManageSavedPaymentMethods() {
        _showManageSavedPaymentMethods.add(Unit)
    }

    override fun onShowNewPaymentOptions() {
        _showNewPaymentOptionsCalls.add(Unit)
    }

    override fun onSelectPaymentMethod(code: PaymentMethodCode) {
    }

    override fun onRemoveSavedPaymentMethod(code: PaymentMethodCode) {
        _removePaymentMethodCalls.add(RemovePaymentMethodCall(code))
    }

    override fun onPaymentMethodFormShown(code: PaymentMethodCode) {
    }

    override fun onPaymentMethodFormInteraction(code: PaymentMethodCode) {
    }

    override fun onPaymentMethodFormCompleted(code: String) {
        _formCompletedCalls.add(
            FormCompletedCall(
                code = code
            )
        )
    }

    override fun onCardNumberCompleted() {
    }

    override fun onSelectPaymentOption(paymentSelection: PaymentSelection) {
    }

    override fun onPressConfirmButton(paymentSelection: PaymentSelection) {
        _pressConfirmButtonCalls.add(paymentSelection)
    }

    override fun onPaymentSuccess(
        paymentSelection: PaymentSelection,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?
    ) {
        _paymentSuccessCalls.add(
            PaymentSuccessCall(
                paymentSelection = paymentSelection,
                deferredIntentConfirmationType = deferredIntentConfirmationType
            )
        )
    }

    override fun onPaymentFailure(
        paymentSelection: PaymentSelection,
        error: PaymentSheetConfirmationError
    ) {
        _paymentFailureCalls.add(
            PaymentFailureCall(
                paymentSelection = paymentSelection,
                error = error,
            )
        )
    }

    override fun onLpmSpecFailure(errorMessage: String?) {
    }

    override fun onAutofill(type: String) {
    }

    override fun onShowEditablePaymentOption() {
        _showEditablePaymentOptionCalls.add(Unit)
    }

    override fun onHideEditablePaymentOption() {
        _hideEditablePaymentOptionCalls.add(Unit)
    }

    override fun onBrandChoiceSelected(source: EventReporter.CardBrandChoiceEventSource, selectedBrand: CardBrand) {}

    override fun onUpdatePaymentMethodSucceeded(selectedBrand: CardBrand?) {
        _updatePaymentMethodSucceededCalls.add(
            UpdatePaymentMethodSucceededCall(selectedBrand = selectedBrand)
        )
    }

    override fun onUpdatePaymentMethodFailed(selectedBrand: CardBrand?, error: Throwable) {
        _updatePaymentMethodFailedCalls.add(
            UpdatePaymentMethodFailedCall(selectedBrand = selectedBrand, error = error)
        )
    }

    override fun onSetAsDefaultPaymentMethodSucceeded(
        paymentMethodType: String?,
    ) {
        _setAsDefaultPaymentMethodSucceededCalls.add(
            SetAsDefaultPaymentMethodSucceededCall(
                paymentMethodType = paymentMethodType,
            )
        )
    }

    override fun onExperimentExposure(experiment: LoggableExperiment) {
        _experimentExposureCalls.add(
            ExperimentExposureCall(experiment)
        )
    }

    override fun onSetAsDefaultPaymentMethodFailed(
        paymentMethodType: String?,
        error: Throwable,
    ) {
        _setAsDefaultPaymentMethodFailedCalls.add(
            SetAsDefaultPaymentMethodFailedCall(
                paymentMethodType = paymentMethodType,
                error = error,
            )
        )
    }

    override fun onCannotProperlyReturnFromLinkAndOtherLPMs() {
        _cannotProperlyReturnFromLinkAndOtherLPMsCalls.add(Unit)
    }

    override fun onUsBankAccountFormEvent(event: USBankAccountFormViewModel.AnalyticsEvent) {
        _usBankAccountFormEventCalls.add(event)
    }

    override fun onDisallowedCardBrandEntered(brand: CardBrand) {
    }

    override fun onAnalyticsEvent(event: AnalyticsEvent) {
    }

    override fun onShopPayWebViewLoadAttempt() {
    }

    override fun onShopPayWebViewConfirmSuccess() {
    }

    override fun onShopPayWebViewCancelled(didReceiveECEClick: Boolean) {
    }

    data class PaymentFailureCall(
        val paymentSelection: PaymentSelection,
        val error: PaymentSheetConfirmationError
    )

    data class PaymentSuccessCall(
        val paymentSelection: PaymentSelection,
        val deferredIntentConfirmationType: DeferredIntentConfirmationType?
    )

    data class UpdatePaymentMethodSucceededCall(
        val selectedBrand: CardBrand?,
    )

    data class UpdatePaymentMethodFailedCall(
        val selectedBrand: CardBrand?,
        val error: Throwable,
    )

    class SetAsDefaultPaymentMethodSucceededCall(
        val paymentMethodType: String?,
    )

    data class SetAsDefaultPaymentMethodFailedCall(
        val paymentMethodType: String?,
        val error: Throwable,
    )

    data class ExperimentExposureCall(
        val experiment: LoggableExperiment
    )

    data class RemovePaymentMethodCall(
        val code: PaymentMethodCode
    )

    data class FormCompletedCall(
        val code: PaymentMethodCode
    )
}
