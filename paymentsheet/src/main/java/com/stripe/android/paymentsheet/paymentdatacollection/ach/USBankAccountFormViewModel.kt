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
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
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
    private val _currentScreenState: MutableStateFlow<USBankAccountFormScreenState> =
        MutableStateFlow(
            USBankAccountFormScreenState.NameAndEmailCollection(
                primaryButtonText = application.getString(
                    R.string.us_bank_account_payment_sheet_primary_button_continue
                ),
                primaryButtonOnClick = {
                    args.clientSecret?.let {
                        collectBankAccount(it)
                    }
                }
            )
        )
    val currentScreenState: StateFlow<USBankAccountFormScreenState>
        get() = _currentScreenState

    val nameElement: SectionElement = NameSpec().transform(emptyMap())
    val name: StateFlow<String> = nameElement.getFormFieldValueFlow().map { formFieldsList ->
        formFieldsList.firstOrNull()?.second?.value ?: ""
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    val emailElement: SectionElement = EmailSpec().transform(
        mapOf(
            IdentifierSpec.Email to null
        )
    )
    val email: StateFlow<String?> = emailElement.getFormFieldValueFlow().map { formFieldsList ->
        formFieldsList.firstOrNull()?.second?.takeIf { it.isComplete }?.value
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val saveForFutureUseElement: SaveForFutureUseElement = SaveForFutureUseSpec().transform(
        initialValue = args.formArgs.showCheckbox,
        merchantName = args.formArgs.merchantName
    ) as SaveForFutureUseElement
    val saveForFutureUse: StateFlow<Boolean> = saveForFutureUseElement.controller.saveForFutureUse
        .stateIn(viewModelScope, SharingStarted.Lazily, args.formArgs.showCheckbox)

    val requiredFields = name
        .map { it.isNotBlank() }
        .combine(
            emailElement.getFormFieldValueFlow().map { formFieldsList ->
                formFieldsList.firstOrNull()?.second?.isComplete ?: false
            }
        ) { validName, validEmail ->
            validName && validEmail
        }

    @VisibleForTesting
    var collectBankAccountLauncher: CollectBankAccountLauncher? = null

    private var hasLaunched: Boolean
        get() = savedStateHandle.get<Boolean>(HAS_LAUNCHED_KEY) == true
        set(value) = savedStateHandle.set(HAS_LAUNCHED_KEY, value)
    private var bankName: String?
        get() = savedStateHandle[BANK_NAME_KEY]
        set(value) = savedStateHandle.set(BANK_NAME_KEY, value)
    private var last4: String?
        get() = savedStateHandle[LAST4_KEY]
        set(value) = savedStateHandle.set(LAST4_KEY, value)

    fun registerFragment(fragment: Fragment) {
        collectBankAccountLauncher = CollectBankAccountLauncher.create(
            fragment,
            ::handleCollectBankAccountResult
        )
    }

    @VisibleForTesting
    fun handleCollectBankAccountResult(result: CollectBankAccountResult) {
        when (result) {
            is CollectBankAccountResult.Completed -> {
                when (
                    val paymentAccount =
                        result.response.financialConnectionsSession.paymentAccount
                ) {
                    is BankAccount -> {
                        result.response.intent.id?.let { intentId ->
                            bankName = paymentAccount.bankName
                            last4 = paymentAccount.last4
                            _currentScreenState.update {
                                USBankAccountFormScreenState.VerifyWithMicrodeposits(
                                    bankName = paymentAccount.bankName,
                                    displayName = paymentAccount.bankName,
                                    last4 = paymentAccount.last4,
                                    primaryButtonText = buildPrimaryButtonText(),
                                    primaryButtonOnClick = {
                                        args.clientSecret?.let {
                                            attach(
                                                clientSecret = it,
                                                intentId = intentId,
                                                linkAccountId =
                                                result.response.financialConnectionsSession.id
                                            )
                                        }
                                    },
                                    mandateText = buildMandateText()
                                )
                            }
                        }
                    }
                    is FinancialConnectionsAccount -> {
                        result.response.intent.id?.let { intentId ->
                            bankName = paymentAccount.institutionName
                            last4 = paymentAccount.last4
                            _currentScreenState.update {
                                USBankAccountFormScreenState.MandateCollection(
                                    bankName = paymentAccount.institutionName,
                                    displayName = paymentAccount.displayName,
                                    last4 = paymentAccount.last4,
                                    primaryButtonText = buildPrimaryButtonText(),
                                    primaryButtonOnClick = {
                                        args.clientSecret?.let {
                                            attach(
                                                clientSecret = it,
                                                intentId = intentId,
                                                linkAccountId =
                                                result.response.financialConnectionsSession.id
                                            )
                                        }
                                    },
                                    mandateText = buildMandateText()
                                )
                            }
                        }
                    }
                    null -> {
                        reset(R.string.us_bank_account_payment_sheet_something_went_wrong)
                    }
                }
            }
            is CollectBankAccountResult.Failed -> {
                reset(R.string.us_bank_account_payment_sheet_something_went_wrong)
            }
            is CollectBankAccountResult.Cancelled -> {
                reset()
            }
        }
    }

    fun reset(@StringRes error: Int? = null) {
        hasLaunched = false
        _currentScreenState.update {
            USBankAccountFormScreenState.NameAndEmailCollection(
                error = error,
                primaryButtonText = application.getString(
                    R.string.us_bank_account_payment_sheet_primary_button_continue
                ),
                primaryButtonOnClick = {
                    args.clientSecret?.let {
                        collectBankAccount(it)
                    }
                }
            )
        }
    }

    fun onDestroy() {
        collectBankAccountLauncher = null
    }

    fun formattedMerchantName(): String {
        return args.formArgs.merchantName.trimEnd { it == '.' }
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

    private fun attach(
        clientSecret: ClientSecret,
        intentId: String,
        linkAccountId: String
    ) {
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
            if (args.completePayment) {
                confirm(clientSecret)
            } else {
                last4?.let { last4 ->
                    bankName?.let { bankName ->
                        _currentScreenState.update {
                            USBankAccountFormScreenState.Finished(linkAccountId, last4, bankName)
                        }
                    }
                }
            }
        }
    }

    private fun confirm(clientSecret: ClientSecret) {
        viewModelScope.launch {
            val intent = when (clientSecret) {
                is PaymentIntentClientSecret -> {
                    ConfirmPaymentIntentParams.create(
                        clientSecret = clientSecret.value,
                        paymentMethodType = PaymentMethod.Type.USBankAccount
                    ).apply {
                        paymentMethodOptions = PaymentMethodOptionsParams.USBankAccount(
                            setupFutureUsage = if (saveForFutureUse.value) {
                                ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                            } else {
                                null
                            }
                        )
                    }
                }
                is SetupIntentClientSecret -> {
                    ConfirmSetupIntentParams.create(
                        clientSecret = clientSecret.value,
                        paymentMethodType = PaymentMethod.Type.USBankAccount
                    )
                }
            }
            _currentScreenState.update {
                USBankAccountFormScreenState.ConfirmIntent(intent)
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
                R.string.us_bank_account_payment_sheet_primary_button_continue
            )
        }
    }

    private fun buildMandateText(): String {
        return if (saveForFutureUse.value) {
            application.getString(
                R.string.us_bank_account_payment_sheet_mandate_save,
                formattedMerchantName()
            )
        } else {
            application.getString(
                R.string.us_bank_account_payment_sheet_mandate_continue
            )
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
        @InjectorKey internal val injectorKey: String = DUMMY_INJECTOR_KEY
    )

    private companion object {
        private const val HAS_LAUNCHED_KEY = "has_launched"
        private const val BANK_NAME_KEY = "bank_name"
        private const val LAST4_KEY = "last4"
    }
}
