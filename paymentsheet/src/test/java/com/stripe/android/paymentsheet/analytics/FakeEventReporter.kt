package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormViewModel
import com.stripe.android.paymentsheet.state.WalletsState

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

    private val _tapToAddButtonShownCalls = Turbine<Unit>()
    val tapToAddButtonShownCalls: ReceiveTurbine<Unit> = _tapToAddButtonShownCalls

    private val _tapToAddStartedCalls = Turbine<Unit>()
    val tapToAddStartedCalls: ReceiveTurbine<Unit> = _tapToAddStartedCalls

    private val _tapToAddCardAddedCalls = Turbine<Boolean>()
    val tapToAddCardAddedCalls: ReceiveTurbine<Boolean> = _tapToAddCardAddedCalls

    private val _tapToAddCanceledCalls = Turbine<EventReporter.TapToAddCancelSource>()
    val tapToAddCanceledCalls: ReceiveTurbine<EventReporter.TapToAddCancelSource> =
        _tapToAddCanceledCalls

    private val _tapToAddContinueAfterCardAddedCalls = Turbine<Boolean?>()
    val tapToAddContinueAfterCardAddedCalls: ReceiveTurbine<Boolean?> =
        _tapToAddContinueAfterCardAddedCalls

    private val _tapToAddConfirmCalls = Turbine<Boolean>()
    val tapToAddConfirmCalls: ReceiveTurbine<Boolean> = _tapToAddConfirmCalls

    private val _failedToAddCardWithTapToAddCalls = Turbine<String>()
    val failedToAddCardWithTapToAddCalls: ReceiveTurbine<String> = _failedToAddCardWithTapToAddCalls

    private val _tapToAddAttemptWithUnsupportedDeviceCalls = Turbine<Unit>()
    val tapToAddAttemptWithUnsupportedDeviceCalls: ReceiveTurbine<Unit> =
        _tapToAddAttemptWithUnsupportedDeviceCalls

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
        _tapToAddButtonShownCalls.ensureAllEventsConsumed()
        _tapToAddStartedCalls.ensureAllEventsConsumed()
        _tapToAddCardAddedCalls.ensureAllEventsConsumed()
        _tapToAddCanceledCalls.ensureAllEventsConsumed()
        _tapToAddContinueAfterCardAddedCalls.ensureAllEventsConsumed()
        _tapToAddConfirmCalls.ensureAllEventsConsumed()
        _failedToAddCardWithTapToAddCalls.ensureAllEventsConsumed()
        _tapToAddAttemptWithUnsupportedDeviceCalls.ensureAllEventsConsumed()
    }

    override fun onInit() {
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
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        intentId: String?,
    ) {
        _paymentSuccessCalls.add(
            PaymentSuccessCall(
                paymentSelection = paymentSelection,
                deferredIntentConfirmationType = deferredIntentConfirmationType,
                intentId = intentId,
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

    override fun onCardScanStarted(implementation: String) {
    }

    override fun onCardScanSucceeded(implementation: String) {
    }

    override fun onCardScanFailed(implementation: String, error: Throwable?) {
    }

    override fun onCardScanCancelled(implementation: String) {
    }

    override fun onCardScanApiCheckSucceeded(implementation: String) {
    }

    override fun onCardScanApiCheckFailed(implementation: String, error: Throwable?) {
    }

    override fun onInitiallyDisplayedPaymentMethodVisibilitySnapshot(
        visiblePaymentMethods: List<String>,
        hiddenPaymentMethods: List<String>,
        walletsState: WalletsState?,
        isVerticalLayout: Boolean
    ) {
    }

    override fun onTapToAddButtonShown() {
        _tapToAddButtonShownCalls.add(Unit)
    }

    override fun onTapToAddStarted() {
        _tapToAddStartedCalls.add(Unit)
    }

    override fun onCardAddedWithTapToAdd(canCollectLinkInput: Boolean) {
        _tapToAddCardAddedCalls.add(canCollectLinkInput)
    }

    override fun onTapToAddCanceled(source: EventReporter.TapToAddCancelSource) {
        _tapToAddCanceledCalls.add(source)
    }

    override fun onTapToAddContinueAfterCardAdded(completedLinkInput: Boolean?) {
        _tapToAddContinueAfterCardAddedCalls.add(completedLinkInput)
    }

    override fun onTapToAddConfirm(recollectedCvc: Boolean) {
        _tapToAddConfirmCalls.add(recollectedCvc)
    }

    override fun onFailedToAddCardWithTapToAdd(message: String) {
        _failedToAddCardWithTapToAddCalls.add(message)
    }

    override fun onTapToAddAttemptWithUnsupportedDevice() {
        _tapToAddAttemptWithUnsupportedDeviceCalls.add(Unit)
    }

    data class PaymentFailureCall(
        val paymentSelection: PaymentSelection,
        val error: PaymentSheetConfirmationError
    )

    data class PaymentSuccessCall(
        val paymentSelection: PaymentSelection,
        val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        val intentId: String?,
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
