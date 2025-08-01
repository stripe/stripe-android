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
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.CompleteLinkFlow
import com.stripe.android.link.confirmation.CompleteLinkFlow.Result
import com.stripe.android.link.confirmation.DefaultCompleteLinkFlow
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.completePaymentButtonLabel
import com.stripe.android.link.withDismissalDisabled
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.addresselement.AUTOCOMPLETE_DEFAULT_COUNTRIES
import com.stripe.android.paymentsheet.addresselement.PaymentElementAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
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
                    .mapCatching { linkPaymentDetails ->
                        val shouldShare = configuration.passthroughModeEnabled &&
                            (linkLaunchMode as? LinkLaunchMode.PaymentMethodSelection)
                                ?.sharePaymentDetailsImmediatelyAfterCreation != false
                        if (shouldShare) {
                            linkAccountManager.shareCardPaymentDetails(linkPaymentDetails).getOrThrow()
                        } else {
                            linkPaymentDetails
                        }
                    }
                    .fold(
                        onSuccess = { linkPaymentDetails ->
                            val params = paymentMethodCreateParams.toParamMap()
                            val cardMap = params["card"] as? Map<*, *>?
                            val billingDetailsMap = params["billing_details"] as? Map<*, *>?
                            attemptCompletion(
                                paymentDetails = linkPaymentDetails,
                                cvc = cardMap?.get("cvc") as? String?,
                                billingPhone = billingDetailsMap?.get("phone") as? String?
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

    private suspend fun attemptCompletion(
        paymentDetails: LinkPaymentDetails,
        cvc: String?,
        billingPhone: String?
    ) {
        val result = completeLinkFlow(
            selectedPaymentDetails = LinkPaymentMethod.LinkPaymentDetails(
                linkPaymentDetails = paymentDetails,
                collectedCvc = cvc,
                billingPhone = billingPhone
            ),
            linkAccount = linkAccount
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
                            linkLaunchMode = parentComponent.linkLaunchMode
                        ),
                        formHelper = DefaultFormHelper.create(
                            coroutineScope = parentComponent.viewModel.viewModelScope,
                            cardAccountRangeRepositoryFactory = parentComponent.cardAccountRangeRepositoryFactory,
                            paymentMethodMetadata = PaymentMethodMetadata.createForNativeLink(
                                configuration = parentComponent.configuration,
                                linkAccount = linkAccount,
                            ),
                            eventReporter = parentComponent.eventReporter,
                            savedStateHandle = parentComponent.viewModel.savedStateHandle,
                            autocompleteAddressInteractorFactory =
                            PaymentElementAutocompleteAddressInteractor.Factory(
                                launcher = parentComponent.autocompleteLauncher,
                                autocompleteConfig = AutocompleteAddressInteractor.Config(
                                    googlePlacesApiKey = parentComponent.configuration.googlePlacesApiKey,
                                    autocompleteCountries = AUTOCOMPLETE_DEFAULT_COUNTRIES,
                                )
                            ),
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
