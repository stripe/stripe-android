package com.stripe.android.customersheet

import android.app.Application
import android.content.res.Resources
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IS_LIVE_MODE
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.customersheet.CustomerAdapter.PaymentOption.Companion.toPaymentOption
import com.stripe.android.customersheet.analytics.CustomerSheetEventReporter
import com.stripe.android.customersheet.injection.CustomerSheetViewModelScope
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.paymentsheet.state.toInternal
import com.stripe.android.paymentsheet.ui.transformToPaymentMethodCreateParams
import com.stripe.android.paymentsheet.utils.mapAsStateFlow
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider

@OptIn(ExperimentalCustomerSheetApi::class)
@CustomerSheetViewModelScope
internal class CustomerSheetViewModel @Inject constructor(
    private val application: Application,
    initialBackStack: @JvmSuppressWildcards List<CustomerSheetViewState>,
    private var savedPaymentSelection: PaymentSelection?,
    private val paymentConfigurationProvider: Provider<PaymentConfiguration>,
    private val resources: Resources,
    private val configuration: CustomerSheet.Configuration,
    private val logger: Logger,
    private val stripeRepository: StripeRepository,
    private val customerAdapter: CustomerAdapter,
    private val lpmRepository: LpmRepository,
    private val statusBarColor: () -> Int?,
    private val eventReporter: CustomerSheetEventReporter,
    @Named(IS_LIVE_MODE) private val isLiveModeProvider: () -> Boolean,
    private val formViewModelSubcomponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>,
    private val paymentLauncherFactory: StripePaymentLauncherAssistedFactory,
    private val intentConfirmationInterceptor: IntentConfirmationInterceptor,
    private val googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
) : ViewModel() {

    private val backStack = MutableStateFlow(initialBackStack)
    val viewState: StateFlow<CustomerSheetViewState> = backStack.mapAsStateFlow { it.last() }

    private val _result = MutableStateFlow<InternalCustomerSheetResult?>(null)
    val result: StateFlow<InternalCustomerSheetResult?> = _result

    private var isGooglePayReadyAndEnabled: Boolean = false
    private var paymentLauncher: PaymentLauncher? = null

    private var unconfirmedPaymentMethod: PaymentMethod? = null

    init {
        lpmRepository.initializeWithCardSpec(
            configuration.billingDetailsCollectionConfiguration.toInternal()
        )

        configuration.appearance.parseAppearance()

        if (viewState.value is CustomerSheetViewState.Loading) {
            loadPaymentMethods()
        }
    }

    fun handleViewAction(viewAction: CustomerSheetViewAction) {
        when (viewAction) {
            is CustomerSheetViewAction.OnDismissed -> onDismissed()
            is CustomerSheetViewAction.OnAddCardPressed -> onAddCardPressed()
            is CustomerSheetViewAction.OnBackPressed -> onBackPressed()
            is CustomerSheetViewAction.OnEditPressed -> onEditPressed()
            is CustomerSheetViewAction.OnItemRemoved -> onItemRemoved(viewAction.paymentMethod)
            is CustomerSheetViewAction.OnItemSelected -> onItemSelected(viewAction.selection)
            is CustomerSheetViewAction.OnPrimaryButtonPressed -> onPrimaryButtonPressed()
            is CustomerSheetViewAction.OnFormDataUpdated -> onFormDataUpdated(viewAction.formData)
        }
    }

    fun providePaymentMethodName(code: PaymentMethodCode?): String {
        val paymentMethod = lpmRepository.fromCode(code)
        return paymentMethod?.displayNameResource?.let {
            resources.getString(it)
        }.orEmpty()
    }

    fun registerFromActivity(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner
    ) {
        val launcher = activityResultCaller.registerForActivityResult(
            PaymentLauncherContract(),
            ::onPaymentLauncherResult
        )

        paymentLauncher = paymentLauncherFactory.create(
            publishableKey = { paymentConfigurationProvider.get().publishableKey },
            stripeAccountId = { paymentConfigurationProvider.get().stripeAccountId },
            statusBarColor = statusBarColor(),
            hostActivityLauncher = launcher,
        )

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    launcher.unregister()
                    paymentLauncher = null
                    super.onDestroy(owner)
                }
            }
        )
    }

    private fun onPaymentLauncherResult(result: PaymentResult) {
        when (result) {
            is PaymentResult.Canceled -> {
                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        enabled = true,
                        isProcessing = false,
                    )
                }
            }
            is PaymentResult.Completed -> {
                safeUpdateSelectPaymentMethodState { viewState ->
                    unconfirmedPaymentMethod?.let { method ->
                        unconfirmedPaymentMethod = null

                        viewState.copy(
                            savedPaymentMethods = listOf(method) + viewState.savedPaymentMethods,
                            paymentSelection = PaymentSelection.Saved(paymentMethod = method),
                            primaryButtonVisible = true,
                            primaryButtonLabel = resources.getString(
                                R.string.stripe_paymentsheet_confirm
                            ),
                        )
                    } ?: viewState
                }
                onBackPressed()
            }
            is PaymentResult.Failed -> {
                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        enabled = true,
                        isProcessing = false,
                        errorMessage = result.throwable.stripeErrorMessage(application),
                    )
                }
            }
        }
    }

    private fun loadPaymentMethods() {
        viewModelScope.launch {
            val paymentMethodsResult = async {
                customerAdapter.retrievePaymentMethods()
            }
            val selectedPaymentOption = async {
                customerAdapter.retrieveSelectedPaymentOption()
            }

            paymentMethodsResult.await().flatMap { paymentMethods ->
                selectedPaymentOption.await().map { paymentOption ->
                    Pair(paymentMethods, paymentOption)
                }
            }.map {
                val paymentMethods = it.first
                val paymentOption = it.second
                val selection = paymentOption?.toPaymentSelection { id ->
                    paymentMethods.find { it.id == id }
                }
                Pair(paymentMethods, selection)
            }.onFailure { cause, _ ->
                _result.update {
                    InternalCustomerSheetResult.Error(exception = cause)
                }
            }.onSuccess { result ->
                var paymentMethods = result.first
                val paymentSelection = result.second

                paymentSelection?.apply {
                    val selectedPaymentMethod = (this as? PaymentSelection.Saved)?.paymentMethod
                    // The order of the payment methods should be selected PM and then any additional PMs
                    // The carousel always starts with Add and Google Pay (if enabled)
                    paymentMethods = paymentMethods.sortedWith { left, right ->
                        // We only care to move the selected payment method, all others stay in the
                        // order they were before
                        when {
                            left.id == selectedPaymentMethod?.id -> -1
                            right.id == selectedPaymentMethod?.id -> 1
                            else -> 0
                        }
                    }
                }

                savedPaymentSelection = paymentSelection
                isGooglePayReadyAndEnabled = configuration.googlePayEnabled && googlePayRepositoryFactory(
                    if (isLiveModeProvider()) GooglePayEnvironment.Production else GooglePayEnvironment.Test
                ).isReady().first()

                transitionToInitialScreen(
                    paymentMethods = paymentMethods,
                    paymentSelection = paymentSelection
                )
            }
        }
    }

    private fun transitionToInitialScreen(paymentMethods: List<PaymentMethod>, paymentSelection: PaymentSelection?) {
        if (paymentMethods.isEmpty() && !isGooglePayReadyAndEnabled) {
            transitionToAddPaymentMethod(isFirstPaymentMethod = true)
        } else {
            transition(
                to = buildDefaultSelectPaymentMethod {
                    it.copy(savedPaymentMethods = paymentMethods, paymentSelection = paymentSelection)
                },
                reset = true
            )
        }
    }

    private fun onAddCardPressed() {
        transitionToAddPaymentMethod(isFirstPaymentMethod = false)
    }

    private fun onDismissed() {
        _result.update {
            InternalCustomerSheetResult.Canceled(savedPaymentSelection)
        }
    }

    private fun onBackPressed() {
        if (backStack.value.size == 1) {
            _result.tryEmit(
                InternalCustomerSheetResult.Canceled(savedPaymentSelection)
            )
        } else {
            backStack.update {
                it.dropLast(1)
            }
        }
    }

    private fun onEditPressed() {
        if (viewState.value.isEditing) {
            eventReporter.onEditCompleted()
        } else {
            eventReporter.onEditTapped()
        }
        updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
            val isEditing = !it.isEditing
            it.copy(
                isEditing = isEditing,
                primaryButtonVisible = !isEditing && savedPaymentSelection != it.paymentSelection,
            )
        }
    }

    private fun onFormDataUpdated(formData: FormViewModel.ViewData) {
        updateViewState<CustomerSheetViewState.AddPaymentMethod> {
            it.copy(
                formViewData = formData,
            )
        }
    }

    private fun onItemRemoved(paymentMethod: PaymentMethod) {
        viewModelScope.launch {
            customerAdapter.detachPaymentMethod(
                paymentMethodId = paymentMethod.id!!
            ).onSuccess {
                when (val currentViewState = viewState.value) {
                    is CustomerSheetViewState.SelectPaymentMethod -> {
                        eventReporter.onRemovePaymentMethodSucceeded()
                        val savedPaymentMethods = currentViewState.savedPaymentMethods.filter { pm ->
                            pm.id != paymentMethod.id!!
                        }

                        updateViewState<CustomerSheetViewState.SelectPaymentMethod> { viewState ->
                            viewState.copy(
                                savedPaymentMethods = savedPaymentMethods,
                                isEditing = false,
                                paymentSelection = viewState.paymentSelection.takeUnless { selection ->
                                    val removedPaymentSelection = selection is PaymentSelection.Saved &&
                                        selection.paymentMethod == paymentMethod

                                    if (removedPaymentSelection && savedPaymentSelection == selection) {
                                        savedPaymentSelection = null
                                    }

                                    removedPaymentSelection
                                } ?: savedPaymentSelection,
                            )
                        }

                        if (savedPaymentMethods.isEmpty() && !isGooglePayReadyAndEnabled) {
                            transitionToAddPaymentMethod(isFirstPaymentMethod = true)
                        }
                    }
                    else -> Unit
                }
            }.onFailure { cause, displayMessage ->
                eventReporter.onRemovePaymentMethodFailed()
                logger.error(
                    msg = "Failed to detach payment method: $paymentMethod",
                    t = cause,
                )
                updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
                    it.copy(
                        errorMessage = displayMessage,
                        isProcessing = false,
                    )
                }
            }
        }
    }

    private fun onItemSelected(paymentSelection: PaymentSelection?) {
        // TODO (jameswoo) consider clearing the error message onItemSelected, currently the only
        // error source is when the payment methods cannot be loaded
        when (paymentSelection) {
            is PaymentSelection.GooglePay, is PaymentSelection.Saved -> {
                if (viewState.value.isEditing) {
                    return
                }

                updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
                    it.copy(
                        paymentSelection = paymentSelection,
                        primaryButtonVisible = savedPaymentSelection != paymentSelection,
                        primaryButtonLabel = resources.getString(
                            R.string.stripe_paymentsheet_confirm
                        ),
                    )
                }
            }
            else -> error("Unsupported payment selection $paymentSelection")
        }
    }

    private fun onPrimaryButtonPressed() {
        when (val currentViewState = viewState.value) {
            is CustomerSheetViewState.AddPaymentMethod -> {
                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        isProcessing = true,
                        enabled = false,
                    )
                }
                lpmRepository.fromCode(currentViewState.paymentMethodCode)?.let { paymentMethodSpec ->
                    addPaymentMethod(
                        paymentMethodSpec = paymentMethodSpec,
                        formViewData = currentViewState.formViewData,
                    )
                } ?: error("${currentViewState.paymentMethodCode} is not supported")
            }
            is CustomerSheetViewState.SelectPaymentMethod -> {
                updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
                    it.copy(isProcessing = true)
                }
                when (val paymentSelection = currentViewState.paymentSelection) {
                    is PaymentSelection.GooglePay -> selectGooglePay()
                    is PaymentSelection.Saved -> selectSavedPaymentMethod(paymentSelection)
                    null -> selectSavedPaymentMethod(null)
                    else -> error("$paymentSelection is not supported")
                }
            }
            else -> error("${viewState.value} is not supported")
        }
    }

    private fun addPaymentMethod(
        paymentMethodSpec: LpmRepository.SupportedPaymentMethod,
        formViewData: FormViewModel.ViewData,
    ) {
        viewModelScope.launch {
            if (formViewData.completeFormValues == null) error("completeFormValues cannot be null")
            val params = formViewData.completeFormValues
                .transformToPaymentMethodCreateParams(paymentMethodSpec)
            createPaymentMethod(params)
                .onSuccess { paymentMethod ->
                    attachPaymentMethodToCustomer(paymentMethod)
                }.onFailure { throwable ->
                    logger.error(
                        msg = "Failed to create payment method for $paymentMethodSpec",
                        t = throwable,
                    )
                    updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                        it.copy(
                            errorMessage = throwable.stripeErrorMessage(application),
                            isProcessing = false,
                        )
                    }
                }
        }
    }

    private fun transitionToAddPaymentMethod(isFirstPaymentMethod: Boolean) {
        val paymentMethodCode = PaymentMethod.Type.Card.code

        val observe = buildFormObserver(
            paymentMethodCode = paymentMethodCode,
            application = application,
            configuration = configuration,
            formViewModelSubcomponentBuilderProvider = formViewModelSubcomponentBuilderProvider,
            onFormDataUpdated = ::onFormDataUpdated
        )

        transition(
            to = CustomerSheetViewState.AddPaymentMethod(
                paymentMethodCode = paymentMethodCode,
                formViewData = FormViewModel.ViewData(),
                enabled = true,
                isLiveMode = isLiveModeProvider(),
                isProcessing = false,
                isFirstPaymentMethod = isFirstPaymentMethod
            ),
            reset = isFirstPaymentMethod
        )

        observe()
    }

    private suspend fun createPaymentMethod(
        createParams: PaymentMethodCreateParams
    ): Result<PaymentMethod> {
        return stripeRepository.createPaymentMethod(
            paymentMethodCreateParams = createParams,
            options = ApiRequest.Options(
                apiKey = paymentConfigurationProvider.get().publishableKey,
                stripeAccount = paymentConfigurationProvider.get().stripeAccountId,
            )
        )
    }

    private fun attachPaymentMethodToCustomer(paymentMethod: PaymentMethod) {
        viewModelScope.launch {
            if (customerAdapter.canCreateSetupIntents) {
                attachWithSetupIntent(paymentMethod = paymentMethod)
            } else {
                attachPaymentMethod(paymentMethod = paymentMethod)
            }
        }
    }

    private suspend fun attachWithSetupIntent(paymentMethod: PaymentMethod) {
        customerAdapter.setupIntentClientSecretForCustomerAttach()
            .mapCatching { clientSecret ->
                val intent = stripeRepository.retrieveSetupIntent(
                    clientSecret = clientSecret,
                    options = ApiRequest.Options(
                        apiKey = paymentConfigurationProvider.get().publishableKey,
                        stripeAccount = paymentConfigurationProvider.get().stripeAccountId,
                    ),
                ).getOrThrow()

                handleStripeIntent(intent, clientSecret, paymentMethod).getOrThrow()

                eventReporter.onAttachPaymentMethodSucceeded(
                    style = CustomerSheetEventReporter.AddPaymentMethodStyle.SetupIntent
                )
            }.onFailure { cause, displayMessage ->
                eventReporter.onAttachPaymentMethodFailed(
                    style = CustomerSheetEventReporter.AddPaymentMethodStyle.SetupIntent
                )
                logger.error(
                    msg = "Failed to attach payment method to SetupIntent: $paymentMethod",
                    t = cause,
                )
                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        errorMessage = displayMessage ?: cause.stripeErrorMessage(application),
                        enabled = true,
                        isProcessing = false,
                    )
                }
            }
    }

    private suspend fun handleStripeIntent(
        stripeIntent: StripeIntent,
        clientSecret: String,
        paymentMethod: PaymentMethod
    ): Result<Unit> {
        val nextStep = intentConfirmationInterceptor.intercept(
            initializationMode = PaymentSheet.InitializationMode.SetupIntent(
                clientSecret = clientSecret,
            ),
            paymentMethod = paymentMethod,
            shippingValues = null,
            setupForFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession,
        )

        unconfirmedPaymentMethod = paymentMethod

        return when (nextStep) {
            is IntentConfirmationInterceptor.NextStep.Complete -> {
                safeUpdateSelectPaymentMethodState { viewState ->
                    unconfirmedPaymentMethod?.let { method ->
                        unconfirmedPaymentMethod = null

                        viewState.copy(
                            savedPaymentMethods = listOf(method) + viewState.savedPaymentMethods,
                            paymentSelection = PaymentSelection.Saved(paymentMethod = method),
                            primaryButtonVisible = true,
                            primaryButtonLabel = resources.getString(
                                R.string.stripe_paymentsheet_confirm
                            ),
                        )
                    } ?: viewState
                }
                onBackPressed()
                Result.success(Unit)
            }
            is IntentConfirmationInterceptor.NextStep.Confirm -> {
                confirmStripeIntent(nextStep.confirmParams)
                Result.success(Unit)
            }
            is IntentConfirmationInterceptor.NextStep.Fail -> {
                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        isProcessing = false,
                        errorMessage = nextStep.message,
                    )
                }
                Result.failure(nextStep.cause)
            }
            is IntentConfirmationInterceptor.NextStep.HandleNextAction -> {
                handleNextAction(
                    clientSecret = nextStep.clientSecret,
                    stripeIntent = stripeIntent
                )
                Result.success(Unit)
            }
        }
    }

    private fun confirmStripeIntent(confirmStripeIntentParams: ConfirmStripeIntentParams) {
        runCatching {
            requireNotNull(paymentLauncher)
        }.fold(
            onSuccess = {
                when (confirmStripeIntentParams) {
                    is ConfirmSetupIntentParams -> {
                        it.confirm(confirmStripeIntentParams)
                    }
                    else -> error("Only SetupIntents are supported at this time")
                }
            },
            onFailure = { throwable ->
                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        isProcessing = false,
                        errorMessage = throwable.stripeErrorMessage(application),
                    )
                }
            }
        )
    }

    private fun handleNextAction(
        clientSecret: String,
        stripeIntent: StripeIntent,
    ) {
        runCatching {
            requireNotNull(paymentLauncher)
        }.fold(
            onSuccess = {
                when (stripeIntent) {
                    is SetupIntent -> {
                        it.handleNextActionForSetupIntent(clientSecret)
                    }
                    else -> error("Only SetupIntents are supported at this time")
                }
            },
            onFailure = { throwable ->
                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        isProcessing = false,
                        errorMessage = throwable.stripeErrorMessage(application),
                    )
                }
            }
        )
    }

    private suspend fun attachPaymentMethod(paymentMethod: PaymentMethod) {
        customerAdapter.attachPaymentMethod(paymentMethod.id!!)
            .onSuccess {
                eventReporter.onAttachPaymentMethodSucceeded(
                    style = CustomerSheetEventReporter.AddPaymentMethodStyle.CreateAttach
                )
                safeUpdateSelectPaymentMethodState {
                    it.copy(
                        savedPaymentMethods = listOf(paymentMethod) + it.savedPaymentMethods,
                        paymentSelection = PaymentSelection.Saved(paymentMethod = paymentMethod),
                        primaryButtonVisible = true,
                        primaryButtonLabel = resources.getString(
                            R.string.stripe_paymentsheet_confirm
                        ),
                    )
                }
                onBackPressed()
            }.onFailure { cause, displayMessage ->
                eventReporter.onAttachPaymentMethodFailed(
                    style = CustomerSheetEventReporter.AddPaymentMethodStyle.CreateAttach
                )
                logger.error(
                    msg = "Failed to attach payment method to Customer: $paymentMethod",
                    t = cause,
                )
                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        errorMessage = displayMessage,
                        isProcessing = false,
                    )
                }
            }
    }

    private fun selectSavedPaymentMethod(savedPaymentSelection: PaymentSelection.Saved?) {
        viewModelScope.launch {
            customerAdapter.setSelectedPaymentOption(
                savedPaymentSelection?.toPaymentOption()
            ).onSuccess {
                confirmPaymentSelection(
                    paymentSelection = savedPaymentSelection,
                    type = savedPaymentSelection?.paymentMethod?.type?.code,
                )
            }.onFailure { cause, displayMessage ->
                confirmPaymentSelectionError(
                    paymentSelection = savedPaymentSelection,
                    type = savedPaymentSelection?.paymentMethod?.type?.code,
                    cause = cause,
                    displayMessage = displayMessage,
                )
            }
        }
    }

    private fun selectGooglePay() {
        viewModelScope.launch {
            customerAdapter.setSelectedPaymentOption(CustomerAdapter.PaymentOption.GooglePay)
                .onSuccess {
                    confirmPaymentSelection(
                        paymentSelection = PaymentSelection.GooglePay,
                        type = "google_pay"
                    )
                }.onFailure { cause, displayMessage ->
                    confirmPaymentSelectionError(
                        paymentSelection = PaymentSelection.GooglePay,
                        type = "google_pay",
                        cause = cause,
                        displayMessage = displayMessage,
                    )
                }
        }
    }

    private fun confirmPaymentSelection(paymentSelection: PaymentSelection?, type: String?) {
        type?.let {
            eventReporter.onConfirmPaymentMethodSucceeded(type)
        }
        _result.tryEmit(
            InternalCustomerSheetResult.Selected(
                paymentSelection = paymentSelection,
            )
        )
    }

    private fun confirmPaymentSelectionError(
        paymentSelection: PaymentSelection?,
        type: String?,
        cause: Throwable,
        displayMessage: String?
    ) {
        type?.let {
            eventReporter.onConfirmPaymentMethodFailed(type)
        }
        logger.error(
            msg = "Failed to persist payment selection: $paymentSelection",
            t = cause,
        )
        updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
            it.copy(
                errorMessage = displayMessage,
                isProcessing = false,
            )
        }
    }

    private fun safeUpdateSelectPaymentMethodState(
        update: (state: CustomerSheetViewState.SelectPaymentMethod) -> CustomerSheetViewState.SelectPaymentMethod
    ) {
        val hasSelectPaymentMethodInBackStack = backStack.value.any { viewState ->
            viewState is CustomerSheetViewState.SelectPaymentMethod
        }

        if (hasSelectPaymentMethodInBackStack) {
            updateViewState<CustomerSheetViewState.SelectPaymentMethod> { state ->
                update(state)
            }
        } else {
            backStack.update { currentStack ->
                listOf(buildDefaultSelectPaymentMethod(update)) + currentStack
            }
        }
    }

    private fun buildDefaultSelectPaymentMethod(
        override: (viewState: CustomerSheetViewState.SelectPaymentMethod) -> CustomerSheetViewState.SelectPaymentMethod
    ): CustomerSheetViewState.SelectPaymentMethod {
        return override(
            CustomerSheetViewState.SelectPaymentMethod(
                title = configuration.headerTextForSelectionScreen,
                savedPaymentMethods = emptyList(),
                paymentSelection = null,
                isLiveMode = isLiveModeProvider(),
                isProcessing = false,
                isEditing = false,
                isGooglePayEnabled = isGooglePayReadyAndEnabled,
                primaryButtonVisible = false,
                primaryButtonLabel = resources.getString(
                    R.string.stripe_paymentsheet_confirm
                ),
                errorMessage = null,
            )
        )
    }

    private fun transition(to: CustomerSheetViewState, reset: Boolean = false) {
        when (to) {
            is CustomerSheetViewState.AddPaymentMethod ->
                eventReporter.onScreenPresented(CustomerSheetEventReporter.Screen.AddPaymentMethod)
            is CustomerSheetViewState.SelectPaymentMethod ->
                eventReporter.onScreenPresented(CustomerSheetEventReporter.Screen.SelectPaymentMethod)
            else -> { }
        }

        backStack.update {
            if (reset) listOf(to) else it + to
        }
    }

    private inline fun <reified T : CustomerSheetViewState> updateViewState(transform: (T) -> T) {
        backStack.update { currentBackStack ->
            currentBackStack.map {
                if (it is T) {
                    transform(it)
                } else {
                    it
                }
            }
        }
    }

    object Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CustomerSessionViewModel.component.customerSheetViewModelComponentBuilder
                .build().viewModel as T
        }
    }
}
