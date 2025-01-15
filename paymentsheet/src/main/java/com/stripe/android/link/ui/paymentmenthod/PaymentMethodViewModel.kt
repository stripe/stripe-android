package com.stripe.android.link.ui.paymentmenthod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.confirmation.Result
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.completePaymentButtonLabel
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class PaymentMethodViewModel @Inject constructor(
    private val configuration: LinkConfiguration,
    private val linkAccount: LinkAccount,
    private val linkConfirmationHandler: LinkConfirmationHandler,
    private val formHelperFactory: (UpdateSelection) -> FormHelper,
    private val dismissWithResult: (LinkActivityResult) -> Unit
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

    fun onPayClicked() {
        val paymentSelection = _state.value.paymentSelection ?: return
        viewModelScope.launch {
            updateButtonState(PrimaryButtonState.Processing)
            val result = linkConfirmationHandler.confirm(
                paymentSelection = paymentSelection,
                linkAccount = linkAccount
            )
            when (result) {
                Result.Canceled -> {
                    updateButtonState(PrimaryButtonState.Enabled)
                }
                is Result.Failed -> {
                    updateButtonState(PrimaryButtonState.Enabled)
                    _state.update { it.copy(errorMessage = result.message) }
                }
                Result.Succeeded -> {
                    dismissWithResult(LinkActivityResult.Completed)
                }
            }
        }
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

    private fun updateButtonState(state: PrimaryButtonState) {
        _state.update {
            it.copy(
                primaryButtonState = state
            )
        }
    }

    companion object {
        fun factory(
            parentComponent: NativeLinkComponent,
            linkAccount: LinkAccount,
            dismissWithResult: (LinkActivityResult) -> Unit
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    PaymentMethodViewModel(
                        configuration = parentComponent.configuration,
                        linkAccount = linkAccount,
                        linkConfirmationHandler = parentComponent.linkConfirmationHandlerFactory.create(
                            confirmationHandler = parentComponent.viewModel.confirmationHandler
                        ),
                        formHelperFactory = { selectionUpdater ->
                            DefaultFormHelper.create(
                                cardAccountRangeRepositoryFactory = parentComponent.cardAccountRangeRepositoryFactory,
                                paymentMethodMetadata = PaymentMethodMetadata.create(
                                    configuration = parentComponent.configuration,
                                ),
                                selectionUpdater = selectionUpdater
                            )
                        },
                        dismissWithResult = dismissWithResult
                    )
                }
            }
        }
    }
}
