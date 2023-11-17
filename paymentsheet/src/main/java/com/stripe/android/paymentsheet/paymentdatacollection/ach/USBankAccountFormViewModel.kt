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
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
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

    private val defaultName: String? = if (args.savedPaymentMethod != null) {
        args.savedPaymentMethod.input.name
    } else if (collectingName || collectionConfiguration.attachDefaultsToPaymentMethod) {
        defaultBillingDetails?.name
    } else {
        null
    }

    val nameController: TextFieldController = NameConfig.createController(
        initialValue = defaultName,
    )

    val name: StateFlow<String> = nameController.formFieldValue.map { formFieldEntry ->
        formFieldEntry.takeIf { it.isComplete }?.value ?: ""
    }.stateIn(viewModelScope, SharingStarted.Eagerly, defaultName ?: "")

    private val defaultEmail: String? = if (args.savedPaymentMethod != null) {
        args.savedPaymentMethod.input.email
    } else if (collectingEmail || collectionConfiguration.attachDefaultsToPaymentMethod) {
        defaultBillingDetails?.email
    } else {
        null
    }

    val emailController: TextFieldController = EmailConfig.createController(
        initialValue = args.savedPaymentMethod?.input?.email ?: defaultEmail,
    )

    val email: StateFlow<String?> = emailController.formFieldValue.map { formFieldEntry ->
        formFieldEntry.takeIf { it.isComplete }?.value
    }.stateIn(viewModelScope, SharingStarted.Eagerly, defaultEmail)

    private val defaultPhoneCountry = if (args.savedPaymentMethod != null) {
        args.savedPaymentMethod.input.address?.country
    } else if (collectingPhone || collectionConfiguration.attachDefaultsToPaymentMethod) {
        defaultBillingDetails?.address?.country
    } else {
        null
    }

    private val defaultPhone: String? = if (args.savedPaymentMethod != null) {
        args.savedPaymentMethod.input.phone
    } else if (collectingPhone || collectionConfiguration.attachDefaultsToPaymentMethod) {
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
    }.stateIn(viewModelScope, SharingStarted.Eagerly, defaultPhone)

    private val defaultAddress: Address? = if (args.savedPaymentMethod != null) {
        args.savedPaymentMethod.input.address
    } else if (collectingAddress || collectionConfiguration.attachDefaultsToPaymentMethod) {
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
        shippingValuesMap = args.formArgs.shippingDetails?.toIdentifierMap(args.formArgs.billingDetails),
    )

    // AddressElement generates a default address if the initial value is null, so we can't rely
    // on the value produced by the controller in that case.
    val address: StateFlow<Address?> = if (defaultAddress == null) {
        MutableStateFlow(null)
    } else {
        addressElement.getFormFieldValueFlow().map { formFieldValues ->
            val rawMap = formFieldValues.associate { it.first to it.second.value }
            Address.fromFormFieldValues(rawMap)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = defaultAddress,
        )
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

    private val _result = MutableSharedFlow<PaymentSelection.New.USBankAccount?>(replay = 1)
    val result: Flow<PaymentSelection.New.USBankAccount?> = _result
    private val _collectBankAccountResult = MutableSharedFlow<CollectBankAccountResultInternal?>(replay = 1)
    val collectBankAccountResult: Flow<CollectBankAccountResultInternal?> = _collectBankAccountResult

    private val defaultSaveForFutureUse: Boolean =
        args.savedPaymentMethod?.input?.saveForFutureUse ?: false

    val saveForFutureUseElement: SaveForFutureUseElement = SaveForFutureUseSpec().transform(
        initialValue = defaultSaveForFutureUse,
        merchantName = args.formArgs.merchantName
    ) as SaveForFutureUseElement

    val saveForFutureUse: StateFlow<Boolean> = saveForFutureUseElement.controller.saveForFutureUse
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = defaultSaveForFutureUse,
        )

    private val _currentScreenState = MutableStateFlow(value = determineInitialState())
    val currentScreenState: StateFlow<USBankAccountFormScreenState> = _currentScreenState

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
        viewModelScope.launch {
            addressElement.countryElement.controller.rawFieldValue.collect {
                it?.let {
                    phoneController.countryDropdownController.onRawValueChange(it)
                }
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

    private var shouldReset: Boolean
        get() = savedStateHandle.get<Boolean>(SHOULD_RESET_KEY) == true
        set(value) = savedStateHandle.set(SHOULD_RESET_KEY, value)

    fun register(activityResultRegistryOwner: ActivityResultRegistryOwner) {
        collectBankAccountLauncher = CollectBankAccountLauncher.create(
            activityResultRegistryOwner = activityResultRegistryOwner,
            callback = ::handleCollectBankAccountResult,
        )
    }

    @VisibleForTesting
    fun handleCollectBankAccountResult(result: CollectBankAccountResultInternal) {
        hasLaunched = false
        _collectBankAccountResult.tryEmit(result)
        when (result) {
            is CollectBankAccountResultInternal.Completed -> {
                when (
                    val paymentAccount =
                        result.response.financialConnectionsSession.paymentAccount
                ) {
                    is BankAccount -> {
                        _currentScreenState.update {
                            USBankAccountFormScreenState.VerifyWithMicrodeposits(
                                paymentAccount = paymentAccount,
                                financialConnectionsSessionId = result.response.financialConnectionsSession.id,
                                intentId = result.response.intent?.id,
                                primaryButtonText = buildPrimaryButtonText(),
                                mandateText = buildMandateText(),
                            )
                        }
                    }
                    is FinancialConnectionsAccount -> {
                        _currentScreenState.update {
                            USBankAccountFormScreenState.MandateCollection(
                                paymentAccount = paymentAccount,
                                financialConnectionsSessionId =
                                result.response.financialConnectionsSession.id,
                                intentId = result.response.intent?.id,
                                primaryButtonText = buildPrimaryButtonText(),
                                mandateText = buildMandateText(),
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
        when (screenState) {
            is USBankAccountFormScreenState.BillingDetailsCollection -> {
                _currentScreenState.update {
                    screenState.copy(isProcessing = true)
                }
                collectBankAccount(args.clientSecret)
            }
            is USBankAccountFormScreenState.MandateCollection ->
                updatePaymentSelection(
                    linkAccountId = screenState.financialConnectionsSessionId,
                    bankName = screenState.paymentAccount.institutionName,
                    last4 = screenState.paymentAccount.last4
                )
            is USBankAccountFormScreenState.VerifyWithMicrodeposits ->
                updatePaymentSelection(
                    linkAccountId = screenState.financialConnectionsSessionId,
                    bankName = screenState.paymentAccount.bankName,
                    last4 = screenState.paymentAccount.last4
                )
            is USBankAccountFormScreenState.SavedAccount -> {
                screenState.financialConnectionsSessionId?.let { linkAccountId ->
                    updatePaymentSelection(
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
        shouldReset = false
        saveForFutureUseElement.controller.onValueChange(true)
        _collectBankAccountResult.tryEmit(null)
        _currentScreenState.update {
            USBankAccountFormScreenState.BillingDetailsCollection(
                error = error,
                primaryButtonText = application.getString(
                    StripeUiCoreR.string.stripe_continue_button_label
                ),
                isProcessing = false,
            )
        }
    }

    fun onDestroy() {
        if (shouldReset) {
            reset()
        }
        _result.tryEmit(null)
        _collectBankAccountResult.tryEmit(null)
        collectBankAccountLauncher?.unregister()
        collectBankAccountLauncher = null
    }

    fun formattedMerchantName(): String {
        return args.formArgs.merchantName.trimEnd { it == '.' }
    }

    private fun determineInitialState(): USBankAccountFormScreenState {
        return if (args.savedPaymentMethod != null) {
            args.savedPaymentMethod.screenState
        } else {
            USBankAccountFormScreenState.BillingDetailsCollection(
                primaryButtonText = application.getString(
                    StripeUiCoreR.string.stripe_continue_button_label
                ),
                isProcessing = false,
            )
        }
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
        linkAccountId: String,
        bankName: String?,
        last4: String?
    ) {
        if (bankName == null || last4 == null) return

        val paymentSelection = createNewPaymentSelection(
            last4 = last4,
            bankName = bankName,
            linkAccountId = linkAccountId,
        )

        _result.tryEmit(paymentSelection)
        shouldReset = true
    }

    private fun createNewPaymentSelection(
        last4: String,
        bankName: String,
        linkAccountId: String,
    ): PaymentSelection.New.USBankAccount {
        val customerRequestedSave = customerRequestedSave(
            showCheckbox = args.formArgs.showCheckbox,
            saveForFutureUse = saveForFutureUse.value
        )
        return PaymentSelection.New.USBankAccount(
            labelResource = application.getString(
                R.string.stripe_paymentsheet_payment_method_item_card_number,
                last4
            ),
            iconResource = TransformToBankIcon(
                bankName
            ),
            paymentMethodCreateParams = PaymentMethodCreateParams.create(
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
            paymentMethodOptionsParams = PaymentMethodOptionsParams.USBankAccount(
                setupFutureUsage = customerRequestedSave.setupFutureUsage
            ),
            customerRequestedSave = customerRequestedSave,
            screenState = currentScreenState.value,
            input = PaymentSelection.New.USBankAccount.Input(
                name = name.value,
                email = email.value,
                phone = phone.value,
                address = address.value,
                saveForFutureUse = saveForFutureUse.value,
            ),
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
            isSetupFlow = !args.isPaymentFlow,
        )
    }

    internal class Factory(
        private val argsSupplier: () -> Args,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return DaggerUSBankAccountFormComponent
                .builder()
                .application(extras.requireApplication())
                .build()
                .subComponentBuilderProvider.get()
                .configuration(argsSupplier())
                .savedStateHandle(extras.createSavedStateHandle())
                .build().viewModel as T
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
    )

    private companion object {
        private const val HAS_LAUNCHED_KEY = "has_launched"
        private const val SHOULD_RESET_KEY = "should_reset"
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

internal fun customerRequestedSave(
    showCheckbox: Boolean,
    saveForFutureUse: Boolean
): PaymentSelection.CustomerRequestedSave {
    return if (showCheckbox) {
        if (saveForFutureUse) {
            PaymentSelection.CustomerRequestedSave.RequestReuse
        } else {
            PaymentSelection.CustomerRequestedSave.RequestNoReuse
        }
    } else {
        PaymentSelection.CustomerRequestedSave.NoRequest
    }
}
