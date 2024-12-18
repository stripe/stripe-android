package com.stripe.android.link.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.supportedPaymentMethodTypes
import com.stripe.android.link.ui.completePaymentButtonLabel
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
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
import kotlin.Result
import kotlin.fold
import com.stripe.android.link.confirmation.Result as LinkConfirmationResult

internal class WalletViewModel @Inject constructor(
    private val configuration: LinkConfiguration,
    private val linkAccount: LinkAccount,
    private val linkAccountManager: LinkAccountManager,
    private val linkConfirmationHandler: LinkConfirmationHandler,
    private val logger: Logger,
    private val navigate: (route: LinkScreen) -> Unit,
    private val confirmationHandler: ConfirmationHandler,
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
            primaryButtonLabel = completePaymentButtonLabel(configuration.stripeIntent)
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
        loadPaymentDetails()

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

    private fun loadPaymentDetails() {
        _uiState.update {
            it.setProcessing()
        }

        viewModelScope.launch {
            linkAccountManager.listPaymentDetails(
                paymentMethodTypes = stripeIntent.supportedPaymentMethodTypes(linkAccount)
            ).fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.updateWithResponse(response)
                    }

                    if (response.paymentDetails.isEmpty()) {
                        navigateAndClearStack(LinkScreen.PaymentMethod)
                    }
                },
                // If we can't load the payment details there's nothing to see here
                onFailure = ::onFatal
            )
        }
    }

    private fun onFatal(fatalError: Throwable) {
        logger.error("WalletViewModel Fatal error: ", fatalError)
        dismissWithResult(LinkActivityResult.Failed(fatalError))
    }

    fun onItemSelected(item: ConsumerPaymentDetails.PaymentDetails) {
        if (item == uiState.value.selectedItem) return

        expiryDateController.onRawValueChange("")
        cvcController.onRawValueChange("")

        _uiState.update {
            it.copy(selectedItem = item)
        }
    }

    fun onPrimaryButtonClicked() {
        val paymentDetail = _uiState.value.selectedItem ?: return
        _uiState.update {
            it.copy(isProcessing = true)
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

        performPaymentConfirmationWithCvc(
            selectedPaymentDetails = selectedPaymentDetails,
            cvc = cvcController.formFieldValue.value.takeIf { it.isComplete }?.value
        )
    }

    private suspend fun performPaymentConfirmationWithCvc(
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
        cvc: String?
    ) {
        viewModelScope.launch {
            confirmationHandler.start(
                arguments = ConfirmationHandler.Args(
                    intent = configuration.stripeIntent,
                    confirmationOption = PaymentMethodConfirmationOption.New(
                        createParams = createPaymentMethodCreateParams(
                            selectedPaymentDetails = selectedPaymentDetails,
                            linkAccount = linkAccount
                        ),
                        optionsParams = null,
                        shouldSave = false
                    ),
                    appearance = PaymentSheet.Appearance(),
                    initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                        clientSecret = configuration.stripeIntent.clientSecret ?: ""
                    ),
                    shippingDetails = null
                )
            )
            val result = confirmationHandler.awaitResult()
            when (result) {
                is ConfirmationHandler.Result.Succeeded -> {
                    dismissWithResult(LinkActivityResult.Completed)
                }
                is ConfirmationHandler.Result.Canceled -> {
                    dismissWithResult(LinkActivityResult.Canceled(LinkActivityResult.Canceled.Reason.BackPressed))
                }
                is ConfirmationHandler.Result.Failed -> {
                    _uiState.update {
                        it.copy(
                            errorMessage = result.message
                        )
                    }
                }
                null -> Unit
            }
        }
    }

    private fun createPaymentMethodCreateParams(
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount,
    ): PaymentMethodCreateParams {
        return PaymentMethodCreateParams.createLink(
            paymentDetailsId = selectedPaymentDetails.id,
            consumerSessionClientSecret = linkAccount.clientSecret,
            extraParams = emptyMap(),
        )
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
        dismissWithResult(LinkActivityResult.Canceled(LinkActivityResult.Canceled.Reason.PayAnotherWay))
    }

    fun onAddNewPaymentMethodClicked() {
        navigate(LinkScreen.PaymentMethod)
    }

    @SuppressWarnings("UnusedParameter")
    fun onEditPaymentMethodClicked(item: ConsumerPaymentDetails.PaymentDetails) {
        navigate(LinkScreen.CardEdit)
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
                        confirmationHandler = parentComponent.viewModel.confirmationHandler,
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
        code = PaymentMethod.Type.Card.code,
        requiresMandate = false
    )
}
