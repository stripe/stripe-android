package com.stripe.android.paymentsheet.analytics

import androidx.annotation.Keep
import com.stripe.android.common.analytics.experiment.LoggableExperiment
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.model.CardBrand
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal interface EventReporter {

    /**
     * PaymentSheet has been instantiated or FlowController has finished its configuration.
     */
    fun onInit(
        commonConfiguration: CommonConfiguration,
        appearance: PaymentSheet.Appearance,
        primaryButtonColor: Boolean?,
        paymentMethodLayout: PaymentSheet.PaymentMethodLayout?,
        isDeferred: Boolean,
    )

    /**
     * PaymentSheet or FlowController have started loading.
     */
    fun onLoadStarted(initializedViaCompose: Boolean)

    /**
     * PaymentSheet or FlowController have successfully loaded the information required to be
     * rendered.
     */
    fun onLoadSucceeded(
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
        setAsDefaultEnabled: Boolean?,
    )

    /**
     * PaymentSheet or FlowController have failed to load.
     */
    fun onLoadFailed(error: Throwable)

    /**
     * PaymentSheet or FlowController have failed to load from the Elements session endpoint.
     */
    fun onElementsSessionLoadFailed(error: Throwable)

    /**
     * PaymentSheet has been dismissed by pressing the close button.
     */
    fun onDismiss()

    /**
     * PaymentSheet is now being displayed and its first screen shows the customer's saved payment
     * methods.
     */
    fun onShowExistingPaymentOptions()

    fun onShowManageSavedPaymentMethods()

    /**
     * PaymentSheet is now being displayed and its first screen shows new payment methods.
     */
    fun onShowNewPaymentOptions()

    /**
     * The customer has selected one of the available payment methods in the payment method form.
     */
    fun onSelectPaymentMethod(
        code: PaymentMethodCode,
        isSaved: Boolean,
    )

    /**
     * The form shown in PaymentSheet after a user or system initiated change.
     */
    fun onPaymentMethodFormShown(
        code: PaymentMethodCode,
    )

    /**
     * The customer has interacted with the form of an available payment method.
     */
    fun onPaymentMethodFormInteraction(
        code: PaymentMethodCode,
    )

    /**
     * The customer has filled in the card number field in the card payment method form.
     */
    fun onCardNumberCompleted()

    /**
     * The customer has selected one of their existing payment methods.
     */
    fun onSelectPaymentOption(
        paymentSelection: PaymentSelection,
    )

    fun onDisallowedCardBrandEntered(brand: CardBrand)

    /**
     * The customer has pressed the confirm button.
     */
    fun onPressConfirmButton(
        paymentSelection: PaymentSelection?,
    )

    /**
     * Payment or setup have succeeded.
     */
    fun onPaymentSuccess(
        paymentSelection: PaymentSelection?,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
    )

    /**
     * Payment or setup have failed.
     */
    fun onPaymentFailure(
        paymentSelection: PaymentSelection?,
        error: PaymentSheetConfirmationError,
    )

    /**
     * The client was unable to parse the response from LUXE.
     */
    fun onLpmSpecFailure(errorMessage: String?)

    /**
     * The user has auto-filled a text field.
     */
    fun onAutofill(
        type: String,
    )

    /**
     * The customer has chosen to show the edit screen for an editable payment option.
     */
    fun onShowEditablePaymentOption()

    /**
     * The customer has chosen to hide the edit screen for an editable payment option.
     */
    fun onHideEditablePaymentOption()

    /**
     * User selected a card brand from the card brand choice dropdown.
     */
    fun onBrandChoiceSelected(
        source: CardBrandChoiceEventSource,
        selectedBrand: CardBrand,
    )

    /**
     * The customer has successfully updated the details of a payment method.
     */
    fun onUpdatePaymentMethodSucceeded(
        selectedBrand: CardBrand?,
    )

    /**
     * The customer has failed to update the details of a payment method.
     */
    fun onUpdatePaymentMethodFailed(
        selectedBrand: CardBrand?,
        error: Throwable,
    )

    /**
     * The customer has successfully set a payment method as the default.
     */
    fun onSetAsDefaultPaymentMethodSucceeded(
        paymentMethodType: String?,
    )

    /**
     * The customer has been exposed to an experiment.
     */
    fun onExperimentExposure(
        experiment: LoggableExperiment
    )

    /**
     * The customer has failed to set a payment method as the default.
     */
    fun onSetAsDefaultPaymentMethodFailed(
        paymentMethodType: String?,
        error: Throwable,
    )

    /**
     * The customer cannot properly return from Link payments or other LPM payments using
     * browser intents.
     *
     * @see <a href="https://docs.google.com/document/d/1nEfPEGpO7N7MmfifBW6jR-AJe9pWd_V9lKvKTIVht8c">
     *     Deep Linking issue for Mobile Android SDK</a>
     */
    fun onCannotProperlyReturnFromLinkAndOtherLPMs()

    enum class Mode(val code: String) {
        Complete("complete"),
        Custom("custom"),
        Embedded("embedded");

        @Keep
        override fun toString(): String = code
    }

    enum class CardBrandChoiceEventSource {
        Edit, Add
    }
}
