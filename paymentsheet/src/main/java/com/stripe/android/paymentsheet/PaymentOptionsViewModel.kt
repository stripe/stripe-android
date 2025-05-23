package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.analytics.SessionSavedStateHandler
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.updateLinkAccount
import com.stripe.android.link.domain.LinkProminenceFeatureProvider
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.DaggerPaymentOptionsViewModelFactoryComponent
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection.Link
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddFirstPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.WalletsProcessingState
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.DefaultAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.DefaultSelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.verticalmode.VerticalModeInitialScreenFactory
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.paymentsheet.viewmodels.PrimaryButtonUiStateMapper
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@JvmSuppressWildcards
internal class PaymentOptionsViewModel @Inject constructor(
    private val args: PaymentOptionContract.Args,
    private val linkProminenceFeatureProvider: LinkProminenceFeatureProvider,
    private val linkAccountHolder: LinkAccountHolder,
    val linkPaymentLauncher: LinkPaymentLauncher,
    eventReporter: EventReporter,
    customerRepository: CustomerRepository,
    @IOContext workContext: CoroutineContext,
    savedStateHandle: SavedStateHandle,
    linkHandler: LinkHandler,
    cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
) : BaseSheetViewModel(
    config = args.configuration,
    eventReporter = eventReporter,
    customerRepository = customerRepository,
    workContext = workContext,
    savedStateHandle = savedStateHandle,
    linkHandler = linkHandler,
    cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
    isCompleteFlow = false,
) {

    private val primaryButtonUiStateMapper = PrimaryButtonUiStateMapper(
        config = config,
        isProcessingPayment = args.state.stripeIntent is PaymentIntent,
        currentScreenFlow = navigationHandler.currentScreen,
        buttonsEnabledFlow = buttonsEnabled,
        amountFlow = stateFlowOf(args.state.paymentMethodMetadata.amount()),
        selectionFlow = selection,
        customPrimaryButtonUiStateFlow = customPrimaryButtonUiState,
        cvcCompleteFlow = cvcRecollectionCompleteFlow,
        onClick = {
            eventReporter.onPressConfirmButton(selection.value)
            onUserSelection()
        },
    )

    private val _paymentOptionResult = MutableSharedFlow<PaymentOptionResult>(replay = 1)
    internal val paymentOptionResult: SharedFlow<PaymentOptionResult> = _paymentOptionResult

    private val _error = MutableStateFlow<ResolvableString?>(null)
    override val error: StateFlow<ResolvableString?> = _error

    override val walletsProcessingState: StateFlow<WalletsProcessingState?> = MutableStateFlow(null).asStateFlow()

    override val walletsState: StateFlow<WalletsState?> = combineAsStateFlow(
        linkHandler.isLinkEnabled,
        linkHandler.linkConfigurationCoordinator.emailFlow,
        buttonsEnabled,
    ) { isLinkAvailable, linkEmail, buttonsEnabled ->
        val paymentMethodMetadata = args.state.paymentMethodMetadata
        WalletsState.create(
            isLinkAvailable = isLinkAvailable,
            linkEmail = linkEmail,
            isGooglePayReady = paymentMethodMetadata.isGooglePayReady,
            buttonsEnabled = buttonsEnabled,
            paymentMethodTypes = paymentMethodMetadata.supportedPaymentMethodTypes(),
            googlePayLauncherConfig = null,
            googlePayButtonType = GooglePayButtonType.Pay,
            onGooglePayPressed = {
                updateSelection(PaymentSelection.GooglePay)
                onUserSelection()
            },
            onLinkPressed = {
                updateSelection(PaymentSelection.Link())
                onUserSelection()
            },
            isSetupIntent = paymentMethodMetadata.stripeIntent is SetupIntent
        )
    }

    // Only used to determine if we should skip the list and go to the add card view and how to populate that view.
    override var newPaymentSelection: NewPaymentOptionSelection? =
        when (val selection = args.state.paymentSelection) {
            is PaymentSelection.New -> NewPaymentOptionSelection.New(selection)
            is PaymentSelection.CustomPaymentMethod -> NewPaymentOptionSelection.Custom(selection)
            is PaymentSelection.ExternalPaymentMethod -> NewPaymentOptionSelection.External(selection)
            else -> null
        }

    override val primaryButtonUiState = primaryButtonUiStateMapper.forCustomFlow()

    init {
        SessionSavedStateHandler.attachTo(this, savedStateHandle)

        linkAccountHolder.set(args.linkAccountInfo)
        linkHandler.setupLink(args.state.paymentMethodMetadata.linkState)
        // After recovering from don't keep activities the paymentMethodMetadata will be saved,
        // calling setPaymentMethodMetadata would require the repository be initialized, which
        // would not be the case.
        if (paymentMethodMetadata.value == null) {
            setPaymentMethodMetadata(args.state.paymentMethodMetadata)
        }
        customerStateHolder.setCustomerState(args.state.customer)

        updateSelection(args.state.paymentSelection)

        navigationHandler.resetTo(
            determineInitialBackStack(
                paymentMethodMetadata = args.state.paymentMethodMetadata,
                customerStateHolder = customerStateHolder,
            )
        )
    }

    fun onLinkAuthenticationResult(result: LinkActivityResult) {
        result.linkAccountUpdate?.updateLinkAccount(linkAccountHolder)
        when (result) {
            // Link verification dialog dismissed -> user canceled
            is LinkActivityResult.Canceled -> {
                Unit
            }
            // Link verification dialog failed -> show error
            is LinkActivityResult.Failed -> {
                onError(result.error.stripeErrorMessage())
            }
            // Link verification dialog completed -> close payment method selection with authenticated state
            is LinkActivityResult.Completed -> {
                _paymentOptionResult.tryEmit(
                    PaymentOptionResult.Succeeded(
                        linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
                        paymentSelection = Link(
                            selectedPayment = result.selectedPayment,
                            shippingAddress = result.shippingAddress,
                        ),
                        paymentMethods = customerStateHolder.paymentMethods.value
                    )
                )
            }
            // This should not happen, but if it does, we should show an error
            is LinkActivityResult.PaymentMethodObtained -> {
                val error = IllegalStateException(
                    "PaymentMethodObtained is not expected from authentication only Link flows"
                )
                onError(error.stripeErrorMessage())
            }
        }
    }

    override fun onUserCancel() {
        eventReporter.onDismiss()
        _paymentOptionResult.tryEmit(
            PaymentOptionResult.Canceled(
                linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
                mostRecentError = null,
                paymentSelection = determinePaymentSelectionUponCancel(),
                paymentMethods = customerStateHolder.paymentMethods.value,
            )
        )
    }

    private fun determinePaymentSelectionUponCancel(): PaymentSelection? {
        val initialSelection = args.state.paymentSelection?.withLinkDetails()

        return if (initialSelection is PaymentSelection.Saved) {
            initialSelection.takeIfStillValid()
        } else {
            initialSelection
        }
    }

    private fun PaymentSelection.Saved.takeIfStillValid(): PaymentSelection.Saved? {
        val paymentMethods = customerStateHolder.paymentMethods.value
        val paymentMethod = paymentMethods.firstOrNull { it.id == paymentMethod.id }
        return paymentMethod?.let {
            this.copy(paymentMethod = it)
        }
    }

    override fun onError(error: ResolvableString?) {
        _error.value = error
    }

    fun onUserSelection() {
        clearErrorMessages()

        selection.value?.let { paymentSelection ->
            eventReporter.onSelectPaymentOption(paymentSelection)
            val linkState = args.state.paymentMethodMetadata.linkState
            val shouldShowLinkConfiguration = linkState != null && shouldShowLinkVerification(
                paymentSelection = paymentSelection,
                linkConfiguration = linkState.configuration
            )
            if (shouldShowLinkConfiguration) {
                linkPaymentLauncher.present(
                    configuration = linkState.configuration,
                    launchMode = LinkLaunchMode.PaymentMethodSelection(selectedPayment = null),
                    linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
                    useLinkExpress = true
                )
            } else {
                _paymentOptionResult.tryEmit(
                    PaymentOptionResult.Succeeded(
                        linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
                        paymentSelection = paymentSelection.withLinkDetails(),
                        paymentMethods = customerStateHolder.paymentMethods.value
                    )
                )
            }
        }
    }

    /**
     * - Updates the [PaymentSelection], if Link, to include the current [LinkAccount] if it exists.
     * - Preserves the previously selected payment method, if any, in case none is selected in this launch.
     */
    private fun PaymentSelection.withLinkDetails(): PaymentSelection = when (this) {
        is Link -> when (linkAccountHolder.linkAccountInfo.value.account) {
            // If link account is null, clear account status and selected payment from payment selection
            null -> copy(
                selectedPayment = null
            )
            // If link account exists, include it in the payment selection and keep the previously selected payment.
            else -> copy(
                selectedPayment = (selectedPayment ?: (args.state.paymentSelection as? Link)?.selectedPayment)
            )
        }
        else -> this
    }

    private fun shouldShowLinkVerification(
        paymentSelection: PaymentSelection,
        linkConfiguration: LinkConfiguration
    ): Boolean {
        return paymentSelection is Link &&
            linkProminenceFeatureProvider.shouldShowEarlyVerificationInFlowController(linkConfiguration)
    }

    override fun handlePaymentMethodSelected(selection: PaymentSelection?) {
        updateSelection(selection)

        if (selection?.requiresConfirmation != true) {
            onUserSelection()
        }
    }

    override fun clearErrorMessages() {
        _error.value = null
    }

    private fun determineInitialBackStack(
        paymentMethodMetadata: PaymentMethodMetadata,
        customerStateHolder: CustomerStateHolder,
    ): List<PaymentSheetScreen> {
        if (config.paymentMethodLayout != PaymentSheet.PaymentMethodLayout.Horizontal) {
            return VerticalModeInitialScreenFactory.create(
                viewModel = this,
                paymentMethodMetadata = paymentMethodMetadata,
                customerStateHolder = customerStateHolder,
            )
        }
        val target = if (args.state.showSavedPaymentMethods) {
            val interactor = DefaultSelectSavedPaymentMethodsInteractor.create(
                viewModel = this,
                paymentMethodMetadata = paymentMethodMetadata,
                customerStateHolder = customerStateHolder,
                savedPaymentMethodMutator = savedPaymentMethodMutator,
            )
            SelectSavedPaymentMethods(interactor = interactor)
        } else {
            val interactor = DefaultAddPaymentMethodInteractor.create(
                viewModel = this,
                paymentMethodMetadata = paymentMethodMetadata,
            )
            AddFirstPaymentMethod(interactor = interactor)
        }

        return buildList {
            add(target)

            if (target is SelectSavedPaymentMethods && newPaymentSelection != null) {
                // The user has previously selected a new payment method. Instead of sending them
                // to the payment methods screen, we directly launch them into the payment method
                // form again.
                val interactor = DefaultAddPaymentMethodInteractor.create(
                    viewModel = this@PaymentOptionsViewModel,
                    paymentMethodMetadata = paymentMethodMetadata,
                )
                add(
                    PaymentSheetScreen.AddAnotherPaymentMethod(interactor = interactor)
                )
            }
        }
    }

    internal class Factory(
        private val starterArgsSupplier: () -> PaymentOptionContract.Args,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()
            val starterArgs = starterArgsSupplier()

            val component = DaggerPaymentOptionsViewModelFactoryComponent.builder()
                .application(application)
                .context(application)
                .productUsage(starterArgs.productUsage)
                .savedStateHandle(savedStateHandle)
                .paymentElementCallbackIdentifier(starterArgs.paymentElementCallbackIdentifier)
                .build()
                .paymentOptionsViewModelSubcomponentBuilder
                .application(application)
                .args(starterArgs)
                .build()

            return component.viewModel as T
        }
    }
}
