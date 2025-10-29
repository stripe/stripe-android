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
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.FinancialConnectionsSheetResult
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.LinkScreen.UpdateCard.BillingDetailsUpdateFlow
import com.stripe.android.link.NoPaymentMethodOptionsAvailable
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
import com.stripe.android.model.ClientAttributionMetadata
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
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

internal class WalletViewModel(
    private val configuration: LinkConfiguration,
    private val linkAccount: LinkAccount,
    private val linkAccountManager: LinkAccountManager,
    private val completeLinkFlow: CompleteLinkFlow,
    private val addPaymentMethodOptions: AddPaymentMethodOptions,
    private val logger: Logger,
    private val navigationManager: NavigationManager,
    private val linkLaunchMode: LinkLaunchMode,
    private val dismissalCoordinator: LinkDismissalCoordinator,
    private val navigateAndClearStack: (route: LinkScreen) -> Unit,
    private val dismissWithResult: (LinkActivityResult) -> Unit
) : ViewModel() {
    private val stripeIntent = configuration.stripeIntent

    private val _uiState = MutableStateFlow(
        value = WalletUiState(
            paymentDetailsList = emptyList(),
            email = linkAccount.email,
            allowLogOut = configuration.allowLogOut,
            isSettingUp = stripeIntent.isSetupForFutureUsage(configuration.passthroughModeEnabled),
            merchantName = configuration.merchantName,
            sellerBusinessName = configuration.sellerBusinessName,
            selectedItemId = null,
            cardBrandFilter = configuration.cardBrandFilter,
            collectMissingBillingDetailsForExistingPaymentMethods = configuration
                .collectMissingBillingDetailsForExistingPaymentMethods,
            isProcessing = false,
            hasCompleted = false,
            // initially expand the wallet if a payment method is preselected.
            userSetIsExpanded = linkLaunchMode.selectedItemId != null,
            primaryButtonLabel = completePaymentButtonLabel(configuration.stripeIntent, linkLaunchMode),
            secondaryButtonLabel = configuration.stripeIntent.secondaryButtonLabel(linkLaunchMode),
            addPaymentMethodOptions = addPaymentMethodOptions.values,
            paymentSelectionHint = paymentSelectionHint,
            isAutoSelecting = shouldAutoSelectDefaultPaymentMethod(),
            signupToggleEnabled = configuration.linkSignUpOptInFeatureEnabled,
            billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration,
        )
    )

    private val LinkLaunchMode.selectedItemId
        get() = when (this) {
            is LinkLaunchMode.Full,
            is LinkLaunchMode.Confirmation -> null
            is LinkLaunchMode.PaymentMethodSelection -> selectedPayment?.id
            is LinkLaunchMode.Authentication -> null
            is LinkLaunchMode.Authorization -> null
        }

    private val paymentMethodFilter
        get() = (linkLaunchMode as? LinkLaunchMode.PaymentMethodSelection)?.paymentMethodFilter

    private val paymentSelectionHint: ResolvableString?
        get() = R.string.stripe_wallet_prefer_debit_card_hint
            .takeIf {
                val isFeatureEnabled =
                    configuration.enableLinkPaymentSelectionHint ||
                        FeatureFlags.forceEnableLinkPaymentSelectionHint.isEnabled
                val canSelectCard = addPaymentMethodOptions.values.contains(AddPaymentMethodOption.Card)
                isFeatureEnabled && canSelectCard
            }
            ?.resolvableString

    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    val allowLogOut: Boolean = configuration.allowLogOut

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
                val filteredPaymentDetails = paymentDetailsState.paymentDetails
                    .filter { paymentMethodFilter?.invoke(it.details) != false }
                    .toList()
                val currentState = _uiState.updateAndGet {
                    it.updateWithResponse(filteredPaymentDetails)
                }
                if (filteredPaymentDetails.isEmpty()) {
                    when (addPaymentMethodOptions.default) {
                        AddPaymentMethodOption.Card -> {
                            navigateAndClearStack(LinkScreen.PaymentMethod)
                        }
                        is AddPaymentMethodOption.Bank -> {
                            presentAddBankAccount()
                        }
                        null -> {
                            dismissWithResult(
                                LinkActivityResult.Failed(
                                    error = NoPaymentMethodOptionsAvailable(),
                                    linkAccountUpdate = linkAccountManager.linkAccountUpdate
                                )
                            )
                        }
                    }
                } else {
                    // Auto-select default payment method only on first load
                    if (shouldAutoSelectDefaultPaymentMethod() && !currentState.hasAttemptedAutoSelection) {
                        handleAutoSelection(filteredPaymentDetails)
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

    private fun shouldAutoSelectDefaultPaymentMethod(): Boolean {
        return linkLaunchMode is LinkLaunchMode.PaymentMethodSelection &&
            linkLaunchMode.selectedPayment == null &&
            configuration.skipWalletInFlowController
    }

    private suspend fun handleAutoSelection(paymentDetails: List<LinkPaymentMethod.ConsumerPaymentDetails>) {
        val autoSelectedPaymentMethod =
            (paymentDetails.firstOrNull { it.details.isDefault } ?: paymentDetails.singleOrNull())?.details

        _uiState.update { it.copy(hasAttemptedAutoSelection = true) }

        if (autoSelectedPaymentMethod?.isReadyForUse() == true) {
            // Set the default as selected and proceed with payment selection
            _uiState.update {
                it.copy(selectedItemId = autoSelectedPaymentMethod.id)
            }
            performPaymentConfirmation(autoSelectedPaymentMethod)
        } else {
            // Auto-selection not supported, show the wallet UI
            _uiState.update {
                it.copy(isAutoSelecting = false)
            }
        }
    }

    private fun ConsumerPaymentDetails.PaymentDetails.isReadyForUse(): Boolean {
        // Check if card requires details recollection (includes both expiry and CVC checks)
        val requiresCardDetailsRecollection = (this as? ConsumerPaymentDetails.Card)
            ?.requiresCardDetailsRecollection == true

        // Check if billing details collection is needed
        val needsBillingDetails = supports(
            billingDetailsConfig = configuration.billingDetailsCollectionConfiguration,
            linkAccount = linkAccount
        ).not() && _uiState.value.collectMissingBillingDetailsForExistingPaymentMethods

        return !requiresCardDetailsRecollection && !needsBillingDetails
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

    fun handleDisabledButtonClick() {
        _uiState.update {
            it.copy(isValidating = true)
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
        val paymentMethodCreateParams = uiState.value.toPaymentMethodCreateParams(
            configuration.clientAttributionMetadata
        )
        dismissalCoordinator.withDismissalDisabled {
            val updateParams = ConsumerPaymentDetailsUpdateParams(
                id = paymentDetail.id,
                isDefault = paymentDetail.isDefault,
                cardPaymentMethodCreateParamsMap = paymentMethodCreateParams.toParamMap(),
                clientAttributionMetadataParams = configuration.clientAttributionMetadata.toParamMap(),
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
            billingDetailsConfig = configuration.billingDetailsCollectionConfiguration,
            linkAccount = linkAccount
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
                cardPaymentMethodCreateParamsMap = null,
                clientAttributionMetadataParams = configuration.clientAttributionMetadata.toParamMap(),
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
                presentAddBankAccount()
            }
            AddPaymentMethodOption.Card -> {
                navigationManager.tryNavigateTo(LinkScreen.PaymentMethod.route)
            }
        }
    }

    private fun presentAddBankAccount() {
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
                    if (uiState.value.paymentDetailsList.isEmpty()) {
                        dismissWithResult(
                            LinkActivityResult.Canceled(
                                linkAccountUpdate = linkAccountManager.linkAccountUpdate
                            )
                        )
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
                        addPaymentMethodOptions = parentComponent.addPaymentMethodOptionsFactory.create(linkAccount),
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

private fun WalletUiState.toPaymentMethodCreateParams(
    clientAttributionMetadata: ClientAttributionMetadata,
): PaymentMethodCreateParams {
    val expiryDateValues = createExpiryDateFormFieldValues(expiryDateInput)
    return FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
        fieldValuePairs = expiryDateValues,
        code = Card.code,
        requiresMandate = false,
        clientAttributionMetadata = clientAttributionMetadata,
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

private fun StripeIntent.secondaryButtonLabel(linkLaunchMode: LinkLaunchMode): ResolvableString? {
    return when (linkLaunchMode) {
        is LinkLaunchMode.Full,
        is LinkLaunchMode.Confirmation -> when (this) {
            is PaymentIntent -> R.string.stripe_wallet_pay_another_way.resolvableString
            is SetupIntent -> R.string.stripe_wallet_continue_another_way.resolvableString
        }
        is LinkLaunchMode.PaymentMethodSelection -> {
            if (linkLaunchMode.shouldShowSecondaryCta) {
                R.string.stripe_wallet_continue_another_way.resolvableString
            } else {
                null
            }
        }
        is LinkLaunchMode.Authentication,
        is LinkLaunchMode.Authorization ->
            R.string.stripe_wallet_continue_another_way.resolvableString
    }
}
