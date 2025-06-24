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
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.CompleteLinkFlow
import com.stripe.android.link.confirmation.CompleteLinkFlow.Result
import com.stripe.android.link.confirmation.DefaultCompleteLinkFlow
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.completePaymentButtonLabel
import com.stripe.android.link.utils.effectiveBillingDetails
import com.stripe.android.link.withDismissalDisabled
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
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
    private val completeLinkFlow: CompleteLinkFlow,
    private val logger: Logger,
    private val formHelper: FormHelper,
    private val dismissalCoordinator: LinkDismissalCoordinator,
    private val linkLaunchMode: LinkLaunchMode,
    private val dismissWithResult: (LinkActivityResult) -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(
        PaymentMethodState(
            formElements = formHelper.formElementsForCode(PaymentMethod.Type.Card.code),
            formArguments = formHelper.createFormArguments(PaymentMethod.Type.Card.code),
            primaryButtonState = PrimaryButtonState.Disabled,
            primaryButtonLabel = completePaymentButtonLabel(configuration.stripeIntent, linkLaunchMode)
        )
    )

    val state: StateFlow<PaymentMethodState> = _state

    fun formValuesChanged(formValues: FormFieldValues?) {
        val paymentMethodCreateParams = formHelper.getPaymentMethodParams(
            formValues = formValues,
            selectedPaymentMethodCode = PaymentMethod.Type.Card.code
        )
        formHelper.onFormFieldValuesChanged(
            formValues = formValues,
            selectedPaymentMethodCode = PaymentMethod.Type.Card.code
        )
        _state.update {
            it.copy(
                paymentMethodCreateParams = paymentMethodCreateParams,
                primaryButtonState = if (paymentMethodCreateParams != null) {
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
            clearErrorMessage()
            updateButtonState(PrimaryButtonState.Processing)

            dismissalCoordinator.withDismissalDisabled {
                linkAccountManager.createCardPaymentDetails(paymentMethodCreateParams)
                    .fold(
                        onSuccess = { linkPaymentDetails ->
                            val cardMap = paymentMethodCreateParams.toParamMap()["card"] as? Map<*, *>?
                            performConfirmation(
                                paymentDetails = linkPaymentDetails,
                                cvc = cardMap?.get("cvc") as? String?
                            )
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
    }

    private suspend fun performConfirmation(
        paymentDetails: LinkPaymentDetails,
        cvc: String?
    ) {
        val result = completeLinkFlow(
            linkPaymentDetails = paymentDetails,
            linkAccount = linkAccount,
            cvc = cvc,
            linkLaunchMode = linkLaunchMode,
        )
        when (result) {
            is Result.Canceled -> Unit
            is Result.Failed -> _state.update { it.copy(errorMessage = result.error) }
            is Result.Completed -> dismissWithResult(result.linkActivityResult)
        }
    }

    private fun updateButtonState(state: PrimaryButtonState) {
        _state.update {
            it.copy(
                primaryButtonState = state
            )
        }
    }

    private fun clearErrorMessage() {
        _state.update {
            it.copy(errorMessage = null)
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
                    val paymentMethodMetadata = PaymentMethodMetadata.createForNativeLink(
                        configuration = parentComponent.configuration
                    ).copy(
                        // Use effective billing details to prefill billing details in new card flows
                        defaultBillingDetails = effectiveBillingDetails(
                            configuration = parentComponent.configuration,
                            linkAccount = linkAccount
                        )
                    )
                    PaymentMethodViewModel(
                        configuration = parentComponent.configuration,
                        linkAccount = linkAccount,
                        linkAccountManager = parentComponent.linkAccountManager,
                        completeLinkFlow = DefaultCompleteLinkFlow(
                            linkConfirmationHandler = parentComponent.linkConfirmationHandlerFactory.create(
                                confirmationHandler = parentComponent.viewModel.confirmationHandler
                            ),
                            linkAccountManager = parentComponent.linkAccountManager,
                            dismissalCoordinator = parentComponent.dismissalCoordinator,
                        ),
                        formHelper = DefaultFormHelper.create(
                            coroutineScope = parentComponent.viewModel.viewModelScope,
                            cardAccountRangeRepositoryFactory = parentComponent.cardAccountRangeRepositoryFactory,
                            paymentMethodMetadata = paymentMethodMetadata,
                            eventReporter = parentComponent.eventReporter,
                            savedStateHandle = parentComponent.viewModel.savedStateHandle,
                        ),
                        logger = parentComponent.logger,
                        dismissalCoordinator = parentComponent.dismissalCoordinator,
                        linkLaunchMode = parentComponent.linkLaunchMode,
                        dismissWithResult = dismissWithResult
                    )
                }
            }
        }
    }
}
