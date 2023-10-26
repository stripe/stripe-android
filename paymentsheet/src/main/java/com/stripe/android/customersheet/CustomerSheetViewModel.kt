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
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.CustomerAdapter.PaymentOption.Companion.toPaymentOption
import com.stripe.android.customersheet.analytics.CustomerSheetEventReporter
import com.stripe.android.customersheet.injection.CustomerSheetViewModelScope
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
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.state.toInternal
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.transformToPaymentMethodCreateParams
import com.stripe.android.paymentsheet.utils.mapAsStateFlow
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext
import com.stripe.android.ui.core.R as UiCoreR

@OptIn(ExperimentalCustomerSheetApi::class)
@CustomerSheetViewModelScope
internal class CustomerSheetViewModel @Inject constructor(
    private val application: Application, // TODO (jameswoo) remove application
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
    private val workContext: CoroutineContext = Dispatchers.IO,
    @Named(IS_LIVE_MODE) private val isLiveModeProvider: () -> Boolean,
    val formViewModelSubcomponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>,
    private val paymentLauncherFactory: StripePaymentLauncherAssistedFactory,
    private val intentConfirmationInterceptor: IntentConfirmationInterceptor,
    private val customerSheetLoader: CustomerSheetLoader,
) : ViewModel() {

    private val backStack = MutableStateFlow(initialBackStack)
    val viewState: StateFlow<CustomerSheetViewState> = backStack.mapAsStateFlow { it.last() }

    private val _result = MutableStateFlow<InternalCustomerSheetResult?>(null)
    val result: StateFlow<InternalCustomerSheetResult?> = _result

    private var isGooglePayReadyAndEnabled: Boolean = false
    private var paymentLauncher: PaymentLauncher? = null

    private var unconfirmedPaymentMethod: PaymentMethod? = null
    private var stripeIntent: StripeIntent? = null

    private val card = LpmRepository.hardcodedCardSpec(
        billingDetailsCollectionConfiguration =
        configuration.billingDetailsCollectionConfiguration.toInternal()
    )
    private val usBankAccount = LpmRepository.hardCodedUsBankAccount

    private val supportedPaymentMethods = listOf(
        card,
        usBankAccount
    )

    init {
        lpmRepository.initializeWithPaymentMethods(
            supportedPaymentMethods.associateBy { it.code }
        )

        configuration.appearance.parseAppearance()

        if (viewState.value is CustomerSheetViewState.Loading) {
            viewModelScope.launch {
                loadCustomerSheetState()
            }
        }
    }

    fun handleViewAction(viewAction: CustomerSheetViewAction) {
        when (viewAction) {
            is CustomerSheetViewAction.OnDismissed -> onDismissed()
            is CustomerSheetViewAction.OnAddCardPressed -> onAddCardPressed()
            is CustomerSheetViewAction.OnBackPressed -> onBackPressed()
            is CustomerSheetViewAction.OnEditPressed -> onEditPressed()
            is CustomerSheetViewAction.OnItemRemoved -> onItemRemoved(viewAction.paymentMethod)
            is CustomerSheetViewAction.OnModifyItem -> onModifyItem(viewAction.paymentMethod)
            is CustomerSheetViewAction.OnItemSelected -> onItemSelected(viewAction.selection)
            is CustomerSheetViewAction.OnPrimaryButtonPressed -> onPrimaryButtonPressed()
            is CustomerSheetViewAction.OnAddPaymentMethodItemChanged ->
                onAddPaymentMethodItemChanged(viewAction.paymentMethod)
            is CustomerSheetViewAction.OnFormFieldValuesCompleted -> {
                onFormFieldValuesCompleted(viewAction.formFieldValues)
            }
            is CustomerSheetViewAction.OnUpdateCustomButtonUIState -> {
                updateCustomButtonUIState(viewAction.callback)
            }
            is CustomerSheetViewAction.OnUpdateMandateText -> {
                updateMandateText(viewAction.mandateText)
            }
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
                        primaryButtonEnabled = it.formViewData.completeFormValues != null,
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
                        primaryButtonEnabled = it.formViewData.completeFormValues != null,
                        errorMessage = result.throwable.stripeErrorMessage(application),
                    )
                }
            }
        }
    }

    private suspend fun loadCustomerSheetState() {
        val result = withContext(workContext) {
            customerSheetLoader.load(
                configuration = configuration,
            )
        }

        result.fold(
            onSuccess = { state ->
                savedPaymentSelection = state.paymentSelection
                isGooglePayReadyAndEnabled = state.isGooglePayReady
                stripeIntent = state.stripeIntent

                transitionToInitialScreen(
                    paymentMethods = state.customerPaymentMethods,
                    paymentSelection = state.paymentSelection
                )
            },
            onFailure = { cause ->
                _result.update {
                    InternalCustomerSheetResult.Error(exception = cause)
                }
            }
        )
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
                primaryButtonEnabled = formData.completeFormValues != null && !it.isProcessing,
            )
        }
    }

    private fun onAddPaymentMethodItemChanged(paymentMethod: LpmRepository.SupportedPaymentMethod) {
        updateViewState<CustomerSheetViewState.AddPaymentMethod> {
            it.copy(
                formArguments = FormArgumentsFactory.create(
                    paymentMethod = paymentMethod,
                    configuration = configuration,
                    merchantName = configuration.merchantDisplayName
                        ?: application.applicationInfo.loadLabel(application.packageManager).toString(),
                ),
                selectedPaymentMethod = paymentMethod,
                primaryButtonLabel = if (paymentMethod.code == PaymentMethod.Type.USBankAccount.code) {
                    resolvableString(
                        id = UiCoreR.string.stripe_continue_button_label
                    )
                } else {
                    resolvableString(
                        id = R.string.stripe_paymentsheet_save
                    )
                },
                primaryButtonEnabled = it.formViewData.completeFormValues != null && !it.isProcessing,
            )
        }
    }

    private fun onFormFieldValuesCompleted(formFieldValues: FormFieldValues?) {
        updateViewState<CustomerSheetViewState.AddPaymentMethod> {
            it.copy(
                formViewData = it.formViewData.copy(
                    completeFormValues = formFieldValues,
                ),
                primaryButtonEnabled = formFieldValues != null && !it.isProcessing,
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

    @Suppress("UNUSED_PARAMETER")
    private fun onModifyItem(paymentMethod: PaymentMethod) {
        // TODO(samer-stripe): Link modification to customer sheet
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
                if (currentViewState.customPrimaryButtonUiState != null) {
                    currentViewState.customPrimaryButtonUiState.onClick()
                    return
                }

                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        isProcessing = true,
                        primaryButtonEnabled = false,
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
                            primaryButtonEnabled = it.formViewData.completeFormValues != null,
                            isProcessing = false,
                        )
                    }
                }
        }
    }

    private fun transitionToAddPaymentMethod(isFirstPaymentMethod: Boolean) {
        val paymentMethodCode = PaymentMethod.Type.Card.code

        val formArguments = FormArgumentsFactory.create(
            paymentMethod = card,
            configuration = configuration,
            merchantName = configuration.merchantDisplayName
                ?: application.applicationInfo.loadLabel(application.packageManager).toString(),
        )

        val observe = buildFormObserver(
            formArguments = formArguments,
            formViewModelSubcomponentBuilderProvider = formViewModelSubcomponentBuilderProvider,
            onFormDataUpdated = ::onFormDataUpdated
        )

        val selectedPaymentMethod = requireNotNull(lpmRepository.fromCode(paymentMethodCode))

        transition(
            to = CustomerSheetViewState.AddPaymentMethod(
                paymentMethodCode = paymentMethodCode,
                supportedPaymentMethods = supportedPaymentMethods,
                formViewData = FormViewModel.ViewData(),
                formArguments = formArguments,
                usBankAccountFormArguments = USBankAccountFormArguments(
                    onBehalfOf = null,
                    isCompleteFlow = false,
                    isPaymentFlow = false,
                    stripeIntentId = stripeIntent?.id,
                    clientSecret = stripeIntent?.clientSecret,
                    shippingDetails = null,
                    draftPaymentSelection = null,
                    onMandateTextChanged = { mandate, _ ->
                        handleViewAction(CustomerSheetViewAction.OnUpdateMandateText(mandate))
                    },
                    onHandleUSBankAccount = { },
                    onUpdatePrimaryButtonUIState = {
                        handleViewAction(CustomerSheetViewAction.OnUpdateCustomButtonUIState(it))
                    },
                    onUpdatePrimaryButtonState = { /* no-op, CustomerSheetScreen does not use PrimaryButton.State */ },
                    onError = { }
                ),
                selectedPaymentMethod = selectedPaymentMethod,
                enabled = true,
                isLiveMode = isLiveModeProvider(),
                isProcessing = false,
                isFirstPaymentMethod = isFirstPaymentMethod,
                primaryButtonLabel = resolvableString(
                    id = R.string.stripe_paymentsheet_save
                ),
                primaryButtonEnabled = false,
                customPrimaryButtonUiState = null,
            ),
            reset = isFirstPaymentMethod
        )

        observe()
    }

    private fun updateCustomButtonUIState(callback: (PrimaryButton.UIState?) -> PrimaryButton.UIState?) {
        updateViewState<CustomerSheetViewState.AddPaymentMethod> {
            val uiState = callback(it.customPrimaryButtonUiState)
            if (uiState != null) {
                it.copy(
                    primaryButtonLabel = resolvableString(uiState.label),
                    primaryButtonEnabled = uiState.enabled,
                    customPrimaryButtonUiState = uiState,
                )
            } else {
                it.copy(
                    primaryButtonLabel = it.primaryButtonLabel,
                    primaryButtonEnabled = it.formViewData.completeFormValues != null && !it.isProcessing,
                    customPrimaryButtonUiState = null,
                )
            }
        }
    }

    private fun updateMandateText(mandateText: String?) {
        updateViewState<CustomerSheetViewState.AddPaymentMethod> {
            it.copy(
                mandateText = mandateText
            )
        }
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
                        primaryButtonEnabled = it.formViewData.completeFormValues != null && !it.isProcessing,
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
                        primaryButtonEnabled = it.formViewData.completeFormValues != null,
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
                        primaryButtonEnabled = it.formViewData.completeFormValues != null,
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
                        primaryButtonEnabled = it.formViewData.completeFormValues != null,
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
                        primaryButtonEnabled = it.formViewData.completeFormValues != null,
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

    private fun buildFormObserver(
        formArguments: FormArguments,
        formViewModelSubcomponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>,
        onFormDataUpdated: (data: FormViewModel.ViewData) -> Unit
    ): () -> Unit {
        val formViewModel = formViewModelSubcomponentBuilderProvider.get()
            .formArguments(formArguments)
            .showCheckboxFlow(flowOf(false))
            .build()
            .viewModel

        return {
            viewModelScope.launch {
                formViewModel.viewDataFlow.collect { data ->
                    onFormDataUpdated(data)
                }
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
