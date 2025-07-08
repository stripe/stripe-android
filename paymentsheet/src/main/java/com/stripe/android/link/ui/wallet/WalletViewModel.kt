package com.stripe.android.link.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.LinkScreen.UpdateCard.BillingDetailsUpdateFlow
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.linkAccountUpdate
import com.stripe.android.link.confirmation.CompleteLinkFlow
import com.stripe.android.link.confirmation.DefaultCompleteLinkFlow
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.supportedPaymentMethodTypes
import com.stripe.android.link.ui.completePaymentButtonLabel
import com.stripe.android.link.utils.supports
import com.stripe.android.link.withDismissalDisabled
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerPaymentDetailsUpdateParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethod.Type.Card
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.R
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.ui.core.elements.CardDetailsUtil.createExpiryDateFormFieldValues
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.navigation.NavigationManager
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class WalletViewModel @Inject constructor(
    private val configuration: LinkConfiguration,
    private val linkAccount: LinkAccount,
    private val linkAccountManager: LinkAccountManager,
    private val completeLinkFlow: CompleteLinkFlow,
    private val logger: Logger,
    private val navigationManager: NavigationManager,
    private val linkLaunchMode: LinkLaunchMode,
    private val dismissalCoordinator: LinkDismissalCoordinator,
    private val navigateAndClearStack: (route: LinkScreen) -> Unit,
    private val dismissWithResult: (LinkActivityResult) -> Unit
) : ViewModel() {
    private val stripeIntent = configuration.stripeIntent

    private val supportedPaymentMethodTypes = stripeIntent.supportedPaymentMethodTypes(linkAccount)

    private val _uiState = MutableStateFlow(
        value = WalletUiState(
            paymentDetailsList = emptyList(),
            email = linkAccount.email,
            isSettingUp = stripeIntent.isSetupForFutureUsage(configuration.passthroughModeEnabled),
            merchantName = configuration.merchantName,
            selectedItemId = null,
            cardBrandFilter = configuration.cardBrandFilter,
            collectMissingBillingDetailsForExistingPaymentMethods =
            configuration.collectMissingBillingDetailsForExistingPaymentMethods,
            isProcessing = false,
            hasCompleted = false,
            // initially expand the wallet if a payment method is preselected.
            userSetIsExpanded = linkLaunchMode.selectedItemId != null,
            primaryButtonLabel = completePaymentButtonLabel(configuration.stripeIntent, linkLaunchMode),
            secondaryButtonLabel = configuration.stripeIntent.secondaryButtonLabel(linkLaunchMode),
            addPaymentMethodOptions = getAddPaymentMethodOptions(),
        )
    )

    val LinkLaunchMode.selectedItemId
        get() = when (this) {
            is LinkLaunchMode.Full,
            is LinkLaunchMode.Confirmation -> null
            is LinkLaunchMode.PaymentMethodSelection -> selectedPayment?.id
        }

    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

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
            it.copy(isProcessing = true)
        }

        viewModelScope.launch {
            loadPaymentDetails(selectedItemId = linkLaunchMode.selectedItemId)
        }

        viewModelScope.launch {
            linkAccountManager.consumerState.filterNotNull().collectLatest { paymentDetailsState ->
                if (paymentDetailsState.paymentDetails.isEmpty()) {
                    navigateAndClearStack(LinkScreen.PaymentMethod)
                } else {
                    _uiState.update {
                        it.updateWithResponse(paymentDetailsState.paymentDetails)
                    }
                }
            }
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

    private suspend fun loadPaymentDetails(
        selectedItemId: String?,
        isAfterAdding: Boolean = false
    ) {
        linkAccountManager.listPaymentDetails(
            paymentMethodTypes = stripeIntent.supportedPaymentMethodTypes(linkAccount)
        ).fold(
            onSuccess = { response ->
                _uiState.update {
                    it.copy(
                        selectedItemId = selectedItemId,
                        userSetIsExpanded = if (isAfterAdding) false else it.userSetIsExpanded,
                        errorMessage = if (isAfterAdding) null else it.errorMessage,
                        addBankAccountState = if (isAfterAdding) AddBankAccountState.Idle else it.addBankAccountState,
                    )
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
                selectedItemId = item.id,
                userSetIsExpanded = null,
            )
        }
    }

    fun onExpandedChanged(expanded: Boolean) {
        _uiState.update {
            it.copy(userSetIsExpanded = expanded)
        }
    }

    fun onPrimaryButtonClicked() {
        val paymentDetail = _uiState.value.selectedItem ?: return

        setProcessingState(true)

        val card = paymentDetail as? ConsumerPaymentDetails.Card
        val isExpired = card?.isExpired == true

        viewModelScope.launch {
            when {
                isExpired -> handleExpiredCard(paymentDetail)
                else -> performPaymentConfirmation(paymentDetail)
            }
        }
    }

    private fun setProcessingState(isProcessing: Boolean, errorMessage: ResolvableString? = null) {
        _uiState.update {
            it.copy(
                isProcessing = isProcessing,
                errorMessage = errorMessage,
            )
        }
    }

    private suspend fun handleExpiredCard(paymentDetail: ConsumerPaymentDetails.PaymentDetails) {
        val paymentMethodCreateParams = uiState.value.toPaymentMethodCreateParams()
        dismissalCoordinator.withDismissalDisabled {
            val updateParams = ConsumerPaymentDetailsUpdateParams(
                id = paymentDetail.id,
                isDefault = paymentDetail.isDefault,
                cardPaymentMethodCreateParamsMap = paymentMethodCreateParams.toParamMap()
            )
            linkAccountManager.updatePaymentDetails(updateParams)
        }.fold(
            onSuccess = { result ->
                val updatedPaymentDetails = result.paymentDetails.single { it.id == paymentDetail.id }
                performPaymentConfirmation(updatedPaymentDetails)
            },
            onFailure = { error -> handleUpdateError(error) }
        )
    }

    private fun handleUpdateError(error: Throwable) {
        _uiState.update {
            it.copy(
                alertMessage = error.stripeErrorMessage(),
                isProcessing = false
            )
        }
    }

    private suspend fun performPaymentConfirmation(
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
    ) {
        // Check if billing details are missing before proceeding with payment
        val shouldCollectMissingBillingDetails = selectedPaymentDetails.supports(
            configuration.billingDetailsCollectionConfiguration, linkAccount
        ).not() && _uiState.value.collectMissingBillingDetailsForExistingPaymentMethods

        if (shouldCollectMissingBillingDetails) {
            setProcessingState(false)
            val cvc = cvcController.formFieldValue.value.takeIf { it.isComplete }?.value
            val billingDetailsUpdateFlow = BillingDetailsUpdateFlow(cvc = cvc)

            navigationManager.tryNavigateTo(
                route = LinkScreen.UpdateCard(
                    paymentDetailsId = selectedPaymentDetails.id,
                    billingDetailsUpdateFlow = billingDetailsUpdateFlow
                ),
            )
            return
        }

        val cvc = cvcController.formFieldValue.value.takeIf { it.isComplete }?.value

        // Use the cached phone for this payment detail if available (ie the user updated it locally)
        val linkPaymentMethod = linkAccountManager.consumerState.value
            ?.paymentDetails?.find { it.details.id == selectedPaymentDetails.id }
        val result = completeLinkFlow(
            selectedPaymentDetails = LinkPaymentMethod.ConsumerPaymentDetails(
                details = selectedPaymentDetails,
                collectedCvc = cvc,
                billingPhone = linkPaymentMethod?.billingPhone ?: linkAccount.unredactedPhoneNumber
            ),
            linkAccount = linkAccount
        )

        when (result) {
            is CompleteLinkFlow.Result.Canceled -> {
                _uiState.update { it.copy(isProcessing = false) }
            }
            is CompleteLinkFlow.Result.Failed -> {
                _uiState.update {
                    it.copy(
                        errorMessage = result.error,
                        isProcessing = false
                    )
                }
            }
            is CompleteLinkFlow.Result.Completed -> {
                dismissWithResult(result.linkActivityResult)
            }
        }
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
            it.copy(cardBeingUpdated = item.id)
        }
        viewModelScope.launch {
            linkAccountManager.deletePaymentDetails(item.id).fold(
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

            _uiState.update {
                it.copy(cardBeingUpdated = null)
            }
        }
    }

    fun onUpdateClicked(item: ConsumerPaymentDetails.PaymentDetails) {
        navigationManager.tryNavigateTo(
            route = LinkScreen.UpdateCard(
                paymentDetailsId = item.id,
                billingDetailsUpdateFlow = null
            ),
        )
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

    fun onAddPaymentMethodOptionClicked(option: AddPaymentMethodOption) {
        when (option) {
            is AddPaymentMethodOption.Bank -> {
                onAddBankAccountClicked()
            }
            AddPaymentMethodOption.Card -> {
                navigationManager.tryNavigateTo(LinkScreen.PaymentMethod.route)
            }
        }
    }

    private fun onAddBankAccountClicked() {
        _uiState.update {
            it.copy(addBankAccountState = AddBankAccountState.Processing())
        }
        viewModelScope.launch {
            linkAccountManager.createLinkAccountSession()
                .mapCatching { session ->
                    FinancialConnectionsSheetConfiguration(
                        financialConnectionsSessionClientSecret = session.clientSecret,
                        publishableKey = linkAccount.consumerPublishableKey!!,
                    )
                }
                .fold(
                    onSuccess = { config ->
                        _uiState.update {
                            it.copy(addBankAccountState = AddBankAccountState.Processing(configToPresent = config))
                        }
                    },
                    onFailure = { error ->
                        onAddBankAccountError(
                            error = error,
                            loggerMessage = "Failed to create Link account session"
                        )
                    }
                )
        }
    }

    fun onPresentFinancialConnections(success: Boolean) {
        if (success) {
            _uiState.update {
                it.copy(addBankAccountState = AddBankAccountState.Processing(configToPresent = null))
            }
        } else {
            // This shouldn't happen, but we'll handle it just in case so the UI isn't stuck processing.
            logger.warning("WalletViewModel: Failed to present Financial Connections")
            _uiState.update {
                it.copy(addBankAccountState = AddBankAccountState.Idle)
            }
        }
    }

    fun onFinancialConnectionsResult(result: FinancialConnectionsSheetResult) {
        viewModelScope.launch {
            when (result) {
                is FinancialConnectionsSheetResult.Completed -> {
                    val accountId = result.financialConnectionsSession.accounts.data.firstOrNull()?.id
                    if (accountId != null) {
                        linkAccountManager.createBankAccountPaymentDetails(accountId)
                            .mapCatching { paymentDetails ->
                                loadPaymentDetails(
                                    selectedItemId = paymentDetails.id,
                                    isAfterAdding = true
                                )
                            }
                            .onFailure {
                                onAddBankAccountError(
                                    error = it,
                                    loggerMessage = "Failed to create/load bank account"
                                )
                            }
                    } else {
                        _uiState.update {
                            it.copy(addBankAccountState = AddBankAccountState.Idle)
                        }
                    }
                }
                FinancialConnectionsSheetResult.Canceled -> {
                    _uiState.update {
                        it.copy(addBankAccountState = AddBankAccountState.Idle)
                    }
                }
                is FinancialConnectionsSheetResult.Failed -> {
                    onAddBankAccountError(
                        error = result.error,
                        loggerMessage = "Failed to get Financial Connections result"
                    )
                }
            }
        }
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

    private fun onAddBankAccountError(
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
                addBankAccountState = AddBankAccountState.Idle
            )
        }
    }

    private fun getAddPaymentMethodOptions(): List<AddPaymentMethodOption> {
        return buildList {
            if (
                linkAccount.consumerPublishableKey != null &&
                configuration.financialConnectionsAvailability != null &&
                supportedPaymentMethodTypes.contains(ConsumerPaymentDetails.BankAccount.TYPE)
            ) {
                add(AddPaymentMethodOption.Bank(configuration.financialConnectionsAvailability))
            }
            if (supportedPaymentMethodTypes.contains(ConsumerPaymentDetails.Card.TYPE)) {
                add(AddPaymentMethodOption.Card)
            }
        }
    }

    companion object {
        fun factory(
            parentComponent: NativeLinkComponent,
            linkAccount: LinkAccount,
            navigateAndClearStack: (route: LinkScreen) -> Unit,
            dismissWithResult: (LinkActivityResult) -> Unit
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    val confirmationHandler = parentComponent.linkConfirmationHandlerFactory.create(
                        confirmationHandler = parentComponent.viewModel.confirmationHandler
                    )
                    WalletViewModel(
                        configuration = parentComponent.configuration,
                        linkAccountManager = parentComponent.linkAccountManager,
                        completeLinkFlow = DefaultCompleteLinkFlow(
                            linkConfirmationHandler = confirmationHandler,
                            linkAccountManager = parentComponent.linkAccountManager,
                            dismissalCoordinator = parentComponent.dismissalCoordinator,
                            linkLaunchMode = parentComponent.linkLaunchMode
                        ),
                        logger = parentComponent.logger,
                        navigationManager = parentComponent.navigationManager,
                        dismissalCoordinator = parentComponent.dismissalCoordinator,
                        linkAccount = linkAccount,
                        navigateAndClearStack = navigateAndClearStack,
                        linkLaunchMode = parentComponent.linkLaunchMode,
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

internal fun StripeIntent.isSetupForFutureUsage(passthroughModeEnabled: Boolean): Boolean {
    return when (this) {
        is PaymentIntent -> {
            if (passthroughModeEnabled) {
                isSetupFutureUsageSet(PaymentMethod.Type.Card.code)
            } else {
                isSetupFutureUsageSet(PaymentMethod.Type.Link.code)
            }
        }
        is SetupIntent -> true
    }
}

private fun StripeIntent.secondaryButtonLabel(linkLaunchMode: LinkLaunchMode): ResolvableString {
    return when (linkLaunchMode) {
        is LinkLaunchMode.Full,
        is LinkLaunchMode.Confirmation -> when (this) {
            is PaymentIntent -> resolvableString(R.string.stripe_wallet_pay_another_way)
            is SetupIntent -> resolvableString(R.string.stripe_wallet_continue_another_way)
        }
        is LinkLaunchMode.PaymentMethodSelection -> resolvableString(R.string.stripe_wallet_continue_another_way)
    }
}
