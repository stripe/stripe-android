package com.stripe.android.link.ui.paymentmenthod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.completePaymentButtonLabel
import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.uicore.elements.FormElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

internal class PaymentMethodViewModel @Inject constructor(
    private val configuration: LinkConfiguration,
    private val formHelperFactory: (UpdateSelection) -> FormHelper
) : ViewModel() {
    private val formHelper = formHelperFactory(::updateSelection)
    private val _state = MutableStateFlow(
        PaymentMethodState(
            isProcessing = false,
            formElements = formHelper.formElementsForCode(PaymentMethod.Type.Card.code),
            formArguments = formHelper.createFormArguments(PaymentMethod.Type.Card.code),
            primaryButtonState = PrimaryButtonState.Disabled,
            primaryButtonLabel = completePaymentButtonLabel(configuration.stripeIntent)
        )
    )

    val state: StateFlow<PaymentMethodState> = _state

    fun formValuesChanged(formValues: FormFieldValues?) {
        formHelper.onFormFieldValuesChanged(
            formValues = formValues,
            selectedPaymentMethodCode = PaymentMethod.Type.Card.code
        )
    }

    private fun updateSelection(selection: PaymentSelection?) {
        _state.update {
            it.copy(
                paymentSelection = selection,
                primaryButtonState = if (selection != null) {
                    PrimaryButtonState.Enabled
                } else {
                    PrimaryButtonState.Disabled
                }
            )
        }
    }

    companion object {
        fun factory(
            parentComponent: NativeLinkComponent
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    PaymentMethodViewModel(
                        configuration = parentComponent.configuration,
                        formHelperFactory = { selectionUpdater ->
                            DefaultFormHelper.create(
                                cardAccountRangeRepositoryFactory = parentComponent.cardAccountRangeRepositoryFactory,
                                paymentMethodMetadata = PaymentMethodMetadata.create(
                                    configuration = parentComponent.configuration,
                                ),
                                selectionUpdater = selectionUpdater
                            )
                        }
                    )
                }
            }
        }
    }
}

internal data class State(
    val selectedPaymentMethodCode: String,
    val isProcessing: Boolean,
    val usBankAccountFormArguments: USBankAccountFormArguments,
    val formArguments: FormArguments,
    val formElements: List<FormElement>,
    val headerInformation: FormHeaderInformation?,
    val primaryButtonState: PrimaryButtonState,
    val primaryButtonLabel: ResolvableString,
    val paymentSelection: PaymentSelection? = null
)
