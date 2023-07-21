package com.stripe.android.paymentsheet

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.model.PaymentIntent
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.DaggerPaymentOptionsViewModelFactoryComponent
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddFirstPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.paymentsheet.ui.HeaderTextFactory
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.paymentsheet.viewmodels.PrimaryButtonUiStateMapper
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.requireApplication
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
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
    lpmRepository: LpmRepository,
    savedStateHandle: SavedStateHandle,
    linkHandler: LinkHandler,
    linkConfigurationCoordinator: LinkConfigurationCoordinator,
    formViewModelSubComponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>
) : BaseSheetViewModel(
    application = application,
    config = args.state.config,
    prefsRepository = prefsRepositoryFactory(args.state.config?.customer),
    eventReporter = eventReporter,
    customerRepository = customerRepository,
    workContext = workContext,
    logger = logger,
    lpmRepository = lpmRepository,
    savedStateHandle = savedStateHandle,
    linkHandler = linkHandler,
    linkConfigurationCoordinator = linkConfigurationCoordinator,
    headerTextFactory = HeaderTextFactory(isCompleteFlow = false),
    formViewModelSubComponentBuilderProvider = formViewModelSubComponentBuilderProvider,
) {

    private val primaryButtonUiStateMapper = PrimaryButtonUiStateMapper(
        context = getApplication(),
        config = config,
        isProcessingPayment = args.state.stripeIntent is PaymentIntent,
        currentScreenFlow = currentScreen,
        buttonsEnabledFlow = buttonsEnabled,
        amountFlow = amount,
        selectionFlow = selection,
        customPrimaryButtonUiStateFlow = customPrimaryButtonUiState,
        onClick = this::onUserSelection,
    )

    private val _paymentOptionResult = MutableSharedFlow<PaymentOptionResult>(replay = 1)
    internal val paymentOptionResult: SharedFlow<PaymentOptionResult> = _paymentOptionResult

    private val _error = MutableStateFlow<String?>(null)
    internal val error: StateFlow<String?> = _error

    // Only used to determine if we should skip the list and go to the add card view.
    // and how to populate that view.
    override var newPaymentSelection: PaymentSelection.New? =
        args.state.paymentSelection as? PaymentSelection.New

    override val primaryButtonUiState = primaryButtonUiStateMapper.forCustomFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null,
    )

    private val isDecoupling: Boolean
        get() = args.state.stripeIntent.clientSecret == null

    init {
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

        linkHandler.linkInlineSelection.value = args.state.paymentSelection as? PaymentSelection.New.LinkInline
        linkHandler.setupLink(linkState)

        // After recovering from don't keep activities the stripe intent will be saved,
        // calling setStripeIntent would require the repository be initialized, which
        // would not be the case.
        if (stripeIntent.value == null) {
            setStripeIntent(args.state.stripeIntent)
        }
        savedStateHandle[SAVE_PAYMENT_METHODS] = args.state.customerPaymentMethods
        savedStateHandle[SAVE_PROCESSING] = false

        updateSelection(args.state.paymentSelection)

        lpmServerSpec = lpmRepository.serverSpecLoadingState.serverLpmSpecs

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
                processingState.details?.let {
                    // Link PaymentDetails was created successfully, use it to confirm the Stripe Intent.
                    updateSelection(PaymentSelection.New.LinkInline(it))
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
            eventReporter.onSelectPaymentOption(
                paymentSelection = paymentSelection,
                currency = stripeIntent.value?.currency,
                isDecoupling = isDecoupling,
            )

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
        onUserSelection()
    }

    override fun clearErrorMessages() {
        _error.value = null
    }

    private fun processExistingPaymentMethod(paymentSelection: PaymentSelection) {
        prefsRepository.savePaymentSelection(paymentSelection)
        _paymentOptionResult.tryEmit(
            PaymentOptionResult.Succeeded(
                paymentSelection = paymentSelection,
                paymentMethods = paymentMethods.value
            )
        )
    }

    private fun processNewPaymentMethod(paymentSelection: PaymentSelection) {
        prefsRepository.savePaymentSelection(paymentSelection)
        _paymentOptionResult.tryEmit(
            PaymentOptionResult.Succeeded(
                paymentSelection = paymentSelection,
                paymentMethods = paymentMethods.value
            )
        )
    }

    override fun transitionToFirstScreen() {
        val target = if (args.state.hasPaymentOptions) {
            SelectSavedPaymentMethods
        } else {
            AddFirstPaymentMethod
        }

        val initialBackStack = buildList {
            add(target)

            if (target is SelectSavedPaymentMethods && newPaymentSelection != null) {
                // The user has previously selected a new payment method. Instead of sending them
                // to the payment methods screen, we directly launch them into the payment method
                // form again.
                add(PaymentSheetScreen.AddAnotherPaymentMethod)
            }
        }

        backStack.value = initialBackStack
        reportNavigationEvent(initialBackStack.last())
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
