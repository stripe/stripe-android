package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.app.Application
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.ConfirmStripeIntentParamsFactory
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.model.Address
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toConfirmPaymentIntentShipping
import com.stripe.android.paymentsheet.addresselement.toIdentifierMap
import com.stripe.android.paymentsheet.model.ClientSecret
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.paymentsheet.model.create
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.di.DaggerUSBankAccountFormComponent
import com.stripe.android.paymentsheet.paymentdatacollection.ach.di.USBankAccountFormViewModelSubcomponent
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.utils.requireApplication
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
    private val lazyPaymentConfig: Provider<PaymentConfiguration>,
    private val savedStateHandle: SavedStateHandle,
    addressRepository: AddressRepository,
) : ViewModel() {
    private val defaultBillingDetails = args.formArgs.billingDetails
    private val collectionConfiguration = args.formArgs.billingDetailsCollectionConfiguration

    private val defaultName = if (
        collectionConfiguration.name != CollectionMode.Never ||
        collectionConfiguration.attachDefaultsToPaymentMethod
    ) {
        defaultBillingDetails?.name
    } else {
        null
    }

    val nameController: TextFieldController =
        NameConfig.createController(defaultName ?: "")

    val name: StateFlow<String> = nameController.formFieldValue.map { formFieldEntry ->
        formFieldEntry.takeIf { it.isComplete }?.value ?: ""
    }.stateIn(viewModelScope, SharingStarted.Eagerly, defaultName ?: "")

    private val defaultEmail = if (
        collectionConfiguration.email != CollectionMode.Never ||
        collectionConfiguration.attachDefaultsToPaymentMethod
    ) {
        defaultBillingDetails?.email
    } else {
        null
    }

    val emailController: TextFieldController = EmailConfig.createController(defaultEmail)

    val email: StateFlow<String?> = emailController.formFieldValue.map { formFieldEntry ->
        formFieldEntry.takeIf { it.isComplete }?.value
    }.stateIn(viewModelScope, SharingStarted.Eagerly, defaultEmail)

    private val defaultPhoneCountry = if (
        collectionConfiguration.phone == CollectionMode.Always ||
        collectionConfiguration.attachDefaultsToPaymentMethod
    ) {
        defaultBillingDetails?.address?.country
    } else {
        null
    }

    private val defaultPhone = if (
        collectionConfiguration.phone == CollectionMode.Always ||
        collectionConfiguration.attachDefaultsToPaymentMethod
    ) {
        defaultBillingDetails?.phone
    } else {
        null
    }

    val phoneController = PhoneNumberController(
        initiallySelectedCountryCode = defaultPhoneCountry,
        initialPhoneNumber = defaultPhone ?: "",
    )

    val phone: StateFlow<String?> = phoneController.formFieldValue.map { formFieldEntry ->
        formFieldEntry.takeIf { it.isComplete }?.value
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val defaultAddress = if (
        collectionConfiguration.address == AddressCollectionMode.Full ||
        collectionConfiguration.attachDefaultsToPaymentMethod
    ) {
        defaultBillingDetails?.address?.asAddressModel()
    } else {
        null
    }

    val sameAsShippingElement = args.formArgs.shippingDetails
        ?.toIdentifierMap(defaultBillingDetails)
        ?.get(IdentifierSpec.SameAsShipping)
        ?.toBooleanStrictOrNull()
        ?.let {
            SameAsShippingElement(
                identifier = IdentifierSpec.SameAsShipping,
                controller = SameAsShippingController(it)
            )
        }

    val addressElement = AddressElement(
        _identifier = IdentifierSpec.Generic("billing_details[address]"),
        addressRepository = addressRepository,
        rawValuesMap = defaultAddress?.asFormFieldValues() ?: emptyMap(),
        sameAsShippingElement = sameAsShippingElement,
        shippingValuesMap = args.formArgs.shippingDetails
            ?.toIdentifierMap(args.formArgs.billingDetails),
    )

    // AddressElement generates a default address if the initial value is null, so we can't rely
    // on the value produced by the controller in that case.
    val address = if (defaultAddress == null) {
        MutableStateFlow(null)
    } else {
        addressElement.getFormFieldValueFlow().map { formFieldValues ->
            val rawMap = formFieldValues.associate { it.first to it.second.value }
            Address.fromFormFieldValues(rawMap)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }

    val lastTextFieldIdentifier = addressElement.getTextFieldIdentifiers().map {
        it.last()
    }

    private val _currentScreenState: MutableStateFlow<USBankAccountFormScreenState> =
        MutableStateFlow(
            USBankAccountFormScreenState.BillingDetailsCollection(
                name = name.value,
                email = email.value,
                phone = phone.value,
                address = address.value,
                primaryButtonText = application.getString(
                    R.string.stripe_continue_button_label
                ),
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
        nameController.formFieldValue.map { it.isComplete },
        emailController.formFieldValue.map { it.isComplete },
        phoneController.formFieldValue.map { it.isComplete },
        addressElement.getFormFieldValueFlow().map { formFieldValues ->
            formFieldValues.all { it.second.isComplete }
        }
    ) { validName, validEmail, validPhone, validAddress ->
        validName && validEmail &&
            (validPhone || collectionConfiguration.phone != CollectionMode.Always) &&
            (validAddress || collectionConfiguration.address != AddressCollectionMode.Full)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = false
    )

    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean>
        get() = _processing

    @VisibleForTesting
    var collectBankAccountLauncher: CollectBankAccountLauncher? = null

    init {
        args.savedPaymentMethod?.paymentMethodCreateParams?.let {
            _currentScreenState.update {
                USBankAccountFormScreenState.SavedAccount(
                    name.value,
                    email.value,
                    phone.value,
                    address.value,
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
                                    phone = phone.value,
                                    address = address.value,
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
                                    phone = phone.value,
                                    address = address.value,
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
            name.value,
            email.value,
            phone.value,
            address.value,
            saveForFutureUse.value,
        )
        when (screenState) {
            is USBankAccountFormScreenState.BillingDetailsCollection -> {
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
        }
    }

    fun reset(@StringRes error: Int? = null) {
        hasLaunched = false
        saveForFutureUseElement.controller.onValueChange(true)
        _currentScreenState.update {
            USBankAccountFormScreenState.BillingDetailsCollection(
                error = error,
                name = name.value,
                email = email.value,
                phone = phone.value,
                address = address.value,
                primaryButtonText = application.getString(
                    R.string.stripe_continue_button_label
                ),
            )
        }
    }

    fun onDestroy() {
        // Save before we die
        _currentScreenState.update {
            it.updateInputs(
                name.value,
                email.value,
                phone.value,
                address.value,
                saveForFutureUse.value
            )
        }

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
                    publishableKey = lazyPaymentConfig.get().publishableKey,
                    stripeAccountId = lazyPaymentConfig.get().stripeAccountId,
                    clientSecret = clientSecret.value,
                    configuration = CollectBankAccountConfiguration.USBankAccount(
                        name.value,
                        email.value
                    )
                )
            }
            is SetupIntentClientSecret -> {
                collectBankAccountLauncher?.presentWithSetupIntent(
                    publishableKey = lazyPaymentConfig.get().publishableKey,
                    stripeAccountId = lazyPaymentConfig.get().stripeAccountId,
                    clientSecret = clientSecret.value,
                    configuration = CollectBankAccountConfiguration.USBankAccount(
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
                                ),
                                expandFields = emptyList()
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
                                ),
                                expandFields = emptyList()
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
                                email = email.value,
                                phone = phone.value,
                                address = address.value,
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

                    if (args.isCompleteFlow) {
                        confirm(clientSecret, paymentSelection)
                    } else {
                        _currentScreenState.update { screenState ->
                            if (screenState is USBankAccountFormScreenState.SavedAccount) {
                                screenState.copy(
                                    bankName = bankName,
                                    last4 = last4
                                )
                            } else {
                                screenState
                            }
                        }
                        args.onUpdateSelectionAndFinish(paymentSelection)
                    }
                }
            }
        }
    }

    private fun confirm(clientSecret: ClientSecret, paymentSelection: PaymentSelection.New) {
        viewModelScope.launch {
            val confirmParamsFactory = ConfirmStripeIntentParamsFactory.createFactory(
                clientSecret = clientSecret.value,
                shipping = args.shippingDetails?.toConfirmPaymentIntentShipping()
            )
            val confirmIntent = confirmParamsFactory.create(paymentSelection)
            args.onConfirmStripeIntent(confirmIntent)
        }
    }

    private fun buildPrimaryButtonText(): String {
        return when {
            args.isCompleteFlow -> {
                if (args.clientSecret is PaymentIntentClientSecret) {
                    args.formArgs.amount!!.buildPayButtonLabel(application.resources)
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
        private val argsSupplier: () -> Args,
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam> {

        internal data class FallbackInitializeParam(val application: Application)

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<USBankAccountFormViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val args = argsSupplier()

            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()

            injectWithFallback(args.injectorKey, FallbackInitializeParam(application))

            return subComponentBuilderProvider.get()
                .configuration(args)
                .savedStateHandle(savedStateHandle)
                .build().viewModel as T
        }

        override fun fallbackInitialize(arg: FallbackInitializeParam): Injector? {
            DaggerUSBankAccountFormComponent
                .builder()
                .application(arg.application)
                .injectorKey(DUMMY_INJECTOR_KEY)
                .build().inject(this)
            return null
        }
    }

    /**
     * Arguments for launching [USBankAccountFormFragment]
     *
     * @param formArgs The form arguments supplied by the payment sheet
     * @param isCompleteFlow Whether the payment should be completed, or the selected payment
     *                          method should be returned as a result
     * @param clientSecret The client secret for the Stripe Intent being processed
     */
    data class Args(
        val formArgs: FormArguments,
        val isCompleteFlow: Boolean,
        val clientSecret: ClientSecret?,
        val savedPaymentMethod: PaymentSelection.New.USBankAccount?,
        val shippingDetails: AddressDetails?,
        val onConfirmStripeIntent: (ConfirmStripeIntentParams) -> Unit,
        val onUpdateSelectionAndFinish: (PaymentSelection) -> Unit,
        @InjectorKey internal val injectorKey: String = DUMMY_INJECTOR_KEY
    )

    private companion object {
        private const val HAS_LAUNCHED_KEY = "has_launched"
    }
}

internal fun Address.asFormFieldValues(): Map<IdentifierSpec, String?> = mapOf(
    IdentifierSpec.Line1 to line1,
    IdentifierSpec.Line2 to line2,
    IdentifierSpec.City to city,
    IdentifierSpec.State to state,
    IdentifierSpec.Country to country,
    IdentifierSpec.PostalCode to postalCode,
)

internal fun Address.Companion.fromFormFieldValues(formFieldValues: Map<IdentifierSpec, String?>) =
    Address(
        line1 = formFieldValues[IdentifierSpec.Line1],
        line2 = formFieldValues[IdentifierSpec.Line2],
        city = formFieldValues[IdentifierSpec.City],
        state = formFieldValues[IdentifierSpec.State],
        country = formFieldValues[IdentifierSpec.Country],
        postalCode = formFieldValues[IdentifierSpec.PostalCode],
    )

internal fun PaymentSheet.Address.asAddressModel() =
    Address(
        line1 = line1,
        line2 = line2,
        city = city,
        state = state,
        country = country,
        postalCode = postalCode,
    )
