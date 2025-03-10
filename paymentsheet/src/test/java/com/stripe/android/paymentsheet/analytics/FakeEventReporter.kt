package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
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

    fun validate() {
        _paymentFailureCalls.ensureAllEventsConsumed()
        _updatePaymentMethodSucceededCalls.ensureAllEventsConsumed()
        _updatePaymentMethodFailedCalls.ensureAllEventsConsumed()
        _setAsDefaultPaymentMethodFailedCalls.ensureAllEventsConsumed()
        _setAsDefaultPaymentMethodSucceededCalls.ensureAllEventsConsumed()
        _showEditablePaymentOptionCalls.ensureAllEventsConsumed()
        _hideEditablePaymentOptionCalls.ensureAllEventsConsumed()
    }

    override fun onInit(configuration: PaymentSheet.Configuration, isDeferred: Boolean) {
    }

    override fun onLoadStarted(initializedViaCompose: Boolean) {
    }

    override fun onLoadSucceeded(
        paymentSelection: PaymentSelection?,
        linkMode: LinkMode?,
        googlePaySupported: Boolean,
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

    override fun onShowNewPaymentOptions() {
    }

    override fun onSelectPaymentMethod(code: PaymentMethodCode) {
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

    override fun onShowPaymentOptionBrands(source: EventReporter.CardBrandChoiceEventSource, selectedBrand: CardBrand) {
    }

    override fun onHidePaymentOptionBrands(
        source: EventReporter.CardBrandChoiceEventSource,
        selectedBrand: CardBrand?
    ) {
    }

    override fun onUpdatePaymentMethodSucceeded(selectedBrand: CardBrand) {
        _updatePaymentMethodSucceededCalls.add(
            UpdatePaymentMethodSucceededCall(selectedBrand = selectedBrand)
        )
    }

    override fun onUpdatePaymentMethodFailed(selectedBrand: CardBrand, error: Throwable) {
        _updatePaymentMethodFailedCalls.add(
            UpdatePaymentMethodFailedCall(selectedBrand = selectedBrand, error = error)
        )
    }

    override fun onSetAsDefaultPaymentMethodSucceeded() {
        _setAsDefaultPaymentMethodSucceededCalls.add(
            SetAsDefaultPaymentMethodSucceededCall()
        )
    }

    override fun onSetAsDefaultPaymentMethodFailed(error: Throwable) {
        _setAsDefaultPaymentMethodFailedCalls.add(
            SetAsDefaultPaymentMethodFailedCall(error = error)
        )
    }

    override fun onCannotProperlyReturnFromLinkAndOtherLPMs() {
    }

    override fun onDisallowedCardBrandEntered(brand: CardBrand) {
    }

    data class PaymentFailureCall(
        val paymentSelection: PaymentSelection?,
        val error: PaymentSheetConfirmationError
    )

    data class UpdatePaymentMethodSucceededCall(
        val selectedBrand: CardBrand,
    )

    data class UpdatePaymentMethodFailedCall(
        val selectedBrand: CardBrand,
        val error: Throwable,
    )

    class SetAsDefaultPaymentMethodSucceededCall

    data class SetAsDefaultPaymentMethodFailedCall(
        val error: Throwable,
    )
}
