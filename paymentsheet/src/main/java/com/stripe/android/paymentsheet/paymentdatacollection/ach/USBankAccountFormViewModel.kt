package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.app.Application
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toIdentifierMap
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.di.DaggerUSBankAccountFormComponent
import com.stripe.android.paymentsheet.paymentdatacollection.ach.di.USBankAccountFormViewModelSubcomponent
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import com.stripe.android.ui.core.R as StripeUiCoreR

internal class USBankAccountFormViewModel @Inject internal constructor(
    private val args: Args,
    private val application: Application,
    private val lazyPaymentConfig: Provider<PaymentConfiguration>,
    private val savedStateHandle: SavedStateHandle,
    addressRepository: AddressRepository,
) : ViewModel() {
    private val defaultBillingDetails = args.formArgs.billingDetails
    private val collectionConfiguration = args.formArgs.billingDetailsCollectionConfiguration

    private val collectingAddress =
        args.formArgs.billingDetailsCollectionConfiguration.address == AddressCollectionMode.Full

    private val collectingPhone =
        args.formArgs.billingDetailsCollectionConfiguration.phone == CollectionMode.Always

    private val collectingName =
        args.formArgs.billingDetailsCollectionConfiguration.name != CollectionMode.Never

    private val collectingEmail =
        args.formArgs.billingDetailsCollectionConfiguration.email != CollectionMode.Never

    private val defaultName = if (
        collectingName || collectionConfiguration.attachDefaultsToPaymentMethod
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
        collectingEmail || collectionConfiguration.attachDefaultsToPaymentMethod
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
        collectingPhone || collectionConfiguration.attachDefaultsToPaymentMethod
    ) {
        defaultBillingDetails?.address?.country
    } else {
        null
    }

    private val defaultPhone = if (
        collectingPhone || collectionConfiguration.attachDefaultsToPaymentMethod
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
        collectingAddress || collectionConfiguration.attachDefaultsToPaymentMethod
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

    val lastTextFieldIdentifier: Flow<IdentifierSpec?> = if (collectingAddress) {
        addressElement.getTextFieldIdentifiers().map { it.last() }
    } else if (collectingPhone) {
        flowOf(IdentifierSpec.Phone)
    } else if (collectingEmail) {
        flowOf(IdentifierSpec.Email)
    } else if (collectingName) {
        flowOf(IdentifierSpec.Name)
    } else {
        flowOf(null)
    }

    private val _result = MutableSharedFlow<PaymentSelection.New.USBankAccount>(replay = 1)
    val result: Flow<PaymentSelection.New.USBankAccount> = _result

    init {
        viewModelScope.launch {
            addressElement.countryElement.controller.rawFieldValue.collect {
                it?.let {
                    phoneController.countryDropdownController.onRawValueChange(it)
                }
            }
        }
    }

    private val _currentScreenState: MutableStateFlow<USBankAccountFormScreenState> =
        MutableStateFlow(
            USBankAccountFormScreenState.BillingDetailsCollection(
                name = name.value,
                email = email.value,
                phone = phone.value,
                address = address.value,
                primaryButtonText = application.getString(
                    StripeUiCoreR.string.stripe_continue_button_label
                ),
            )
        )

    val currentScreenState: StateFlow<USBankAccountFormScreenState>
        get() = _currentScreenState

    val saveForFutureUseElement: SaveForFutureUseElement = SaveForFutureUseSpec().transform(
        initialValue = false,
        merchantName = args.formArgs.merchantName
    ) as SaveForFutureUseElement

    val saveForFutureUse: StateFlow<Boolean> = saveForFutureUseElement.controller.saveForFutureUse
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

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

        val hasDefaultName = args.formArgs.billingDetails?.name != null &&
            args.formArgs.billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod
        val hasDefaultEmail = args.formArgs.billingDetails?.email != null &&
            args.formArgs.billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod

        assert((hasDefaultName || collectingName) && (hasDefaultEmail || collectingEmail)) {
            "If name or email are not collected, they must be provided through defaults"
        }
    }

    private var hasLaunched: Boolean
        get() = savedStateHandle.get<Boolean>(HAS_LAUNCHED_KEY) == true
        set(value) = savedStateHandle.set(HAS_LAUNCHED_KEY, value)

    fun register(activityResultRegistryOwner: ActivityResultRegistryOwner) {
        collectBankAccountLauncher = CollectBankAccountLauncher.create(
            activityResultRegistryOwner = activityResultRegistryOwner,
            callback = ::handleCollectBankAccountResult,
        )
    }

    @VisibleForTesting
    fun handleCollectBankAccountResult(result: CollectBankAccountResultInternal) {
        hasLaunched = false
        when (result) {
            is CollectBankAccountResultInternal.Completed -> {
                when (
                    val paymentAccount =
                        result.response.financialConnectionsSession.paymentAccount
                ) {
                    is BankAccount -> {
                        _currentScreenState.update {
                            USBankAccountFormScreenState.VerifyWithMicrodeposits(
                                name = name.value,
                                email = email.value,
                                phone = phone.value,
                                address = address.value,
                                paymentAccount = paymentAccount,
                                financialConnectionsSessionId =
                                result.response.financialConnectionsSession.id,
                                intentId = result.response.intent?.id,
                                primaryButtonText = buildPrimaryButtonText(),
                                mandateText = buildMandateText(),
                                saveForFutureUsage = saveForFutureUse.value
                            )
                        }
                    }
                    is FinancialConnectionsAccount -> {
                        _currentScreenState.update {
                            USBankAccountFormScreenState.MandateCollection(
                                name = name.value,
                                email = email.value,
                                phone = phone.value,
                                address = address.value,
                                paymentAccount = paymentAccount,
                                financialConnectionsSessionId =
                                result.response.financialConnectionsSession.id,
                                intentId = result.response.intent?.id,
                                primaryButtonText = buildPrimaryButtonText(),
                                mandateText = buildMandateText(),
                                saveForFutureUsage = saveForFutureUse.value
                            )
                        }
                    }
                    null -> {
                        reset(R.string.stripe_paymentsheet_ach_something_went_wrong)
                    }
                }
            }
            is CollectBankAccountResultInternal.Failed -> {
                reset(R.string.stripe_paymentsheet_ach_something_went_wrong)
            }
            is CollectBankAccountResultInternal.Cancelled -> {
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
                collectBankAccount(args.clientSecret)
            }
            is USBankAccountFormScreenState.MandateCollection ->
                updatePaymentSelection(
                    intentId = screenState.intentId,
                    linkAccountId = screenState.financialConnectionsSessionId,
                    bankName = screenState.paymentAccount.institutionName,
                    last4 = screenState.paymentAccount.last4
                )
            is USBankAccountFormScreenState.VerifyWithMicrodeposits ->
                updatePaymentSelection(
                    intentId = screenState.intentId,
                    linkAccountId = screenState.financialConnectionsSessionId,
                    bankName = screenState.paymentAccount.bankName,
                    last4 = screenState.paymentAccount.last4
                )
            is USBankAccountFormScreenState.SavedAccount -> {
                screenState.financialConnectionsSessionId?.let { linkAccountId ->
                    updatePaymentSelection(
                        intentId = screenState.intentId,
                        linkAccountId = linkAccountId,
                        bankName = screenState.bankName,
                        last4 = screenState.last4
                    )
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
                    StripeUiCoreR.string.stripe_continue_button_label
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

        collectBankAccountLauncher?.unregister()
        collectBankAccountLauncher = null
    }

    fun formattedMerchantName(): String {
        return args.formArgs.merchantName.trimEnd { it == '.' }
    }

    private fun collectBankAccount(clientSecret: String?) {
        if (hasLaunched) return
        hasLaunched = true
        if (clientSecret != null) {
            if (args.isPaymentFlow) {
                collectBankAccountLauncher?.presentWithPaymentIntent(
                    publishableKey = lazyPaymentConfig.get().publishableKey,
                    stripeAccountId = lazyPaymentConfig.get().stripeAccountId,
                    clientSecret = clientSecret,
                    configuration = CollectBankAccountConfiguration.USBankAccount(
                        name.value,
                        email.value
                    )
                )
            } else {
                collectBankAccountLauncher?.presentWithSetupIntent(
                    publishableKey = lazyPaymentConfig.get().publishableKey,
                    stripeAccountId = lazyPaymentConfig.get().stripeAccountId,
                    clientSecret = clientSecret,
                    configuration = CollectBankAccountConfiguration.USBankAccount(
                        name.value,
                        email.value
                    )
                )
            }
        } else {
            // Decoupled Flow
            args.stripeIntentId?.let { elementsSessionId ->
                if (args.isPaymentFlow) {
                    collectBankAccountLauncher?.presentWithDeferredPayment(
                        publishableKey = lazyPaymentConfig.get().publishableKey,
                        stripeAccountId = lazyPaymentConfig.get().stripeAccountId,
                        configuration = CollectBankAccountConfiguration.USBankAccount(
                            name.value,
                            email.value
                        ),
                        elementsSessionId = elementsSessionId,
                        customerId = null,
                        onBehalfOf = args.onBehalfOf,
                        amount = args.formArgs.amount?.value?.toInt(),
                        currency = args.formArgs.amount?.currencyCode
                    )
                } else {
                    collectBankAccountLauncher?.presentWithDeferredSetup(
                        publishableKey = lazyPaymentConfig.get().publishableKey,
                        stripeAccountId = lazyPaymentConfig.get().stripeAccountId,
                        configuration = CollectBankAccountConfiguration.USBankAccount(
                            name.value,
                            email.value
                        ),
                        elementsSessionId = elementsSessionId,
                        customerId = null,
                        onBehalfOf = args.onBehalfOf,
                    )
                }
            }
        }
    }

    private fun updatePaymentSelection(
        intentId: String?,
        linkAccountId: String,
        bankName: String?,
        last4: String?
    ) {
        if (bankName == null || last4 == null) return

        val paymentSelection = createNewPaymentSelection(
            last4 = last4,
            bankName = bankName,
            linkAccountId = linkAccountId,
            intentId = intentId,
        )

        _result.tryEmit(paymentSelection)
    }

    private fun createNewPaymentSelection(
        last4: String,
        bankName: String,
        linkAccountId: String,
        intentId: String?,
    ): PaymentSelection.New.USBankAccount {
        return PaymentSelection.New.USBankAccount(
            labelResource = application.getString(
                R.string.stripe_paymentsheet_payment_method_item_card_number,
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
    }

    private fun buildPrimaryButtonText(): String {
        return when {
            args.isCompleteFlow -> {
                if (args.isPaymentFlow) {
                    args.formArgs.amount!!.buildPayButtonLabel(application.resources)
                } else {
                    application.getString(
                        StripeUiCoreR.string.stripe_setup_button_label
                    )
                }
            }
            else -> application.getString(
                StripeUiCoreR.string.stripe_continue_button_label
            )
        }
    }

    private fun buildMandateText(): String {
        return ACHText.getContinueMandateText(
            context = application,
            merchantName = formattedMerchantName(),
            isSaveForFutureUseSelected = saveForFutureUse.value,
        )
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

    data class Args(
        val formArgs: FormArguments,
        val isCompleteFlow: Boolean,
        val isPaymentFlow: Boolean,
        val stripeIntentId: String?,
        val clientSecret: String?,
        val onBehalfOf: String?,
        val savedPaymentMethod: PaymentSelection.New.USBankAccount?,
        val shippingDetails: AddressDetails?,
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
