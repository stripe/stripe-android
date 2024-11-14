package com.stripe.android.paymentsheet.analytics

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.confirmation.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader

@Suppress("EmptyFunctionBlock")
internal class FakeEventReporter : EventReporter {
    private val _paymentFailureCalls = Turbine<PaymentFailureCall>()
    val paymentFailureCalls: ReceiveTurbine<PaymentFailureCall> = _paymentFailureCalls

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
        requireCvcRecollection: Boolean
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
    }

    override fun onHideEditablePaymentOption() {
    }

    override fun onShowPaymentOptionBrands(source: EventReporter.CardBrandChoiceEventSource, selectedBrand: CardBrand) {
    }

    override fun onHidePaymentOptionBrands(
        source: EventReporter.CardBrandChoiceEventSource,
        selectedBrand: CardBrand?
    ) {
    }

    override fun onUpdatePaymentMethodSucceeded(selectedBrand: CardBrand) {
    }

    override fun onUpdatePaymentMethodFailed(selectedBrand: CardBrand, error: Throwable) {
    }

    override fun onCannotProperlyReturnFromLinkAndOtherLPMs() {
    }

    override fun onDisallowedCardBrandEntered(brand: CardBrand) {
    }

    data class PaymentFailureCall(
        val paymentSelection: PaymentSelection?,
        val error: PaymentSheetConfirmationError
    )
}
