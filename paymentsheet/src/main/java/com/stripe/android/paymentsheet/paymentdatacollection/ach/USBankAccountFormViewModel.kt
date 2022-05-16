package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.app.Application
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.ConfirmStripeIntentParamsFactory
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.di.DaggerUSBankAccountFormComponent
import com.stripe.android.paymentsheet.paymentdatacollection.ach.di.USBankAccountFormViewModelSubcomponent
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SectionElement
import dagger.Lazy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal class USBankAccountFormViewModel @Inject internal constructor(
    private val args: Args,
    private val application: Application,
    private val stripeRepository: StripeRepository,
    private val lazyPaymentConfig: Lazy<PaymentConfiguration>,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val nameElement: SectionElement = NameSpec().transform(
        mapOf(
            IdentifierSpec.Name to args.formArgs.billingDetails?.name
        )
    )
    val name: StateFlow<String> = nameElement.getFormFieldValueFlow().map { formFieldsList ->
        formFieldsList.firstOrNull()?.second?.takeIf { it.isComplete }?.value ?: ""
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val emailElement: SectionElement = EmailSpec().transform(
        mapOf(
            IdentifierSpec.Email to args.formArgs.billingDetails?.email
        )
    )
    val email: StateFlow<String?> = emailElement.getFormFieldValueFlow().map { formFieldsList ->
        formFieldsList.firstOrNull()?.second?.takeIf { it.isComplete }?.value
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _currentScreenState: MutableStateFlow<USBankAccountFormScreenState> =
        MutableStateFlow(
            args.savedScreenState ?: USBankAccountFormScreenState.NameAndEmailCollection(
                name = name.value,
                email = email.value,
                primaryButtonText = application.getString(
                    R.string.stripe_continue_button_label
                )
            )
        )
    val currentScreenState: StateFlow<USBankAccountFormScreenState>
        get() = _currentScreenState

    val saveForFutureUseElement: SaveForFutureUseElement = SaveForFutureUseSpec().transform(
        initialValue = args.formArgs.showCheckbox,
        merchantName = args.formArgs.merchantName
    ) as SaveForFutureUseElement
    val saveForFutureUse: StateFlow<Boolean> = saveForFutureUseElement.controller.saveForFutureUse
        .stateIn(viewModelScope, SharingStarted.Lazily, args.formArgs.showCheckbox)

    val requiredFields = combine(
        nameElement.getFormFieldValueFlow().map { formFieldsList ->
            formFieldsList.firstOrNull()?.second?.value?.isNotBlank() ?: false
        },
        emailElement.getFormFieldValueFlow().map { formFieldsList ->
            formFieldsList.firstOrNull()?.second?.isComplete ?: false
        }
    ) { validName, validEmail ->
        validName && validEmail
    }

    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean>
        get() = _processing

    @VisibleForTesting
    var collectBankAccountLauncher: CollectBankAccountLauncher? = null

    init {
        args.savedPaymentMethod?.paymentMethodCreateParams?.let { params ->
            _currentScreenState.update {
                USBankAccountFormScreenState.SavedAccount(
                    params.billingDetails?.name ?: name.value,
                    params.billingDetails?.email ?: email.value,
                    args.savedPaymentMethod.financialConnectionsSessionId,
                    args.savedPaymentMethod.intentId,
                    args.savedPaymentMethod.bankName,
                    args.savedPaymentMethod.last4,
                    buildPrimaryButtonText(),
                    buildMandateText(),
                    args.formArgs.showCheckbox
                )
            }
        }
    }

    private var hasLaunched: Boolean
        get() = savedStateHandle.get<Boolean>(HAS_LAUNCHED_KEY) == true
        set(value) = savedStateHandle.set(HAS_LAUNCHED_KEY, value)

    fun registerFragment(fragment: Fragment) {
        collectBankAccountLauncher = CollectBankAccountLauncher.create(
            fragment,
            ::handleCollectBankAccountResult
        )
    }

    @VisibleForTesting
    fun handleCollectBankAccountResult(result: CollectBankAccountResult) {
        hasLaunched = false
        when (result) {
            is CollectBankAccountResult.Completed -> {
                when (
                    val paymentAccount =
                        result.response.financialConnectionsSession.paymentAccount
                ) {
                    is BankAccount -> {
                        result.response.intent.id?.let { intentId ->
                            _currentScreenState.update {
                                USBankAccountFormScreenState.VerifyWithMicrodeposits(
                                    name = name.value,
                                    email = email.value,
                                    paymentAccount = paymentAccount,
                                    financialConnectionsSessionId =
                                    result.response.financialConnectionsSession.id,
                                    intentId = intentId,
                                    primaryButtonText = buildPrimaryButtonText(),
                                    mandateText = buildMandateText(),
                                    saveForFutureUsage = saveForFutureUse.value
                                )
                            }
                        }
                    }
                    is FinancialConnectionsAccount -> {
                        result.response.intent.id?.let { intentId ->
                            _currentScreenState.update {
                                USBankAccountFormScreenState.MandateCollection(
                                    name = name.value,
                                    email = email.value,
                                    paymentAccount = paymentAccount,
                                    financialConnectionsSessionId =
                                    result.response.financialConnectionsSession.id,
                                    intentId = intentId,
                                    primaryButtonText = buildPrimaryButtonText(),
                                    mandateText = buildMandateText(),
                                    saveForFutureUsage = saveForFutureUse.value
                                )
                            }
                        }
                    }
                    null -> {
                        reset(R.string.stripe_paymentsheet_ach_something_went_wrong)
                    }
                }
            }
            is CollectBankAccountResult.Failed -> {
                reset(R.string.stripe_paymentsheet_ach_something_went_wrong)
            }
            is CollectBankAccountResult.Cancelled -> {
                reset()
            }
        }
    }

    fun handlePrimaryButtonClick(screenState: USBankAccountFormScreenState) {
        _currentScreenState.value = _currentScreenState.value.updateInputs(
            name.value, email.value, saveForFutureUse.value
        )
        when (screenState) {
            is USBankAccountFormScreenState.NameAndEmailCollection -> {
                args.clientSecret?.let {
                    collectBankAccount(it)
                }
            }
            is USBankAccountFormScreenState.MandateCollection ->
                args.clientSecret?.let {
                    attachFinancialAccountToIntent(
                        clientSecret = it,
                        intentId = screenState.intentId,
                        linkAccountId = screenState.financialConnectionsSessionId,
                        bankName = screenState.paymentAccount.institutionName,
                        last4 = screenState.paymentAccount.last4
                    )
                }
            is USBankAccountFormScreenState.VerifyWithMicrodeposits ->
                args.clientSecret?.let {
                    attachFinancialAccountToIntent(
                        clientSecret = it,
                        intentId = screenState.intentId,
                        linkAccountId = screenState.financialConnectionsSessionId,
                        bankName = screenState.paymentAccount.bankName,
                        last4 = screenState.paymentAccount.last4
                    )
                }
            is USBankAccountFormScreenState.SavedAccount -> {
                args.clientSecret?.let {
                    screenState.financialConnectionsSessionId?.let { linkAccountId ->
                        attachFinancialAccountToIntent(
                            clientSecret = it,
                            intentId = screenState.intentId,
                            linkAccountId = linkAccountId,
                            bankName = screenState.bankName,
                            last4 = screenState.last4
                        )
                    }
                }
            }
            else -> { /* no op */ }
        }
    }

    fun generateSavedState(
        screenState: USBankAccountFormScreenState
    ): USBankAccountFormScreenState? {
        return when (screenState) {
            is USBankAccountFormScreenState.Finished -> {
                USBankAccountFormScreenState.SavedAccount(
                    name = name.value,
                    email = email.value,
                    bankName = screenState.bankName,
                    last4 = screenState.last4,
                    financialConnectionsSessionId = screenState.financialConnectionsSessionId,
                    intentId = screenState.intentId,
                    primaryButtonText = buildPrimaryButtonText(),
                    mandateText = buildMandateText(),
                    saveForFutureUsage = saveForFutureUse.value
                )
            }
            else -> null
        }
    }

    fun reset(@StringRes error: Int? = null) {
        hasLaunched = false
        saveForFutureUseElement.controller.onValueChange(true)
        _currentScreenState.update {
            USBankAccountFormScreenState.NameAndEmailCollection(
                error = error,
                name = name.value,
                email = email.value,
                primaryButtonText = application.getString(
                    R.string.stripe_continue_button_label
                )
            )
        }
    }

    fun onDestroy() {
        collectBankAccountLauncher = null
    }

    fun formattedMerchantName(): String {
        return args.formArgs.merchantName.trimEnd { it == '.' }
    }

    fun setProcessing(enabled: Boolean) {
        _processing.update { enabled }
    }

    private fun collectBankAccount(clientSecret: ClientSecret) {
        if (hasLaunched) return
        hasLaunched = true
        when (clientSecret) {
            is PaymentIntentClientSecret -> {
                collectBankAccountLauncher?.presentWithPaymentIntent(
                    lazyPaymentConfig.get().publishableKey,
                    clientSecret.value,
                    CollectBankAccountConfiguration.USBankAccount(
                        name.value,
                        email.value
                    )
                )
            }
            is SetupIntentClientSecret -> {
                collectBankAccountLauncher?.presentWithSetupIntent(
                    lazyPaymentConfig.get().publishableKey,
                    clientSecret.value,
                    CollectBankAccountConfiguration.USBankAccount(
                        name.value,
                        email.value
                    )
                )
            }
        }
    }

    private fun attachFinancialAccountToIntent(
        clientSecret: ClientSecret,
        intentId: String,
        linkAccountId: String,
        bankName: String?,
        last4: String?
    ) {
        bankName?.let {
            last4?.let {
                viewModelScope.launch {
                    when (clientSecret) {
                        is PaymentIntentClientSecret -> {
                            stripeRepository.attachFinancialConnectionsSessionToPaymentIntent(
                                clientSecret.value,
                                intentId,
                                linkAccountId,
                                ApiRequest.Options(
                                    apiKey = lazyPaymentConfig.get().publishableKey,
                                    stripeAccount = lazyPaymentConfig.get().stripeAccountId
                                )
                            )
                        }
                        is SetupIntentClientSecret -> {
                            stripeRepository.attachFinancialConnectionsSessionToSetupIntent(
                                clientSecret.value,
                                intentId,
                                linkAccountId,
                                ApiRequest.Options(
                                    apiKey = lazyPaymentConfig.get().publishableKey,
                                    stripeAccount = lazyPaymentConfig.get().stripeAccountId
                                )
                            )
                        }
                    }

                    val paymentSelection = PaymentSelection.New.USBankAccount(
                        labelResource = application.getString(
                            R.string.paymentsheet_payment_method_item_card_number,
                            last4
                        ),
                        iconResource = TransformToBankIcon(
                            bankName
                        ),
                        paymentMethodCreateParams =
                        PaymentMethodCreateParams.create(
                            usBankAccount = PaymentMethodCreateParams.USBankAccount(
                                linkAccountSessionId = linkAccountId
                            ),
                            billingDetails = PaymentMethod.BillingDetails(
                                name = name.value,
                                email = email.value
                            )
                        ),
                        customerRequestedSave = if (args.formArgs.showCheckbox) {
                            if (saveForFutureUse.value) {
                                PaymentSelection.CustomerRequestedSave.RequestReuse
                            } else {
                                PaymentSelection.CustomerRequestedSave.RequestNoReuse
                            }
                        } else {
                            PaymentSelection.CustomerRequestedSave.NoRequest
                        },
                        bankName = bankName,
                        last4 = last4,
                        financialConnectionsSessionId = linkAccountId,
                        intentId = intentId
                    )

                    if (args.completePayment) {
                        confirm(clientSecret, paymentSelection)
                    } else {
                        _currentScreenState.update {
                            USBankAccountFormScreenState.Finished(
                                paymentSelection,
                                linkAccountId,
                                intentId,
                                bankName,
                                last4
                            )
                        }
                    }
                }
            }
        }
    }

    private fun confirm(clientSecret: ClientSecret, paymentSelection: PaymentSelection.New) {
        viewModelScope.launch {
            val confirmParamsFactory = ConfirmStripeIntentParamsFactory.createFactory(
                clientSecret
            )
            val confirmIntent = confirmParamsFactory.create(paymentSelection)
            _currentScreenState.update {
                USBankAccountFormScreenState.ConfirmIntent(confirmIntent)
            }
        }
    }

    private fun buildPrimaryButtonText(): String? {
        return when {
            args.completePayment -> {
                if (args.clientSecret is PaymentIntentClientSecret) {
                    args.formArgs.amount?.buildPayButtonLabel(application.resources)
                } else {
                    application.getString(
                        R.string.stripe_setup_button_label
                    )
                }
            }
            else -> application.getString(
                R.string.stripe_continue_button_label
            )
        }
    }

    private fun buildMandateText(): String {
        return if (saveForFutureUse.value) {
            application.getString(
                R.string.stripe_paymentsheet_ach_save_mandate,
                formattedMerchantName()
            )
        } else {
            ACHText.getContinueMandateText(application)
        }
    }

    internal class Factory(
        private val applicationSupplier: () -> Application,
        private val argsSupplier: () -> Args,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs),
        Injectable<Factory.FallbackInitializeParam> {
        internal data class FallbackInitializeParam(
            val application: Application,
        )

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<USBankAccountFormViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            savedStateHandle: SavedStateHandle
        ): T {
            val args = argsSupplier()

            injectWithFallback(args.injectorKey, FallbackInitializeParam(applicationSupplier()))

            return subComponentBuilderProvider.get()
                .configuration(args)
                .savedStateHandle(savedStateHandle)
                .build().viewModel as T
        }

        override fun fallbackInitialize(arg: FallbackInitializeParam) {
            DaggerUSBankAccountFormComponent
                .builder()
                .application(arg.application)
                .injectorKey(DUMMY_INJECTOR_KEY)
                .build().inject(this)
        }
    }

    /**
     * Arguments for launching [USBankAccountFormFragment]
     *
     * @param formArgs The form arguments supplied by the payment sheet
     * @param completePayment Whether the payment should be completed, or the selected payment
     *                          method should be returned as a result
     * @param clientSecret The client secret for the Stripe Intent being processed
     */
    data class Args(
        val formArgs: FormFragmentArguments,
        val completePayment: Boolean,
        val clientSecret: ClientSecret?,
        val savedScreenState: USBankAccountFormScreenState?,
        val savedPaymentMethod: PaymentSelection.New.USBankAccount?,
        @InjectorKey internal val injectorKey: String = DUMMY_INJECTOR_KEY
    )

    private companion object {
        private const val HAS_LAUNCHED_KEY = "has_launched"
    }
}
