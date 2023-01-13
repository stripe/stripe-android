package com.stripe.android.paymentsheet

import android.app.Application
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.DaggerPaymentOptionsViewModelFactoryComponent
import com.stripe.android.paymentsheet.injection.PaymentOptionsViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddFirstPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.SelectSavedPaymentMethods
import com.stripe.android.paymentsheet.paymentdatacollection.ach.ACHText
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.ui.HeaderTextFactory
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import com.stripe.android.utils.requireApplication
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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
    @InjectorKey injectorKey: String,
    lpmResourceRepository: ResourceRepository<LpmRepository>,
    addressResourceRepository: ResourceRepository<AddressRepository>,
    savedStateHandle: SavedStateHandle,
    linkLauncher: LinkPaymentLauncher
) : BaseSheetViewModel(
    application = application,
    config = args.state.config,
    prefsRepository = prefsRepositoryFactory(args.state.config?.customer),
    eventReporter = eventReporter,
    customerRepository = customerRepository,
    workContext = workContext,
    logger = logger,
    injectorKey = injectorKey,
    lpmResourceRepository = lpmResourceRepository,
    addressResourceRepository = addressResourceRepository,
    savedStateHandle = savedStateHandle,
    linkLauncher = linkLauncher,
    headerTextFactory = HeaderTextFactory(isCompleteFlow = false),
) {
    @VisibleForTesting
    internal val _paymentOptionResult = MutableLiveData<PaymentOptionResult>()
    internal val paymentOptionResult: LiveData<PaymentOptionResult> = _paymentOptionResult

    private val _error = MutableLiveData<String?>()
    internal val error: LiveData<String?> = _error

    // Only used to determine if we should skip the list and go to the add card view.
    // and how to populate that view.
    override var newPaymentSelection = args.state.newPaymentSelection

    override var linkInlineSelection = MutableLiveData<PaymentSelection.New.LinkInline?>(
        args.state.newPaymentSelection as? PaymentSelection.New.LinkInline,
    )

    init {
        savedStateHandle[SAVE_GOOGLE_PAY_STATE] = if (args.state.isGooglePayReady) {
            GooglePayState.Available
        } else {
            GooglePayState.NotAvailable
        }

        val linkState = args.state.linkState

        _isLinkEnabled.value = linkState != null
        activeLinkSession.value = linkState?.loginState == LinkState.LoginState.LoggedIn

        if (linkState != null) {
            setupLink(linkState)
        }

        // After recovering from don't keep activities the stripe intent will be saved,
        // calling setStripeIntent would require the repository be initialized, which
        // would not be the case.
        if (stripeIntent.value == null) {
            setStripeIntent(args.state.stripeIntent)
        }
        savedStateHandle[SAVE_PAYMENT_METHODS] = args.state.customerPaymentMethods
        savedStateHandle[SAVE_PROCESSING] = false

        // If we are not recovering from don't keep activities than the resources
        // repository is loaded, and we should save off the LPM repository server specs so
        // it can be restored after don't keep activities or process killed.
        if (lpmResourceRepository.getRepository().isLoaded()) {
            lpmServerSpec =
                lpmResourceRepository.getRepository().serverSpecLoadingState.serverLpmSpecs
        }
    }

    override fun onFatal(throwable: Throwable) {
        mostRecentError = throwable
        _paymentOptionResult.value =
            PaymentOptionResult.Failed(
                error = throwable,
                paymentMethods = paymentMethods.value
            )
    }

    override fun onUserCancel() {
        _paymentOptionResult.value =
            PaymentOptionResult.Canceled(
                mostRecentError = mostRecentError,
                paymentMethods = paymentMethods.value
            )
    }

    override fun onFinish() {
        onUserSelection()
    }

    override fun onError(@StringRes error: Int?) =
        onError(error?.let { getApplication<Application>().getString(it) })

    override fun onError(error: String?) {
        error?.let {
            _error.value = it
        }
    }

    fun onUserSelection() {
        clearErrorMessages()

        selection.value?.let { paymentSelection ->
            // TODO(michelleb-stripe): Should the payment selection in the event be the saved or new item?
            eventReporter.onSelectPaymentOption(paymentSelection)

            when (paymentSelection) {
                is PaymentSelection.Saved ->
                    // We don't want the USBankAccount selection to close the payment sheet right
                    // away, the user needs to accept a mandate
                    if (paymentSelection.paymentMethod.type != PaymentMethod.Type.USBankAccount) {
                        processExistingPaymentMethod(
                            paymentSelection
                        )
                    }
                is PaymentSelection.GooglePay,
                is PaymentSelection.Link -> processExistingPaymentMethod(paymentSelection)
                is PaymentSelection.New -> processNewPaymentMethod(paymentSelection)
            }
        }
    }

    private fun setupLink(linkState: LinkState) {
        _linkConfiguration.value = linkState.configuration

        if (linkState.isReadyForUse) {
            // If account exists, select Link by default
            savedStateHandle[SAVE_SELECTION] = PaymentSelection.Link
        }
    }

    override fun onLinkPaymentDetailsCollected(linkPaymentDetails: LinkPaymentDetails.New?) {
        linkPaymentDetails?.let {
            // Link PaymentDetails was created successfully, use it to confirm the Stripe Intent.
            updateSelection(PaymentSelection.New.LinkInline(it))
            onUserSelection()
        } ?: run {
            // Creating Link PaymentDetails failed, fallback to regular checkout.
            // paymentSelection is already set to the card parameters from the form.
            onUserSelection()
        }
    }

    override fun onPaymentResult(paymentResult: PaymentResult) {
        savedStateHandle[SAVE_PROCESSING] = false
    }

    override fun updateSelection(selection: PaymentSelection?) {
        super.updateSelection(selection)
        when {
            selection is PaymentSelection.Saved &&
                selection.paymentMethod.type == PaymentMethod.Type.USBankAccount -> {
                updateBelowButtonText(
                    ACHText.getContinueMandateText(getApplication())
                )
                updatePrimaryButtonUIState(
                    PrimaryButton.UIState(
                        label = getApplication<Application>().getString(
                            R.string.stripe_continue_button_label
                        ),
                        visible = true,
                        enabled = true,
                        onClick = {
                            processExistingPaymentMethod(selection)
                        }
                    )
                )
            }
            selection is PaymentSelection.Saved ||
                selection is PaymentSelection.GooglePay -> {
                updatePrimaryButtonUIState(
                    primaryButtonUIState.value?.copy(
                        visible = false
                    )
                )
            }
            else -> {
                updatePrimaryButtonUIState(
                    primaryButtonUIState.value?.copy(
                        label = getApplication<Application>().getString(
                            R.string.stripe_continue_button_label
                        ),
                        visible = true,
                        enabled = true,
                        onClick = {
                            onUserSelection()
                        }
                    )
                )
            }
        }
    }

    override fun clearErrorMessages() {
        _error.value = null
    }

    private fun processExistingPaymentMethod(paymentSelection: PaymentSelection) {
        prefsRepository.savePaymentSelection(paymentSelection)
        _paymentOptionResult.value =
            PaymentOptionResult.Succeeded(
                paymentSelection = paymentSelection,
                paymentMethods = paymentMethods.value
            )
    }

    private fun processNewPaymentMethod(paymentSelection: PaymentSelection) {
        prefsRepository.savePaymentSelection(paymentSelection)
        _paymentOptionResult.value =
            PaymentOptionResult.Succeeded(
                paymentSelection = paymentSelection,
                paymentMethods = paymentMethods.value
            )
    }

    fun transitionToFirstScreenWhenReady() {
        viewModelScope.launch {
            awaitReady()
            awaitRepositoriesReady()
            transitionToFirstScreen()
        }
    }

    private suspend fun awaitReady() {
        isReadyEvents.asFlow().filter { it.peekContent() }.first()
    }

    private suspend fun awaitRepositoriesReady() {
        isResourceRepositoryReady.asFlow().filterNotNull().filter { it }.first()
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
    }

    internal class Factory(
        private val starterArgsSupplier: () -> PaymentOptionContract.Args,
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam> {
        internal data class FallbackInitializeParam(
            val application: Application,
            val productUsage: Set<String>
        )

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<PaymentOptionsViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()
            val starterArgs = starterArgsSupplier()

            val injector = injectWithFallback(
                starterArgs.injectorKey,
                FallbackInitializeParam(application, starterArgs.productUsage)
            )

            val subcomponent = subComponentBuilderProvider.get()
                .application(application)
                .args(starterArgs)
                .savedStateHandle(savedStateHandle)
                .build()

            val viewModel = subcomponent.viewModel
            viewModel.injector = requireNotNull(injector as NonFallbackInjector)
            return viewModel as T
        }

        override fun fallbackInitialize(arg: FallbackInitializeParam): Injector {
            val component = DaggerPaymentOptionsViewModelFactoryComponent.builder()
                .context(arg.application)
                .productUsage(arg.productUsage)
                .build()
            component.inject(this)
            return component
        }
    }
}
