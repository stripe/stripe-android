package com.stripe.android.link.ui.paymentmenthod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.confirmation.Result
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.completePaymentButtonLabel
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.forms.FormFieldValues
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class PaymentMethodViewModel @Inject constructor(
    private val configuration: LinkConfiguration,
    private val linkAccount: LinkAccount,
    private val linkAccountManager: LinkAccountManager,
    private val linkConfirmationHandler: LinkConfirmationHandler,
    private val logger: Logger,
    private val formHelper: FormHelper,
    private val dismissWithResult: (LinkActivityResult) -> Unit
) : ViewModel() {
    private val _state = MutableStateFlow(
        PaymentMethodState(
            formElements = formHelper.formElementsForCode(PaymentMethod.Type.Card.code),
            formArguments = formHelper.createFormArguments(PaymentMethod.Type.Card.code),
            primaryButtonState = PrimaryButtonState.Disabled,
            primaryButtonLabel = completePaymentButtonLabel(configuration.stripeIntent)
        )
    )

    val state: StateFlow<PaymentMethodState> = _state

    fun formValuesChanged(formValues: FormFieldValues?) {
        val params = formHelper.getPaymentMethodParams(
            formValues = formValues,
            selectedPaymentMethodCode = PaymentMethod.Type.Card.code
        )
        _state.update {
            it.copy(
                paymentMethodCreateParams = params,
                primaryButtonState = if (params != null) {
                    PrimaryButtonState.Enabled
                } else {
                    PrimaryButtonState.Disabled
                }
            )
        }
    }

    fun onPayClicked() {
        val paymentMethodCreateParams = _state.value.paymentMethodCreateParams
        if (paymentMethodCreateParams == null) {
            logger.error("PaymentMethodViewModel: onPayClicked without paymentMethodCreateParams")
            return
        }
        viewModelScope.launch {
            updateButtonState(PrimaryButtonState.Processing)
            linkAccountManager.createCardPaymentDetails(paymentMethodCreateParams)
                .fold(
                    onSuccess = { linkPaymentDetails ->
                        val cardMap = paymentMethodCreateParams.toParamMap()["card"] as? Map<String, Any?>?
                        performConfirmation(linkPaymentDetails.paymentDetails, cardMap?.get("cvc") as? String?)
                        updateButtonState(PrimaryButtonState.Enabled)
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                errorMessage = error.stripeErrorMessage()
                            )
                        }
                        updateButtonState(PrimaryButtonState.Enabled)
                        logger.error(
                            msg = "PaymentMethodViewModel: Failed to create card payment details",
                            t = error
                        )
                    }
                )
        }
    }

    private suspend fun performConfirmation(paymentDetails: ConsumerPaymentDetails.PaymentDetails, cvc: String?) {
        val result = linkConfirmationHandler.confirm(
            paymentDetails = paymentDetails,
            linkAccount = linkAccount,
            cvc = cvc
        )
        when (result) {
            Result.Canceled -> Unit
            is Result.Failed -> {
                _state.update { it.copy(errorMessage = result.message) }
            }
            Result.Succeeded -> {
                dismissWithResult(LinkActivityResult.Completed)
            }
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
                        linkAccountManager = parentComponent.linkAccountManager,
                        linkConfirmationHandler = parentComponent.linkConfirmationHandlerFactory.create(
                            confirmationHandler = parentComponent.viewModel.confirmationHandler
                        ),
                        formHelper = DefaultFormHelper.create(
                            cardAccountRangeRepositoryFactory = parentComponent.cardAccountRangeRepositoryFactory,
                            paymentMethodMetadata = PaymentMethodMetadata.create(
                                configuration = parentComponent.configuration,
                            ),
                        ),
                        logger = parentComponent.logger,
                        dismissWithResult = dismissWithResult
                    )
                }
            }
        }
    }
}
