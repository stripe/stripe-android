package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.previouslySentDeepLinkEvent
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface EmbeddedSheetAnalytics {
    fun attach(screen: StateFlow<EmbeddedNavigator.Screen>)

    fun onScreenShown(screen: EmbeddedNavigator.Screen)

    fun onScreenHidden(screen: EmbeddedNavigator.Screen)

    fun onFormInteraction(code: PaymentMethodCode)

    fun onListPrimaryButtonPressed(selection: PaymentSelection?)

    fun onSelectionReturned(selection: PaymentSelection)

    fun onPresented(isApplicationTaskOwner: Boolean)

    fun onClosed(result: EmbeddedActivityResult?)
}

internal class DefaultEmbeddedSheetAnalytics @Inject constructor(
    private val eventReporter: EventReporter,
) : EmbeddedSheetAnalytics {
    override fun attach(screen: StateFlow<EmbeddedNavigator.Screen>) = Unit

    override fun onScreenShown(screen: EmbeddedNavigator.Screen) {
        when (screen) {
            is EmbeddedNavigator.Screen.ManageAll -> eventReporter.onShowManageSavedPaymentMethods()
            is EmbeddedNavigator.Screen.ManageUpdate -> eventReporter.onShowEditablePaymentOption()
            is EmbeddedNavigator.Screen.Form -> Unit
            is EmbeddedNavigator.Screen.SavedPaymentMethodConfirm -> Unit
            is EmbeddedNavigator.Screen.VerticalPaymentOptions -> eventReporter.onShowNewPaymentOptions()
            is EmbeddedNavigator.Screen.HorizontalPaymentOptions -> eventReporter.onShowNewPaymentOptions()
            is EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions -> eventReporter.onShowExistingPaymentOptions()
        }
    }

    override fun onScreenHidden(screen: EmbeddedNavigator.Screen) {
        if (screen is EmbeddedNavigator.Screen.ManageUpdate) {
            eventReporter.onHideEditablePaymentOption()
        }
    }

    override fun onFormInteraction(code: PaymentMethodCode) {
        eventReporter.onPaymentMethodFormInteraction(code)
    }

    override fun onListPrimaryButtonPressed(selection: PaymentSelection?) = Unit

    override fun onSelectionReturned(selection: PaymentSelection) = Unit

    override fun onPresented(isApplicationTaskOwner: Boolean) = Unit

    override fun onClosed(result: EmbeddedActivityResult?) {
        eventReporter.onDismiss()
    }
}

/**
 * Reports the same payment option events that FlowController reported before it presented the embedded sheet:
 * sheet shown events once per presentation, form events once per form, a selection event for every returned
 * selection, and a dismiss event only when the customer cancels.
 */
internal class FlowControllerEmbeddedSheetAnalytics @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val eventReporter: EventReporter,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : EmbeddedSheetAnalytics {
    private var previouslyPresentedSheet: Boolean
        get() = savedStateHandle[PREVIOUSLY_PRESENTED_SHEET] ?: false
        set(value) {
            savedStateHandle[PREVIOUSLY_PRESENTED_SHEET] = value
        }

    private var previouslyShownForm: PaymentMethodCode?
        get() = savedStateHandle[PREVIOUSLY_SHOWN_PAYMENT_FORM]
        set(value) {
            savedStateHandle[PREVIOUSLY_SHOWN_PAYMENT_FORM] = value
        }

    private var previouslyInteractedForm: PaymentMethodCode?
        get() = savedStateHandle[PREVIOUSLY_INTERACTION_PAYMENT_FORM]
        set(value) {
            savedStateHandle[PREVIOUSLY_INTERACTION_PAYMENT_FORM] = value
        }

    override fun attach(screen: StateFlow<EmbeddedNavigator.Screen>) {
        coroutineScope.launch {
            screen.collect(::reportScreenShown)
        }
    }

    // Shown screens are reported from the current screen in attach, which also covers screens revealed by back.
    override fun onScreenShown(screen: EmbeddedNavigator.Screen) = Unit

    override fun onScreenHidden(screen: EmbeddedNavigator.Screen) {
        if (screen is EmbeddedNavigator.Screen.ManageUpdate) {
            eventReporter.onHideEditablePaymentOption()
        }
    }

    override fun onFormInteraction(code: PaymentMethodCode) {
        if (previouslyInteractedForm != code) {
            eventReporter.onPaymentMethodFormInteraction(code)
            previouslyInteractedForm = code
        }
    }

    override fun onListPrimaryButtonPressed(selection: PaymentSelection?) {
        selection?.let(eventReporter::onPressConfirmButton)
    }

    override fun onSelectionReturned(selection: PaymentSelection) {
        eventReporter.onSelectPaymentOption(selection)
    }

    override fun onPresented(isApplicationTaskOwner: Boolean) {
        if (!isApplicationTaskOwner && !savedStateHandle.previouslySentDeepLinkEvent) {
            eventReporter.onCannotProperlyReturnFromLinkAndOtherLPMs()
            savedStateHandle.previouslySentDeepLinkEvent = true
        }
    }

    override fun onClosed(result: EmbeddedActivityResult?) {
        if (result is EmbeddedActivityResult.Cancelled) {
            eventReporter.onDismiss()
        }
    }

    private fun reportScreenShown(screen: EmbeddedNavigator.Screen) {
        when (screen) {
            is EmbeddedNavigator.Screen.SavedPaymentMethodConfirm -> Unit
            is EmbeddedNavigator.Screen.ManageAll -> eventReporter.onShowManageSavedPaymentMethods()
            is EmbeddedNavigator.Screen.ManageUpdate -> eventReporter.onShowEditablePaymentOption()
            is EmbeddedNavigator.Screen.HorizontalSavedPaymentOptions -> {
                reportPaymentOptions(isSaved = true)
                previouslyShownForm = null
                previouslyInteractedForm = null
            }
            is EmbeddedNavigator.Screen.VerticalPaymentOptions -> reportPaymentOptions(isSaved = false)
            is EmbeddedNavigator.Screen.Form -> {
                // Forms opened from the vertical list are reported by its interactor.
                if (!previouslyPresentedSheet) {
                    reportFormShown(screen.paymentMethodCode)
                    reportPaymentOptions(isSaved = false)
                }
            }
            is EmbeddedNavigator.Screen.HorizontalPaymentOptions -> {
                reportFormShown(screen.selectedPaymentMethodCode)
                reportPaymentOptions(isSaved = false)
            }
        }
    }

    private fun reportPaymentOptions(isSaved: Boolean) {
        if (!previouslyPresentedSheet) {
            if (isSaved) {
                eventReporter.onShowExistingPaymentOptions()
            } else {
                eventReporter.onShowNewPaymentOptions()
            }
        }
        previouslyPresentedSheet = true
    }

    private fun reportFormShown(code: PaymentMethodCode) {
        if (previouslyShownForm != code) {
            eventReporter.onPaymentMethodFormShown(code)
            previouslyShownForm = code
        }
    }

    private companion object {
        const val PREVIOUSLY_SHOWN_PAYMENT_FORM = "flow_controller_previously_shown_payment_form"
        const val PREVIOUSLY_INTERACTION_PAYMENT_FORM = "flow_controller_previously_interacted_payment_form"
        const val PREVIOUSLY_PRESENTED_SHEET = "flow_controller_previously_presented_sheet"
    }
}
