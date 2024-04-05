package com.stripe.android.paymentsheet

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.analytics.SessionSavedStateHandler
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.link.LinkConfigurationCoordinator
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
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.paymentsheet.state.WalletsProcessingState
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.HeaderTextFactory
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.paymentsheet.viewmodels.PrimaryButtonUiStateMapper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@JvmSuppressWildcards
internal class PaymentOptionsViewModel @Inject constructor(
    private val args: PaymentOptionContract.Args,
    prefsRepositoryFactory: (PaymentSheet.CustomerConfiguration?) -> PrefsRepository,
    eventReporter: EventReporter,
    customerRepository: CustomerRepository,
    @IOContext workContext: CoroutineContext,
    application: Application,
    logger: Logger,
    savedStateHandle: SavedStateHandle,
    linkHandler: LinkHandler,
    linkConfigurationCoordinator: LinkConfigurationCoordinator,
    editInteractorFactory: ModifiableEditPaymentMethodViewInteractor.Factory
) : BaseSheetViewModel(
    application = application,
    config = args.state.config,
    prefsRepository = prefsRepositoryFactory(args.state.config.customer),
    eventReporter = eventReporter,
    customerRepository = customerRepository,
    workContext = workContext,
    logger = logger,
    savedStateHandle = savedStateHandle,
    linkHandler = linkHandler,
    linkConfigurationCoordinator = linkConfigurationCoordinator,
    headerTextFactory = HeaderTextFactory(isCompleteFlow = false),
    editInteractorFactory = editInteractorFactory
) {

    private val primaryButtonUiStateMapper = PrimaryButtonUiStateMapper(
        context = getApplication(),
        config = config,
        isProcessingPayment = args.state.stripeIntent is PaymentIntent,
        currentScreenFlow = currentScreen,
        buttonsEnabledFlow = buttonsEnabled,
        amountFlow = paymentMethodMetadata.map { it?.amount() },
        selectionFlow = selection,
        customPrimaryButtonUiStateFlow = customPrimaryButtonUiState,
        onClick = {
            reportConfirmButtonPressed()
            onUserSelection()
        },
    )

    private val _paymentOptionResult = MutableSharedFlow<PaymentOptionResult>(replay = 1)
    internal val paymentOptionResult: SharedFlow<PaymentOptionResult> = _paymentOptionResult

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error

    override val walletsProcessingState: StateFlow<WalletsProcessingState?> = MutableStateFlow(null).asStateFlow()

    override val walletsState: StateFlow<WalletsState?> = combine(
        linkHandler.isLinkEnabled,
        linkEmailFlow,
        buttonsEnabled,
        supportedPaymentMethodsFlow,
        backStack,
    ) { isLinkAvailable, linkEmail, buttonsEnabled, paymentMethodTypes, stack ->
        WalletsState.create(
            isLinkAvailable = isLinkAvailable,
            linkEmail = linkEmail,
            googlePayState = GooglePayState.NotAvailable,
            buttonsEnabled = buttonsEnabled,
            paymentMethodTypes = paymentMethodTypes,
            googlePayLauncherConfig = null,
            googlePayButtonType = GooglePayButtonType.Pay,
            screen = stack.last(),
            isCompleteFlow = false,
            onGooglePayPressed = {
                error("Google Pay shouldn't be enabled in the custom flow.")
            },
            onLinkPressed = {
                updateSelection(PaymentSelection.Link)
                onUserSelection()
            },
            isSetupIntent = paymentMethodMetadata.value?.stripeIntent is SetupIntent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = null,
    )

    // Only used to determine if we should skip the list and go to the add card view.
    // and how to populate that view.
    override var newPaymentSelection: PaymentSelection.New? =
        args.state.paymentSelection as? PaymentSelection.New

    override val primaryButtonUiState = primaryButtonUiStateMapper.forCustomFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null,
    )

    init {
        SessionSavedStateHandler.attachTo(this, savedStateHandle)

        savedStateHandle[SAVE_GOOGLE_PAY_STATE] = if (args.state.isGooglePayReady) {
            GooglePayState.Available
        } else {
            GooglePayState.NotAvailable
        }

        val linkState = args.state.linkState

        viewModelScope.launch {
            linkHandler.processingState.collect { processingState ->
                handleLinkProcessingState(processingState)
            }
        }

        // This is bad, but I don't think there's a better option
        PaymentSheet.FlowController.linkHandler = linkHandler

        linkHandler.linkInlineSelection.value = args.state.paymentSelection as? PaymentSelection.New.LinkInline
        linkHandler.setupLink(linkState)

        // After recovering from don't keep activities the paymentMethodMetadata will be saved,
        // calling setPaymentMethodMetadata would require the repository be initialized, which
        // would not be the case.
        if (paymentMethodMetadata.value == null) {
            setPaymentMethodMetadata(args.state.paymentMethodMetadata)
        }
        savedStateHandle[SAVE_PAYMENT_METHODS] = args.state.customerPaymentMethods
        savedStateHandle[SAVE_PROCESSING] = false

        updateSelection(args.state.paymentSelection)

        transitionToFirstScreen()
    }

    override val shouldCompleteLinkFlowInline: Boolean = false

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
                onError(processingState.message)
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

    override fun onFatal(throwable: Throwable) {
        mostRecentError = throwable
        _paymentOptionResult.tryEmit(
            PaymentOptionResult.Failed(
                error = throwable,
                paymentMethods = paymentMethods.value
            )
        )
    }

    override fun onUserCancel() {
        reportDismiss()
        _paymentOptionResult.tryEmit(
            PaymentOptionResult.Canceled(
                mostRecentError = mostRecentError,
                paymentSelection = determinePaymentSelectionUponCancel(),
                paymentMethods = paymentMethods.value,
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
        val paymentMethods = paymentMethods.value.orEmpty()
        val isStillAround = paymentMethods.any { it.id == paymentMethod.id }
        return this.takeIf { isStillAround }
    }

    override fun onFinish() {
        onUserSelection()
    }

    override fun onError(@StringRes error: Int?) =
        onError(error?.let { getApplication<Application>().getString(it) })

    override fun onError(error: String?) {
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
                is PaymentSelection.New -> processNewPaymentMethod(paymentSelection)
            }
        }
    }

    override fun onPaymentResult(paymentResult: PaymentResult) {
        savedStateHandle[SAVE_PROCESSING] = false
    }

    override fun handlePaymentMethodSelected(selection: PaymentSelection?) {
        if (!editing.value) {
            updateSelection(selection)

            if (selection?.requiresConfirmation != true) {
                onUserSelection()
            }
        }
    }

    override fun handleConfirmUSBankAccount(paymentSelection: PaymentSelection.New.USBankAccount) {
        updateSelection(paymentSelection)
        reportConfirmButtonPressed()
        onUserSelection()
    }

    override fun clearErrorMessages() {
        _error.value = null
    }

    private fun processExistingPaymentMethod(paymentSelection: PaymentSelection) {
        _paymentOptionResult.tryEmit(
            PaymentOptionResult.Succeeded(
                paymentSelection = paymentSelection,
                paymentMethods = paymentMethods.value
            )
        )
    }

    private fun processNewPaymentMethod(paymentSelection: PaymentSelection) {
        _paymentOptionResult.tryEmit(
            PaymentOptionResult.Succeeded(
                paymentSelection = paymentSelection,
                paymentMethods = paymentMethods.value
            )
        )
    }

    override fun determineInitialBackStack(): List<PaymentSheetScreen> {
        val target = if (args.state.showSavedPaymentMethods) {
            SelectSavedPaymentMethods
        } else {
            AddFirstPaymentMethod
        }

        return buildList {
            add(target)

            if (target is SelectSavedPaymentMethods && newPaymentSelection != null) {
                // The user has previously selected a new payment method. Instead of sending them
                // to the payment methods screen, we directly launch them into the payment method
                // form again.
                add(PaymentSheetScreen.AddAnotherPaymentMethod)
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
