package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.analytics.SessionSavedStateHandler
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.DaggerPaymentOptionsViewModelFactoryComponent
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddFirstPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.WalletsProcessingState
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.DefaultAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.DefaultSelectSavedPaymentMethodsInteractor
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.verticalmode.VerticalModeInitialScreenFactory
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.paymentsheet.viewmodels.PrimaryButtonUiStateMapper
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@JvmSuppressWildcards
internal class PaymentOptionsViewModel @Inject constructor(
    private val args: PaymentOptionContract.Args,
    eventReporter: EventReporter,
    customerRepository: CustomerRepository,
    @IOContext workContext: CoroutineContext,
    savedStateHandle: SavedStateHandle,
    linkHandler: LinkHandler,
    cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    editInteractorFactory: ModifiableEditPaymentMethodViewInteractor.Factory
) : BaseSheetViewModel(
    config = args.state.config,
    eventReporter = eventReporter,
    customerRepository = customerRepository,
    workContext = workContext,
    savedStateHandle = savedStateHandle,
    linkHandler = linkHandler,
    editInteractorFactory = editInteractorFactory,
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
                updateSelection(PaymentSelection.Link)
                onUserSelection()
            },
            isSetupIntent = paymentMethodMetadata.stripeIntent is SetupIntent,
            cardBrandFilter = PaymentSheetCardBrandFilter(config.cardBrandAcceptance)
        )
    }

    // Only used to determine if we should skip the list and go to the add card view and how to populate that view.
    override var newPaymentSelection: NewOrExternalPaymentSelection? =
        when (val selection = args.state.paymentSelection) {
            is PaymentSelection.New -> NewOrExternalPaymentSelection.New(selection)
            is PaymentSelection.ExternalPaymentMethod -> NewOrExternalPaymentSelection.External(selection)
            else -> null
        }

    override val primaryButtonUiState = primaryButtonUiStateMapper.forCustomFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null,
    )

    init {
        SessionSavedStateHandler.attachTo(this, savedStateHandle)

        viewModelScope.launch {
            linkHandler.processingState.collect { processingState ->
                handleLinkProcessingState(processingState)
            }
        }

        // This is bad, but I don't think there's a better option
        PaymentSheet.FlowController.linkHandler = linkHandler

        linkHandler.setupLink(args.state.linkState)

        // After recovering from don't keep activities the paymentMethodMetadata will be saved,
        // calling setPaymentMethodMetadata would require the repository be initialized, which
        // would not be the case.
        if (paymentMethodMetadata.value == null) {
            setPaymentMethodMetadata(args.state.paymentMethodMetadata)
        }
        customerStateHolder.setCustomerState(args.state.customer)
        savedStateHandle[SAVE_PROCESSING] = false

        updateSelection(args.state.paymentSelection)

        navigationHandler.resetTo(
            determineInitialBackStack(
                paymentMethodMetadata = args.state.paymentMethodMetadata,
                customerStateHolder = customerStateHolder,
            )
        )
    }

    private fun handleLinkProcessingState(processingState: LinkHandler.ProcessingState) {
        when (processingState) {
            LinkHandler.ProcessingState.Cancelled -> {
                onPaymentResult(PaymentResult.Canceled)
            }
            is LinkHandler.ProcessingState.PaymentMethodCollected -> {
                TODO("This can't happen. Will follow up to remodel the states better.")
            }
            is LinkHandler.ProcessingState.CompletedWithPaymentResult -> {
                onPaymentResult(processingState.result)
            }
            is LinkHandler.ProcessingState.Error -> {
                onError(processingState.message?.resolvableString)
            }
            LinkHandler.ProcessingState.Launched -> {
            }
            is LinkHandler.ProcessingState.PaymentDetailsCollected -> {
                processingState.paymentSelection?.let {
                    // Link PaymentDetails was created successfully, use it to confirm the Stripe Intent.
                    updateSelection(it)
                    onUserSelection()
                } ?: run {
                    // Creating Link PaymentDetails failed, fallback to regular checkout.
                    // paymentSelection is already set to the card parameters from the form.
                    onUserSelection()
                }
            }
            LinkHandler.ProcessingState.Ready -> {
                updatePrimaryButtonState(PrimaryButton.State.Ready)
            }
            LinkHandler.ProcessingState.Started -> {
                updatePrimaryButtonState(PrimaryButton.State.StartProcessing)
            }
            LinkHandler.ProcessingState.CompleteWithoutLink -> {
                onUserSelection()
            }
        }
    }

    override fun onUserCancel() {
        eventReporter.onDismiss()
        _paymentOptionResult.tryEmit(
            PaymentOptionResult.Canceled(
                mostRecentError = null,
                paymentSelection = determinePaymentSelectionUponCancel(),
                paymentMethods = customerStateHolder.paymentMethods.value,
            )
        )
    }

    private fun determinePaymentSelectionUponCancel(): PaymentSelection? {
        val initialSelection = args.state.paymentSelection

        return if (initialSelection is PaymentSelection.Saved) {
            initialSelection.takeIfStillValid()
        } else {
            initialSelection
        }
    }

    private fun PaymentSelection.Saved.takeIfStillValid(): PaymentSelection.Saved? {
        val paymentMethods = customerStateHolder.paymentMethods.value
        val isStillAround = paymentMethods.any { it.id == paymentMethod.id }
        return this.takeIf { isStillAround }
    }

    override fun onError(error: ResolvableString?) {
        _error.value = error
    }

    fun onUserSelection() {
        clearErrorMessages()

        selection.value?.let { paymentSelection ->
            // TODO(michelleb-stripe): Should the payment selection in the event be the saved or new item?
            eventReporter.onSelectPaymentOption(paymentSelection)

            when (paymentSelection) {
                is PaymentSelection.Saved,
                is PaymentSelection.GooglePay,
                is PaymentSelection.Link -> processExistingPaymentMethod(paymentSelection)
                is PaymentSelection.New -> processNewOrExternalPaymentMethod(paymentSelection)
                is PaymentSelection.ExternalPaymentMethod -> processNewOrExternalPaymentMethod(paymentSelection)
            }
        }
    }

    override fun onPaymentResult(paymentResult: PaymentResult) {
        savedStateHandle[SAVE_PROCESSING] = false
    }

    override fun handlePaymentMethodSelected(selection: PaymentSelection?) {
        updateSelection(selection)

        if (selection?.requiresConfirmation != true) {
            onUserSelection()
        }
    }

    override fun handleConfirmUSBankAccount(paymentSelection: PaymentSelection.New.USBankAccount) {
        updateSelection(paymentSelection)
        eventReporter.onPressConfirmButton(selection.value)
        onUserSelection()
    }

    override fun clearErrorMessages() {
        _error.value = null
    }

    private fun processExistingPaymentMethod(paymentSelection: PaymentSelection) {
        _paymentOptionResult.tryEmit(
            PaymentOptionResult.Succeeded(
                paymentSelection = paymentSelection,
                paymentMethods = customerStateHolder.paymentMethods.value
            )
        )
    }

    private fun processNewOrExternalPaymentMethod(paymentSelection: PaymentSelection) {
        _paymentOptionResult.tryEmit(
            PaymentOptionResult.Succeeded(
                paymentSelection = paymentSelection,
                paymentMethods = customerStateHolder.paymentMethods.value
            )
        )
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
                .context(application)
                .productUsage(starterArgs.productUsage)
                .build()
                .paymentOptionsViewModelSubcomponentBuilder
                .application(application)
                .args(starterArgs)
                .savedStateHandle(savedStateHandle)
                .build()

            return component.viewModel as T
        }
    }
}
