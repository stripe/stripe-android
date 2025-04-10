package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
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
    }

    override fun onInit(
        commonConfiguration: CommonConfiguration,
        appearance: PaymentSheet.Appearance,
        primaryButtonColor: Boolean?,
        paymentMethodLayout: PaymentSheet.PaymentMethodLayout?,
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
        orderedLpms: List<String>,
        requireCvcRecollection: Boolean,
        hasDefaultPaymentMethod: Boolean?,
        setAsDefaultEnabled: Boolean?
    ) {}

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

    override fun onSelectPaymentMethod(code: PaymentMethodCode, isSaved: Boolean) {
    }

    override fun onRemoveSavedPaymentMethod(code: PaymentMethodCode) {
    }

    override fun onPaymentMethodFormShown(code: PaymentMethodCode) {
    }

    override fun onPaymentMethodFormInteraction(code: PaymentMethodCode) {
    }

    override fun onCardNumberCompleted() {
    }

    override fun onSelectPaymentOption(paymentSelection: PaymentSelection) {
    }

    override fun onPressConfirmButton(paymentSelection: PaymentSelection?) {
    }

    override fun onPaymentSuccess(
        paymentSelection: PaymentSelection?,
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
        paymentSelection: PaymentSelection?,
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

    override fun onDisallowedCardBrandEntered(brand: CardBrand) {
    }

    data class PaymentFailureCall(
        val paymentSelection: PaymentSelection?,
        val error: PaymentSheetConfirmationError
    )

    data class PaymentSuccessCall(
        val paymentSelection: PaymentSelection?,
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
}
