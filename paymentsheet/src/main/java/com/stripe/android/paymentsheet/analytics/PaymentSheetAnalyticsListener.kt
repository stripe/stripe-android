package com.stripe.android.paymentsheet.analytics

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddFirstPaymentMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class PaymentSheetAnalyticsListener(
    private val savedStateHandle: SavedStateHandle,
    private val eventReporter: EventReporter,
    currentScreen: Flow<PaymentSheetScreen>,
    coroutineScope: CoroutineScope,
    private val currentPaymentMethodTypeProvider: () -> String
) {
    private var previouslySentDeepLinkEvent: Boolean
        get() = savedStateHandle[PREVIOUSLY_SENT_DEEP_LINK_EVENT] ?: false
        set(value) {
            savedStateHandle[PREVIOUSLY_SENT_DEEP_LINK_EVENT] = value
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

    init {
        coroutineScope.launch {
            currentScreen.collectLatest { screen ->
                reportPaymentSheetShown(screen)
            }
        }
    }

    fun cannotProperlyReturnFromLinkAndOtherLPMs() {
        if (!previouslySentDeepLinkEvent) {
            eventReporter.onCannotProperlyReturnFromLinkAndOtherLPMs()

            previouslySentDeepLinkEvent = true
        }
    }

    fun reportFieldInteraction(code: PaymentMethodCode) {
        /*
         * Prevents this event from being reported multiple times on field interactions
         * on the same payment form. We should have one field interaction event for
         * every form shown event triggered.
         */
        if (previouslyInteractedForm != code) {
            eventReporter.onPaymentMethodFormInteraction(code)
            previouslyInteractedForm = code
        }
    }

    private fun reportPaymentSheetShown(currentScreen: PaymentSheetScreen) {
        when (currentScreen) {
            is PaymentSheetScreen.Loading,
            is PaymentSheetScreen.VerticalModeForm,
            is PaymentSheetScreen.ManageOneSavedPaymentMethod,
            is PaymentSheetScreen.ManageSavedPaymentMethods,
            is PaymentSheetScreen.UpdatePaymentMethod,
            is PaymentSheetScreen.CvcRecollection -> {
                // Nothing to do here
            }
            is PaymentSheetScreen.EditPaymentMethod -> {
                eventReporter.onShowEditablePaymentOption()
            }
            is PaymentSheetScreen.SelectSavedPaymentMethods -> {
                eventReporter.onShowExistingPaymentOptions()
                previouslyShownForm = null
                previouslyInteractedForm = null
            }
            is PaymentSheetScreen.VerticalMode -> {
                eventReporter.onShowNewPaymentOptions()
            }
            is AddFirstPaymentMethod, is AddAnotherPaymentMethod -> {
                reportFormShown(currentPaymentMethodTypeProvider())
                eventReporter.onShowNewPaymentOptions()
            }
        }
    }

    fun reportPaymentSheetHidden(hiddenScreen: PaymentSheetScreen) {
        when (hiddenScreen) {
            is PaymentSheetScreen.EditPaymentMethod -> {
                eventReporter.onHideEditablePaymentOption()
            }
            else -> {
                // Events for hiding other screens not supported
            }
        }
    }

    private fun reportFormShown(code: String) {
        /*
         * Prevents this event from being reported multiple times on the same payment form after process death. We
         * should only trigger a form shown event when initially shown in the add payment method screen or the user
         * navigates to a different form.
         */
        if (previouslyShownForm != code) {
            eventReporter.onPaymentMethodFormShown(code)
            previouslyShownForm = code
        }
    }

    companion object {
        internal const val PREVIOUSLY_SHOWN_PAYMENT_FORM = "previously_shown_payment_form"
        internal const val PREVIOUSLY_INTERACTION_PAYMENT_FORM = "previously_interacted_payment_form"
        internal const val PREVIOUSLY_SENT_DEEP_LINK_EVENT = "previously_sent_deep_link_event"
    }
}
