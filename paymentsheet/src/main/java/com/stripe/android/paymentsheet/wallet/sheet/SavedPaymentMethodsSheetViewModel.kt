package com.stripe.android.paymentsheet.wallet.sheet

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
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.customer.CustomerAdapter
import com.stripe.android.paymentsheet.customer.CustomerAdapterConfig
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.paymentsheet.ui.HeaderTextFactory
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.paymentsheet.viewmodels.PrimaryButtonUiStateMapper
import com.stripe.android.paymentsheet.wallet.controller.SavedPaymentMethodsController
import com.stripe.android.ui.core.CardBillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.requireApplication
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

@JvmSuppressWildcards
internal class SavedPaymentMethodsSheetViewModel @Inject constructor(
    private val args: SavedPaymentMethodsSheetContract.Args,
    private val customerAdapter: CustomerAdapter,
    prefsRepositoryFactory: (PaymentSheet.CustomerConfiguration?) -> PrefsRepository,
    eventReporter: EventReporter,
    customerRepository: CustomerRepository,
    @IOContext workContext: CoroutineContext,
    application: Application,
    logger: Logger,
    lpmRepository: LpmRepository,
    savedStateHandle: SavedStateHandle,
    linkHandler: LinkHandler,
) : BaseSheetViewModel(
    application = application,
    config = args.paymentSheetConfig,
    prefsRepository = prefsRepositoryFactory(args.paymentSheetConfig.customer),
    eventReporter = eventReporter,
    customerRepository = customerRepository,
    workContext = workContext,
    logger = logger,
    lpmRepository = lpmRepository,
    savedStateHandle = savedStateHandle,
    linkHandler = linkHandler,
    headerTextFactory = HeaderTextFactory(isCompleteFlow = false),
) {
    private val primaryButtonUiStateMapper = PrimaryButtonUiStateMapper(
        context = getApplication(),
        config = config,
        isProcessingPayment = false,
        currentScreenFlow = currentScreen,
        buttonsEnabledFlow = buttonsEnabled,
        amountFlow = flowOf(null),
        selectionFlow = selection,
        customPrimaryButtonUiStateFlow = customPrimaryButtonUiState,
        onClick = this::onUserSelection,
    )

    private val _savedPaymentMethodsResults = MutableSharedFlow<SavedPaymentMethodsSheetResult>(replay = 1)
    internal val savedPaymentMethodsResults: SharedFlow<SavedPaymentMethodsSheetResult> = _savedPaymentMethodsResults

    private val _error = MutableStateFlow<String?>(null)
    internal val error: StateFlow<String?> = _error

    private val _paymentMethods = MutableStateFlow<List<PaymentMethod>>(listOf())

    override val primaryButtonUiState = primaryButtonUiStateMapper.forCustomFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null,
    )

    override var newPaymentSelection: PaymentSelection.New? =
        args.paymentSelection as? PaymentSelection.New

    override val shouldCompleteLinkFlowInline: Boolean = false

    init {
        viewModelScope.launch {
            savedStateHandle[SAVE_GOOGLE_PAY_STATE] = if (args.isGooglePayReady) {
                GooglePayState.Available
            } else {
                GooglePayState.NotAvailable
            }

            val stripeIntent = SetupIntent.fromJson(
                JSONObject(
                    """
                            {
                                "object": "setup_intent",
                                "created": ${System.currentTimeMillis()},
                                "livemode": false,
                                "payment_method_types": [
                                    "card"
                                ]
                            }
                        """.trimIndent()
                )
            )

            // TODO: How can we improve this?
            setStripeIntent(
                stripeIntent
            )

            linkHandler.prepareLink(null)

            supportedPaymentMethods = listOf(
                LpmRepository.hardcodedCardSpec(
                    CardBillingDetailsCollectionConfiguration(
                        address = CardBillingDetailsCollectionConfiguration.AddressCollectionMode.Never
                    )
                )
            )

            loadCustomerPaymentMethods()
            updateSelection(args.paymentSelection)

            // TODO: WHY?!
            lpmServerSpec = lpmRepository.serverSpecLoadingState.serverLpmSpecs

            transitionToFirstScreen()
        }
    }

    fun onUserSelection() {
        clearErrorMessages()
        selection.value?.let { paymentSelection ->
            when (paymentSelection) {
                is PaymentSelection.Saved,
                is PaymentSelection.GooglePay,
                is PaymentSelection.Link -> {
//                    customerAdapter.setSelectedPaymentMethodOption()
                }
                is PaymentSelection.New -> {
//                    customerAdapter.attachPaymentMethod()
                }
            }
        }
    }

    override fun onFatal(throwable: Throwable) {
        mostRecentError = throwable
    }

    override fun transitionToFirstScreen() {
        val target = if (_paymentMethods.value.isNotEmpty()) {
            PaymentSheetScreen.SelectSavedPaymentMethods
        } else {
            PaymentSheetScreen.AddFirstPaymentMethod
        }

        val initialBackStack = buildList {
            add(target)

            if (target is PaymentSheetScreen.SelectSavedPaymentMethods && newPaymentSelection != null) {
                // The user has previously selected a new payment method. Instead of sending them
                // to the payment methods screen, we directly launch them into the payment method
                // form again.
                add(PaymentSheetScreen.AddAnotherPaymentMethod)
            }
        }

        backStack.value = initialBackStack
    }

    override fun clearErrorMessages() {
        _error.value = null
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
        error("USBankAccount is not supported")
    }

    override fun onUserCancel() {
        // Handle cancel
        _savedPaymentMethodsResults.tryEmit(
            SavedPaymentMethodsSheetResult.Canceled
        )
    }

    override fun onPaymentResult(paymentResult: PaymentResult) {
        error("onPaymentResult should not be called")
    }

    override fun onFinish() {
        onUserSelection()
    }

    override fun onError(@StringRes error: Int?) {
        onError(error?.let { getApplication<Application>().getString(it) })
    }

    override fun onError(error: String?) {
        _error.value = error
    }

    private suspend fun loadCustomerPaymentMethods() {
        val paymentMethods = customerAdapter.fetchPaymentMethods()
        savedStateHandle[SAVE_PAYMENT_METHODS] = paymentMethods
        println("JAMES: ${this.paymentMethods}")
        _paymentMethods.update{
            paymentMethods
        }
    }

    internal class Factory(
        private val starterArgsSupplier: () -> SavedPaymentMethodsSheetContract.Args,
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam> {
        internal data class FallbackInitializeParam(
            val application: Application,
            val productUsage: Set<String>,
            val customerAdapterConfig: CustomerAdapterConfig,
        )

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<SavedPaymentMethodsSheetViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()
            val starterArgs = starterArgsSupplier()

            val injector = injectWithFallback(
                starterArgs.injectorKey,
                FallbackInitializeParam(
                    application,
                    starterArgs.productUsage,
                    SavedPaymentMethodsController.customerAdapterConfig
                )
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
            val component = DaggerSavedPaymentMethodsSheetViewModelFactoryComponent.builder()
                .context(arg.application)
                .productUsage(arg.productUsage)
                .customerAdapterConfig(arg.customerAdapterConfig)
                .build()
            component.inject(this)
            return component
        }
    }
}