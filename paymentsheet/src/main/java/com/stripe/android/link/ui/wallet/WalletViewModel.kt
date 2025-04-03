package com.stripe.android.link.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.linkAccountUpdate
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.supportedPaymentMethodTypes
import com.stripe.android.link.ui.completePaymentButtonLabel
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.PaymentMethod.Type.Card
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.ui.core.elements.CardDetailsUtil.createExpiryDateFormFieldValues
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.stripe.android.link.confirmation.Result as LinkConfirmationResult

internal class WalletViewModel @Inject constructor(
    private val configuration: LinkConfiguration,
    private val linkAccount: LinkAccount,
    private val linkAccountManager: LinkAccountManager,
    private val linkConfirmationHandler: LinkConfirmationHandler,
    private val logger: Logger,
    private val navigate: (route: LinkScreen) -> Unit,
    private val navigateAndClearStack: (route: LinkScreen) -> Unit,
    private val dismissWithResult: (LinkActivityResult) -> Unit
) : ViewModel() {
    private val stripeIntent = configuration.stripeIntent

    private val _uiState = MutableStateFlow(
        value = WalletUiState(
            paymentDetailsList = emptyList(),
            selectedItem = null,
            isProcessing = false,
            hasCompleted = false,
            primaryButtonLabel = completePaymentButtonLabel(configuration.stripeIntent),
            // TODO(tillh-stripe) Update this as soon as adding bank accounts is supported
            canAddNewPaymentMethod = stripeIntent.paymentMethodTypes.contains(Card.code),
        )
    )

    val uiState: StateFlow<WalletUiState> = _uiState

    val expiryDateController = SimpleTextFieldController(
        textFieldConfig = DateConfig()
    )
    val cvcController = CvcController(
        cardBrandFlow = uiState.mapAsStateFlow {
            (it.selectedItem as? ConsumerPaymentDetails.Card)?.brand ?: CardBrand.Unknown
        }
    )

    init {
        _uiState.update {
            it.setProcessing()
        }

        viewModelScope.launch {
            loadPaymentDetails(selectedItemId = null)
        }

        viewModelScope.launch {
            expiryDateController.formFieldValue.collectLatest { input ->
                _uiState.update {
                    it.copy(expiryDateInput = input)
                }
            }
        }

        viewModelScope.launch {
            cvcController.formFieldValue.collectLatest { input ->
                _uiState.update {
                    it.copy(cvcInput = input)
                }
            }
        }
    }

    private suspend fun loadPaymentDetails(selectedItemId: String?) {
        linkAccountManager.listPaymentDetails(
            paymentMethodTypes = stripeIntent.supportedPaymentMethodTypes(linkAccount)
        ).fold(
            onSuccess = { response ->
                _uiState.update {
                    it.updateWithResponse(response, selectedItemId = selectedItemId)
                }

                if (response.paymentDetails.isEmpty()) {
                    navigateAndClearStack(LinkScreen.PaymentMethod)
                }
            },
            // If we can't load the payment details there's nothing to see here
            onFailure = ::onFatal
        )
    }

    private fun onFatal(fatalError: Throwable) {
        logger.error("WalletViewModel Fatal error: ", fatalError)
        dismissWithResult(
            LinkActivityResult.Failed(
                error = fatalError,
                linkAccountUpdate = linkAccountManager.linkAccountUpdate
            )
        )
    }

    fun onItemSelected(item: ConsumerPaymentDetails.PaymentDetails) {
        if (item != uiState.value.selectedItem) {
            expiryDateController.onRawValueChange("")
            cvcController.onRawValueChange("")
        }

        _uiState.update {
            it.copy(
                selectedItem = item,
                isExpanded = false,
            )
        }
    }

    fun onExpandedChanged(expanded: Boolean) {
        _uiState.update {
            it.copy(isExpanded = expanded)
        }
    }

    fun onPrimaryButtonClicked() {
        val paymentDetail = _uiState.value.selectedItem ?: return
        _uiState.update {
            it.copy(
                isProcessing = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            performPaymentConfirmation(paymentDetail)
        }
    }

    private suspend fun performPaymentConfirmation(
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
    ) {
        val card = selectedPaymentDetails as? ConsumerPaymentDetails.Card
        val isExpired = card != null && card.isExpired

        if (isExpired) {
            performPaymentDetailsUpdate(selectedPaymentDetails).fold(
                onSuccess = { result ->
                    val updatedPaymentDetails = result.paymentDetails.single {
                        it.id == selectedPaymentDetails.id
                    }
                    performPaymentConfirmation(updatedPaymentDetails)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            alertMessage = error.stripeErrorMessage(),
                            isProcessing = false
                        )
                    }
                }
            )
        } else {
            // Confirm payment with LinkConfirmationHandler
            performPaymentConfirmationWithCvc(
                selectedPaymentDetails = selectedPaymentDetails,
                cvc = cvcController.formFieldValue.value.takeIf { it.isComplete }?.value
            )
        }
    }

    private suspend fun performPaymentConfirmationWithCvc(
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
        cvc: String?
    ) {
        val result = linkConfirmationHandler.confirm(
            paymentDetails = selectedPaymentDetails,
            linkAccount = linkAccount,
            cvc = cvc
        )
        when (result) {
            LinkConfirmationResult.Canceled -> Unit
            is LinkConfirmationResult.Failed -> {
                _uiState.update {
                    it.copy(
                        errorMessage = result.message,
                        isProcessing = false
                    )
                }
            }
            LinkConfirmationResult.Succeeded -> {
                dismissWithResult(
                    LinkActivityResult.Completed(
                        linkAccountUpdate = LinkAccountUpdate.Value(null)
                    )
                )
            }
        }
    }

    private suspend fun performPaymentDetailsUpdate(
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails
    ): Result<ConsumerPaymentDetails> {
        val paymentMethodCreateParams = uiState.value.toPaymentMethodCreateParams()

        val updateParams = ConsumerPaymentDetailsUpdateParams(
            id = selectedPaymentDetails.id,
            isDefault = selectedPaymentDetails.isDefault,
            cardPaymentMethodCreateParamsMap = paymentMethodCreateParams.toParamMap()
        )

        return linkAccountManager.updatePaymentDetails(updateParams)
    }

    fun onPayAnotherWayClicked() {
        dismissWithResult(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.PayAnotherWay,
                linkAccountUpdate = linkAccountManager.linkAccountUpdate
            )
        )
    }

    fun onRemoveClicked(item: ConsumerPaymentDetails.PaymentDetails) {
        _uiState.update {
            it.setProcessing()
        }
        viewModelScope.launch {
            linkAccountManager.deletePaymentDetails(item.id)
                .fold(
                    onSuccess = {
                        loadPaymentDetails(selectedItemId = uiState.value.selectedItem?.id)
                    },
                    onFailure = { error ->
                        updateErrorMessageAndStopProcessing(
                            error = error,
                            loggerMessage = "Failed to delete payment method"
                        )
                    }
                )
        }
    }

    fun onSetDefaultClicked(item: ConsumerPaymentDetails.PaymentDetails) {
        _uiState.update {
            it.copy(
                cardBeingUpdated = item.id,
            )
        }
        viewModelScope.launch {
            val updateParams = ConsumerPaymentDetailsUpdateParams(
                id = item.id,
                isDefault = true,
                cardPaymentMethodCreateParamsMap = null
            )
            linkAccountManager.updatePaymentDetails(updateParams)
                .fold(
                    onSuccess = {
                        _uiState.update { state ->
                            state.copy(
                                paymentDetailsList = state.paymentDetailsList.map { details ->
                                    when (details) {
                                        is ConsumerPaymentDetails.BankAccount -> {
                                            details.copy(isDefault = item.id == details.id)
                                        }
                                        is ConsumerPaymentDetails.Card -> {
                                            details.copy(isDefault = item.id == details.id)
                                        }
                                        is ConsumerPaymentDetails.Passthrough -> details
                                    }
                                },
                                cardBeingUpdated = null
                            )
                        }
                    },
                    onFailure = { error ->
                        updateErrorMessageAndStopProcessing(
                            error = error,
                            loggerMessage = "Failed to set payment method as default"
                        )
                    }
                )
        }
    }

    fun onAddNewPaymentMethodClicked() {
        navigate(LinkScreen.PaymentMethod)
    }

    fun onDismissAlert() {
        _uiState.update {
            it.copy(alertMessage = null)
        }
    }

    private fun updateErrorMessageAndStopProcessing(
        error: Throwable,
        loggerMessage: String
    ) {
        logger.error(
            msg = "WalletViewModel: $loggerMessage",
            t = error
        )
        _uiState.update {
            it.copy(
                alertMessage = error.stripeErrorMessage(),
                isProcessing = false,
                cardBeingUpdated = null
            )
        }
    }

    companion object {
        fun factory(
            parentComponent: NativeLinkComponent,
            linkAccount: LinkAccount,
            navigate: (route: LinkScreen) -> Unit,
            navigateAndClearStack: (route: LinkScreen) -> Unit,
            dismissWithResult: (LinkActivityResult) -> Unit
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    WalletViewModel(
                        configuration = parentComponent.configuration,
                        linkAccountManager = parentComponent.linkAccountManager,
                        linkConfirmationHandler = parentComponent.linkConfirmationHandlerFactory.create(
                            confirmationHandler = parentComponent.viewModel.confirmationHandler
                        ),
                        logger = parentComponent.logger,
                        linkAccount = linkAccount,
                        navigate = navigate,
                        navigateAndClearStack = navigateAndClearStack,
                        dismissWithResult = dismissWithResult
                    )
                }
            }
        }
    }
}

private fun WalletUiState.toPaymentMethodCreateParams(): PaymentMethodCreateParams {
    val expiryDateValues = createExpiryDateFormFieldValues(expiryDateInput)
    return FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
        fieldValuePairs = expiryDateValues,
        code = Card.code,
        requiresMandate = false
    )
}
