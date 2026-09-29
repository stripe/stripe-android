package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.lpmfoundations.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.FormHelper.FormType
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.paymentMethodType
import com.stripe.android.paymentsheet.ui.transformToPaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val PREVIOUSLY_COMPLETED_PAYMENT_FORM = "previously_completed_payment_form"

internal class DefaultFormHelper(
    private val coroutineScope: CoroutineScope,
    private val linkInlineHandler: LinkInlineHandler,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val selectionUpdater: (PaymentSelection?) -> Unit,
    private val eventReporter: EventReporter,
    private val savedStateHandle: SavedStateHandle,
    formDefinitionFactory: FormDefinitionFactory,
) : FormHelper, FormDefinitionFactory by formDefinitionFactory {
    private val lastFormValues = MutableSharedFlow<Pair<FormFieldValues?, String>>(replay = 1)

    private val paymentSelection: Flow<PaymentSelection?> = combine(
        lastFormValues,
        linkInlineHandler.linkInlineState,
    ) { formValues, inlineSignupViewState ->
        formValues.first?.transformToPaymentSelection(
            paymentMethod = supportedPaymentMethodForCode(formValues.second),
            paymentMethodMetadata = paymentMethodMetadata,
            inlineSignupViewState = inlineSignupViewState,
        )
    }

    private var previouslyCompletedForm: PaymentMethodCode?
        get() = savedStateHandle[PREVIOUSLY_COMPLETED_PAYMENT_FORM]
        set(value) {
            savedStateHandle[PREVIOUSLY_COMPLETED_PAYMENT_FORM] = value
        }

    init {
        coroutineScope.launch {
            paymentSelection.collect { selection ->
                selectionUpdater(selection)
                reportFieldCompleted(selection?.paymentMethodType)
            }
        }
    }

    override fun onFormFieldValuesChanged(formValues: FormFieldValues?, selectedPaymentMethodCode: String) {
        coroutineScope.launch {
            lastFormValues.emit(formValues to selectedPaymentMethodCode)
        }
    }

    private fun supportedPaymentMethodForCode(code: String): SupportedPaymentMethod {
        return requireNotNull(paymentMethodMetadata.supportedPaymentMethodForCode(code = code))
    }

    private fun reportFieldCompleted(code: PaymentMethodCode?) {
        if (code == null || formTypeForCode(code) != FormType.UserInteractionRequired) {
            return
        }
        /*
         * Prevents this event from being reported multiple times on field interactions
         * on the same payment form. We should have one field interaction event for
         * every form shown event triggered.
         */
        if (previouslyCompletedForm != code) {
            eventReporter.onPaymentMethodFormCompleted(code)
            previouslyCompletedForm = code
        }
    }
}
