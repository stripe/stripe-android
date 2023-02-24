package com.stripe.android.link.ui.paymentmethod

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.NonFallbackInjectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetLinkResult
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkActivityResult.Canceled.Reason.PayAnotherWay
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.R
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.ConfirmStripeIntentParamsFactory
import com.stripe.android.link.confirmation.ConfirmationManager
import com.stripe.android.link.injection.SignedInViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.model.supportedPaymentMethodTypes
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.getErrorMessage
import com.stripe.android.link.ui.wallet.PaymentDetailsResult
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.ui.core.FieldValuesToParamsMapConverter
import com.stripe.android.ui.core.FormController
import com.stripe.android.ui.core.address.toConfirmPaymentIntentShipping
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.forms.convertToFormValuesMap
import com.stripe.android.ui.core.injection.FormControllerSubcomponent
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

/**
 * ViewModel that controls the PaymentMethod screen, managing what payment method form to show and
 * how the user interacts with it to add a new payment method.
 */
internal class PaymentMethodViewModel @Inject constructor(
    val args: LinkActivityContract.Args,
    val linkAccount: LinkAccount,
    private val linkAccountManager: LinkAccountManager,
    private val navigator: Navigator,
    private val confirmationManager: ConfirmationManager,
    private val logger: Logger,
    private val formControllerProvider: Provider<FormControllerSubcomponent.Builder>
) : ViewModel() {
    private val stripeIntent = args.stripeIntent

    private val _primaryButtonState = MutableStateFlow(PrimaryButtonState.Enabled)
    val primaryButtonState: StateFlow<PrimaryButtonState> = _primaryButtonState

    val isEnabled: Flow<Boolean> = _primaryButtonState.map { !it.isBlocking }

    private val _errorMessage = MutableStateFlow<ErrorMessage?>(null)
    val errorMessage: StateFlow<ErrorMessage?> = _errorMessage

    private val isRootScreen = navigator.isOnRootScreen() == true

    val secondaryButtonLabel = if (isRootScreen) {
        R.string.wallet_pay_another_way
    } else {
        R.string.cancel
    }

    val supportedTypes = args.stripeIntent.supportedPaymentMethodTypes(linkAccount)
        .let { supportedTypes ->
            SupportedPaymentMethod.values().filter { supportedTypes.contains(it.type) }
        }

    private val _paymentMethod = MutableStateFlow(supportedTypes.first())
    val paymentMethod: StateFlow<SupportedPaymentMethod> = _paymentMethod

    val formController = MutableStateFlow<FormController?>(null)
    private val formControllersCache = mutableMapOf<SupportedPaymentMethod, FormController>()

    private val _financialConnectionsSessionClientSecret = MutableStateFlow<String?>(null)
    val financialConnectionsSessionClientSecret: StateFlow<String?> =
        _financialConnectionsSessionClientSecret

    // User must be signed in when Wallet Screen is loaded, so [consumerPublishableKey] is not null
    val publishableKey = requireNotNull(linkAccountManager.consumerPublishableKey)

    fun init(loadFromArgs: Boolean) {
        val cardMap = args.prefilledCardParams?.toParamMap()
            ?.takeIf { loadFromArgs }
            ?.let { convertToFormValuesMap(it) }
            ?: emptyMap()
        updateFormController(cardMap)
    }

    fun onPaymentMethodSelected(paymentMethod: SupportedPaymentMethod) {
        _paymentMethod.value = paymentMethod
        updateFormController()
    }

    fun startPayment(formValues: Map<IdentifierSpec, FormFieldEntry>) {
        clearError()
        setState(PrimaryButtonState.Processing)

        when (paymentMethod.value) {
            SupportedPaymentMethod.Card -> {
                val paymentMethodCreateParams =
                    FieldValuesToParamsMapConverter.transformToPaymentMethodCreateParams(
                        formValues,
                        paymentMethod.value.type,
                        false
                    )

                viewModelScope.launch {
                    linkAccountManager.createCardPaymentDetails(
                        paymentMethodCreateParams,
                        linkAccount.email,
                        args.stripeIntent
                    ).fold(
                        onSuccess = ::completePayment,
                        onFailure = ::onError
                    )
                }
            }
            SupportedPaymentMethod.BankAccount -> {
                viewModelScope.launch {
                    linkAccountManager.createFinancialConnectionsSession()
                        .mapCatching { requireNotNull(it.clientSecret) }
                        .fold(
                            onSuccess = {
                                _financialConnectionsSessionClientSecret.value = it
                            },
                            onFailure = ::onError
                        )
                }
            }
        }
    }

    fun onSecondaryButtonClick() {
        if (isRootScreen) {
            payAnotherWay()
        } else {
            navigator.onBack(userInitiated = true)
        }
    }

    fun onFinancialConnectionsAccountLinked(result: FinancialConnectionsSheetLinkResult) {
        when (result) {
            is FinancialConnectionsSheetLinkResult.Canceled -> setState(PrimaryButtonState.Enabled)
            is FinancialConnectionsSheetLinkResult.Failed -> onError(result.error)
            is FinancialConnectionsSheetLinkResult.Completed -> {
                viewModelScope.launch {
                    linkAccountManager.createBankAccountPaymentDetails(result.linkedAccountId)
                        .fold(
                            onSuccess = ::navigateToWallet,
                            onFailure = ::onError
                        )
                }
            }
        }
    }

    private fun updateFormController(
        initialValues: Map<IdentifierSpec, String?> = emptyMap()
    ) {
        formController.value =
            formControllersCache[paymentMethod.value] ?: formControllerProvider.get()
                .formSpec(LayoutSpec(paymentMethod.value.formSpec))
                .viewOnlyFields(emptySet())
                .viewModelScope(viewModelScope)
                .initialValues(initialValues)
                .stripeIntent(args.stripeIntent)
                .merchantName(args.merchantName)
                .shippingValues(args.shippingValues)
                .build()
                .formController
                .also { formControllersCache[paymentMethod.value] = it }
    }

    private fun navigateToWallet(selectedAccount: ConsumerPaymentDetails.BankAccount) {
        if (navigator.isOnRootScreen() == false) {
            navigator.setResult(
                PaymentDetailsResult.KEY,
                PaymentDetailsResult.Success(selectedAccount.id)
            )
            navigator.onBack(userInitiated = false)
        } else {
            navigator.navigateTo(LinkScreen.Wallet, clearBackStack = true)
        }
    }

    private fun payAnotherWay() {
        clearError()
        navigator.cancel(reason = PayAnotherWay)
    }

    private fun completePayment(linkPaymentDetails: LinkPaymentDetails) {
        val params = ConfirmStripeIntentParamsFactory.createFactory(
            stripeIntent,
            args.shippingValues?.toConfirmPaymentIntentShipping()
        )
            .createConfirmStripeIntentParams(linkPaymentDetails.paymentMethodCreateParams)

        confirmationManager.confirmStripeIntent(params) { result ->
            result.fold(
                onSuccess = { paymentResult ->
                    when (paymentResult) {
                        is PaymentResult.Canceled -> {
                            // no-op, let the user continue their flow
                            setState(PrimaryButtonState.Enabled)
                        }
                        is PaymentResult.Failed -> {
                            onError(paymentResult.throwable)
                        }
                        is PaymentResult.Completed -> {
                            setState(PrimaryButtonState.Completed)
                            viewModelScope.launch {
                                delay(PrimaryButtonState.COMPLETED_DELAY_MS)
                                navigator.dismiss(LinkActivityResult.Completed)
                            }
                        }
                    }
                },
                onFailure = ::onError
            )
        }
    }

    private fun clearError() {
        _errorMessage.value = null
    }

    private fun onError(error: Throwable) = error.getErrorMessage().let {
        logger.error("Error: ", error)
        setState(PrimaryButtonState.Enabled)
        _errorMessage.value = it
    }

    private fun setState(state: PrimaryButtonState) {
        _primaryButtonState.value = state
        navigator.userNavigationEnabled = !state.isBlocking
    }

    internal class Factory(
        private val linkAccount: LinkAccount,
        private val injector: NonFallbackInjector,
        private val loadFromArgs: Boolean
    ) : ViewModelProvider.Factory, NonFallbackInjectable {

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<SignedInViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
                .linkAccount(linkAccount)
                .build().paymentMethodViewModel.apply {
                    init(loadFromArgs)
                } as T
        }
    }
}
