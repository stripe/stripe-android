package com.stripe.android.paymentsheet.paymentdatacollection.ach

import android.app.Application
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.model.Address
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.payments.bankaccount.CollectBankAccountConfiguration
import com.stripe.android.payments.bankaccount.CollectBankAccountForInstantDebitsLauncher
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountForInstantDebitsResult
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResponseInternal
import com.stripe.android.payments.bankaccount.navigation.CollectBankAccountResultInternal
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toIdentifierMap
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormScreenState.ResultIdentifier
import com.stripe.android.paymentsheet.paymentdatacollection.ach.di.DaggerUSBankAccountFormComponent
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.uicore.elements.AddressElement
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.SameAsShippingController
import com.stripe.android.uicore.elements.SameAsShippingElement
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
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
) : ViewModel() {
    private val defaultBillingDetails = args.formArgs.billingDetails
    private val collectionConfiguration = args.formArgs.billingDetailsCollectionConfiguration

    val fieldsState = BankFormFieldsState(
        formArgs = args.formArgs,
        instantDebits = args.instantDebits,
    )

    private val defaultName: String? = if (args.savedPaymentMethod != null) {
        args.savedPaymentMethod.input.name
    } else if (fieldsState.showNameField || collectionConfiguration.attachDefaultsToPaymentMethod) {
        defaultBillingDetails?.name
    } else {
        null
    }

    val nameController: TextFieldController = NameConfig.createController(
        initialValue = defaultName,
    )

    val name: StateFlow<String> = nameController.formFieldValue.mapAsStateFlow { formFieldEntry ->
        formFieldEntry.takeIf { it.isComplete }?.value ?: ""
    }

    private val defaultEmail: String? = if (args.savedPaymentMethod != null) {
        args.savedPaymentMethod.input.email
    } else if (fieldsState.showEmailField || collectionConfiguration.attachDefaultsToPaymentMethod) {
        defaultBillingDetails?.email
    } else {
        null
    }

    val emailController: TextFieldController = EmailConfig.createController(
        initialValue = args.savedPaymentMethod?.input?.email ?: defaultEmail,
    )

    val email: StateFlow<String?> = emailController.formFieldValue.mapAsStateFlow { formFieldEntry ->
        formFieldEntry.takeIf { it.isComplete }?.value
    }

    private val defaultPhoneCountry = if (args.savedPaymentMethod != null) {
        args.savedPaymentMethod.input.address?.country
    } else if (fieldsState.showPhoneField || collectionConfiguration.attachDefaultsToPaymentMethod) {
        defaultBillingDetails?.address?.country
    } else {
        null
    }

    private val defaultPhone: String? = if (args.savedPaymentMethod != null) {
        args.savedPaymentMethod.input.phone
    } else if (fieldsState.showPhoneField || collectionConfiguration.attachDefaultsToPaymentMethod) {
        defaultBillingDetails?.phone
    } else {
        null
    }

    val phoneController = PhoneNumberController.createPhoneNumberController(
        initiallySelectedCountryCode = defaultPhoneCountry,
        initialValue = defaultPhone ?: "",
    )

    val phone: StateFlow<String?> = phoneController.formFieldValue.mapAsStateFlow { formFieldEntry ->
        formFieldEntry.takeIf { it.isComplete }?.value
    }

    private val defaultAddress: Address? = if (args.savedPaymentMethod != null) {
        args.savedPaymentMethod.input.address
    } else if (fieldsState.showAddressFields || collectionConfiguration.attachDefaultsToPaymentMethod) {
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
        rawValuesMap = defaultAddress?.asFormFieldValues() ?: emptyMap(),
        sameAsShippingElement = sameAsShippingElement,
        shippingValuesMap = args.formArgs.shippingDetails?.toIdentifierMap(args.formArgs.billingDetails),
    )

    // AddressElement generates a default address if the initial value is null, so we can't rely
    // on the value produced by the controller in that case.
    val address: StateFlow<Address?> = if (defaultAddress == null) {
        MutableStateFlow(null)
    } else {
        addressElement.getFormFieldValueFlow().mapAsStateFlow { formFieldValues ->
            val rawMap = formFieldValues.associate { it.first to it.second.value }
            Address.fromFormFieldValues(rawMap)
        }
    }

    val lastTextFieldIdentifier: StateFlow<IdentifierSpec?> = if (fieldsState.showAddressFields) {
        addressElement.getTextFieldIdentifiers().mapAsStateFlow { it.last() }
    } else if (fieldsState.showPhoneField) {
        stateFlowOf(IdentifierSpec.Phone)
    } else if (fieldsState.showEmailField) {
        stateFlowOf(IdentifierSpec.Email)
    } else if (fieldsState.showNameField) {
        stateFlowOf(IdentifierSpec.Name)
    } else {
        stateFlowOf(null)
    }

    private val _result = MutableSharedFlow<PaymentSelection.New.USBankAccount?>(replay = 1)
    val result: Flow<PaymentSelection.New.USBankAccount?> = _result
    private val _collectBankAccountResult = MutableSharedFlow<CollectBankAccountResultInternal?>(replay = 1)
    val collectBankAccountResult: Flow<CollectBankAccountResultInternal?> = _collectBankAccountResult

    private val defaultSaveForFutureUse: Boolean =
        args.savedPaymentMethod?.input?.saveForFutureUse ?: false

    val saveForFutureUseElement: SaveForFutureUseElement = SaveForFutureUseElement(
        initialValue = defaultSaveForFutureUse,
        merchantName = args.formArgs.merchantName
    )

    val saveForFutureUse: StateFlow<Boolean> = saveForFutureUseElement.controller.saveForFutureUse

    private val _currentScreenState = MutableStateFlow(value = determineInitialState())
    val currentScreenState: StateFlow<USBankAccountFormScreenState> = _currentScreenState

    val requiredFields = combineAsStateFlow(
        nameController.formFieldValue.mapAsStateFlow { it.isComplete },
        emailController.formFieldValue.mapAsStateFlow { it.isComplete },
        phoneController.formFieldValue.mapAsStateFlow { it.isComplete },
        addressElement.getFormFieldValueFlow().mapAsStateFlow { formFieldValues ->
            formFieldValues.all { it.second.isComplete }
        }
    ) { validName, validEmail, validPhone, validAddress ->
        val validBaseInfo = if (args.instantDebits) {
            validEmail
        } else {
            validName && validEmail
        }

        val validAddressInfo = (validPhone || collectionConfiguration.phone != CollectionMode.Always) &&
            (validAddress || collectionConfiguration.address != AddressCollectionMode.Full)

        validBaseInfo && validAddressInfo
    }

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

        viewModelScope.launch {
            saveForFutureUse.onEach { saveForFutureUse ->
                updateScreenStateWithSaveForFutureUse(saveForFutureUse)
            }.collect()
        }
    }

    private var hasLaunched: Boolean
        get() = savedStateHandle.get<Boolean>(HAS_LAUNCHED_KEY) == true
        set(value) = savedStateHandle.set(HAS_LAUNCHED_KEY, value)

    private var shouldReset: Boolean
        get() = savedStateHandle.get<Boolean>(SHOULD_RESET_KEY) == true
        set(value) = savedStateHandle.set(SHOULD_RESET_KEY, value)

    fun register(activityResultRegistryOwner: ActivityResultRegistryOwner) {
        collectBankAccountLauncher = if (args.instantDebits) {
            CollectBankAccountForInstantDebitsLauncher.createForPaymentSheet(
                hostedSurface = args.hostedSurface,
                activityResultRegistryOwner = activityResultRegistryOwner,
                callback = ::handleInstantDebitsResult,
            )
        } else {
            CollectBankAccountLauncher.createForPaymentSheet(
                hostedSurface = args.hostedSurface,
                activityResultRegistryOwner = activityResultRegistryOwner,
                callback = ::handleCollectBankAccountResult,
            )
        }
    }

    @VisibleForTesting
    fun handleCollectBankAccountResult(result: CollectBankAccountResultInternal) {
        hasLaunched = false
        _collectBankAccountResult.tryEmit(result)
        when (result) {
            is CollectBankAccountResultInternal.Completed -> {
                handleCompletedBankAccountResult(result)
            }

            is CollectBankAccountResultInternal.Failed -> {
                reset(R.string.stripe_paymentsheet_ach_something_went_wrong.resolvableString)
            }

            is CollectBankAccountResultInternal.Cancelled -> {
                reset()
            }
        }
    }

    private fun handleInstantDebitsResult(result: CollectBankAccountForInstantDebitsResult) {
        hasLaunched = false

        when (result) {
            is CollectBankAccountForInstantDebitsResult.Completed -> {
                handleCompletedInstantDebitsResult(result)
            }
            is CollectBankAccountForInstantDebitsResult.Failed -> {
                reset(R.string.stripe_paymentsheet_ach_something_went_wrong.resolvableString)
            }
            is CollectBankAccountForInstantDebitsResult.Cancelled -> {
                reset()
            }
        }
    }

    private fun handleCompletedBankAccountResult(
        result: CollectBankAccountResultInternal.Completed,
    ) {
        val intentId = result.response.intent?.id
        val usBankAccountData = result.response.usBankAccountData

        if (usBankAccountData != null) {
            handleResultForACH(usBankAccountData, intentId)
        } else {
            reset(R.string.stripe_paymentsheet_ach_something_went_wrong.resolvableString)
        }
    }

    private fun handleCompletedInstantDebitsResult(
        result: CollectBankAccountForInstantDebitsResult.Completed,
    ) {
        _currentScreenState.update {
            USBankAccountFormScreenState.MandateCollection(
                resultIdentifier = ResultIdentifier.PaymentMethod(result.paymentMethodId),
                bankName = result.bankName,
                last4 = result.last4,
                intentId = result.intent.id,
                primaryButtonText = buildPrimaryButtonText(),
                mandateText = buildMandateText(isVerifyWithMicrodeposits = false),
            )
        }
    }

    private fun handleResultForACH(
        usBankAccountData: CollectBankAccountResponseInternal.USBankAccountData,
        intentId: String?,
    ) {
        when (val paymentAccount = usBankAccountData.financialConnectionsSession.paymentAccount) {
            is BankAccount -> {
                _currentScreenState.update {
                    USBankAccountFormScreenState.VerifyWithMicrodeposits(
                        paymentAccount = paymentAccount,
                        financialConnectionsSessionId = usBankAccountData.financialConnectionsSession.id,
                        intentId = intentId,
                        primaryButtonText = buildPrimaryButtonText(),
                        mandateText = buildMandateText(isVerifyWithMicrodeposits = true),
                    )
                }
            }

            is FinancialConnectionsAccount -> {
                _currentScreenState.update {
                    USBankAccountFormScreenState.MandateCollection(
                        resultIdentifier = ResultIdentifier.Session(
                            id = usBankAccountData.financialConnectionsSession.id,
                        ),
                        bankName = paymentAccount.institutionName,
                        last4 = paymentAccount.last4,
                        intentId = intentId,
                        primaryButtonText = buildPrimaryButtonText(),
                        mandateText = buildMandateText(isVerifyWithMicrodeposits = false),
                    )
                }
            }

            null -> {
                reset(R.string.stripe_paymentsheet_ach_something_went_wrong.resolvableString)
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
                    resultIdentifier = screenState.resultIdentifier,
                    bankName = screenState.bankName,
                    last4 = screenState.last4,
                )

            is USBankAccountFormScreenState.VerifyWithMicrodeposits ->
                updatePaymentSelection(
                    resultIdentifier = ResultIdentifier.Session(
                        id = screenState.financialConnectionsSessionId,
                    ),
                    bankName = screenState.paymentAccount.bankName,
                    last4 = screenState.paymentAccount.last4
                )

            is USBankAccountFormScreenState.SavedAccount -> {
                screenState.financialConnectionsSessionId?.let { linkAccountId ->
                    updatePaymentSelection(
                        resultIdentifier = ResultIdentifier.Session(
                            id = linkAccountId,
                        ),
                        bankName = screenState.bankName,
                        last4 = screenState.last4
                    )
                }
            }
        }
    }

    fun reset(error: ResolvableString? = null) {
        hasLaunched = false
        shouldReset = false
        saveForFutureUseElement.controller.onValueChange(true)
        _collectBankAccountResult.tryEmit(null)
        _currentScreenState.update {
            USBankAccountFormScreenState.BillingDetailsCollection(
                error = error,
                primaryButtonText = StripeUiCoreR.string.stripe_continue_button_label.resolvableString,
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
                primaryButtonText = StripeUiCoreR.string.stripe_continue_button_label.resolvableString,
                isProcessing = false,
            )
        }
    }

    private fun collectBankAccount(clientSecret: String?) {
        if (hasLaunched) return
        hasLaunched = true

        if (clientSecret != null) {
            collectBankAccountForIntent(clientSecret)
        } else {
            collectBankAccountForDeferredIntent()
        }
    }

    private fun collectBankAccountForIntent(clientSecret: String) {
        val configuration = if (args.instantDebits) {
            createInstantDebitsConfiguration()
        } else {
            createUSBankAccountConfiguration()
        }

        if (args.isPaymentFlow) {
            collectBankAccountLauncher?.presentWithPaymentIntent(
                publishableKey = lazyPaymentConfig.get().publishableKey,
                stripeAccountId = lazyPaymentConfig.get().stripeAccountId,
                clientSecret = clientSecret,
                configuration = configuration,
            )
        } else {
            collectBankAccountLauncher?.presentWithSetupIntent(
                publishableKey = lazyPaymentConfig.get().publishableKey,
                stripeAccountId = lazyPaymentConfig.get().stripeAccountId,
                clientSecret = clientSecret,
                configuration = configuration,
            )
        }
    }

    private fun createInstantDebitsConfiguration(): CollectBankAccountConfiguration.InstantDebits {
        val initializationMode = if (args.clientSecret == null) {
            ElementsSessionContext.InitializationMode.DeferredIntent
        } else if (args.isPaymentFlow) {
            ElementsSessionContext.InitializationMode.PaymentIntent(args.stripeIntentId!!)
        } else {
            ElementsSessionContext.InitializationMode.SetupIntent(args.stripeIntentId!!)
        }

        return CollectBankAccountConfiguration.InstantDebits(
            email = email.value,
            elementsSessionContext = ElementsSessionContext(
                initializationMode = initializationMode,
                amount = args.formArgs.amount?.value,
                currency = args.formArgs.amount?.currencyCode,
                linkMode = args.linkMode,
            ),
        )
    }

    private fun createUSBankAccountConfiguration(): CollectBankAccountConfiguration.USBankAccount {
        return CollectBankAccountConfiguration.USBankAccount(
            name = name.value,
            email = email.value,
        )
    }

    private fun collectBankAccountForDeferredIntent() {
        val elementsSessionId = args.stripeIntentId ?: return

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

    private fun updatePaymentSelection(
        resultIdentifier: ResultIdentifier,
        bankName: String?,
        last4: String?
    ) {
        if (bankName == null || last4 == null) return

        val paymentSelection = createNewPaymentSelection(
            resultIdentifier = resultIdentifier,
            last4 = last4,
            bankName = bankName,
        )

        _result.tryEmit(paymentSelection)
        shouldReset = true
    }

    private fun createNewPaymentSelection(
        resultIdentifier: ResultIdentifier,
        last4: String,
        bankName: String,
    ): PaymentSelection.New.USBankAccount {
        val customerRequestedSave = customerRequestedSave(
            showCheckbox = args.showCheckbox,
            saveForFutureUse = saveForFutureUse.value
        )

        val paymentMethodCreateParams = when (resultIdentifier) {
            is ResultIdentifier.PaymentMethod -> {
                PaymentMethodCreateParams.createInstantDebits(
                    paymentMethodId = resultIdentifier.id,
                    requiresMandate = true,
                    productUsage = setOf("PaymentSheet"),
                    allowRedisplay = args.formArgs.paymentMethodSaveConsentBehavior.allowRedisplay(
                        isSetupIntent = args.formArgs.hasIntentToSetup,
                        customerRequestedSave = customerRequestedSave,
                    ),
                )
            }
            is ResultIdentifier.Session -> {
                PaymentMethodCreateParams.create(
                    usBankAccount = PaymentMethodCreateParams.USBankAccount(
                        linkAccountSessionId = resultIdentifier.id,
                    ),
                    billingDetails = PaymentMethod.BillingDetails(
                        name = name.value,
                        email = email.value,
                        phone = phone.value,
                        address = address.value,
                    ),
                    allowRedisplay = args.formArgs.paymentMethodSaveConsentBehavior.allowRedisplay(
                        isSetupIntent = args.formArgs.hasIntentToSetup,
                        customerRequestedSave = customerRequestedSave,
                    ),
                )
            }
        }

        val instantDebitsInfo = (resultIdentifier as? ResultIdentifier.PaymentMethod)?.let {
            PaymentSelection.New.USBankAccount.InstantDebitsInfo(
                paymentMethodId = it.id,
                linkMode = args.linkMode,
            )
        }

        val paymentMethodOptionsParams = if (resultIdentifier is ResultIdentifier.Session) {
            PaymentMethodOptionsParams.USBankAccount(
                setupFutureUsage = customerRequestedSave.setupFutureUsage
            )
        } else {
            null
        }

        return PaymentSelection.New.USBankAccount(
            labelResource = application.getString(
                R.string.stripe_paymentsheet_payment_method_item_card_number,
                last4
            ),
            iconResource = TransformToBankIcon(bankName),
            paymentMethodCreateParams = paymentMethodCreateParams,
            paymentMethodOptionsParams = paymentMethodOptionsParams,
            customerRequestedSave = customerRequestedSave,
            screenState = currentScreenState.value,
            instantDebits = instantDebitsInfo,
            input = PaymentSelection.New.USBankAccount.Input(
                name = name.value,
                email = email.value,
                phone = phone.value,
                address = address.value,
                saveForFutureUse = saveForFutureUse.value,
            ),
        )
    }

    private fun buildPrimaryButtonText(): ResolvableString {
        return when {
            args.isCompleteFlow -> {
                if (args.isPaymentFlow) {
                    args.formArgs.amount!!.buildPayButtonLabel()
                } else {
                    StripeUiCoreR.string.stripe_setup_button_label.resolvableString
                }
            }

            else -> StripeUiCoreR.string.stripe_continue_button_label.resolvableString
        }
    }

    private fun updateScreenStateWithSaveForFutureUse(saveForFutureUse: Boolean) {
        _currentScreenState.update { state ->
            val mandateText = buildMandateText(
                isVerifyWithMicrodeposits = state is USBankAccountFormScreenState.VerifyWithMicrodeposits,
                isSaveForFutureUseSelected = saveForFutureUse,
            )
            state.updateWithMandate(mandateText)
        }
    }

    private fun buildMandateText(
        isVerifyWithMicrodeposits: Boolean,
        isSaveForFutureUseSelected: Boolean = saveForFutureUse.value,
    ): ResolvableString {
        return USBankAccountTextBuilder.buildMandateAndMicrodepositsText(
            merchantName = formattedMerchantName(),
            isVerifyingMicrodeposits = isVerifyWithMicrodeposits,
            isSaveForFutureUseSelected = isSaveForFutureUseSelected,
            isInstantDebits = args.instantDebits,
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
        val instantDebits: Boolean,
        val linkMode: LinkMode?,
        val formArgs: FormArguments,
        val showCheckbox: Boolean,
        val isCompleteFlow: Boolean,
        val isPaymentFlow: Boolean,
        val stripeIntentId: String?,
        val clientSecret: String?,
        val onBehalfOf: String?,
        val savedPaymentMethod: PaymentSelection.New.USBankAccount?,
        val shippingDetails: AddressDetails?,
        val hostedSurface: String,
    )

    private companion object {
        private const val HAS_LAUNCHED_KEY = "has_launched"
        private const val SHOULD_RESET_KEY = "should_reset"
    }
}

internal data class BankFormFieldsState(
    val showNameField: Boolean,
    val showEmailField: Boolean,
    val showPhoneField: Boolean,
    val showAddressFields: Boolean,
)

internal fun BankFormFieldsState(
    formArgs: FormArguments,
    instantDebits: Boolean,
): BankFormFieldsState {
    val attachDefaults = formArgs.billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod

    val hasDefaultName = attachDefaults && !formArgs.billingDetails?.name.isNullOrBlank()
    val hasDefaultEmail = attachDefaults && !formArgs.billingDetails?.email.isNullOrBlank()

    val collectsName = if (instantDebits) {
        formArgs.billingDetailsCollectionConfiguration.name == CollectionMode.Always
    } else {
        formArgs.billingDetailsCollectionConfiguration.name != CollectionMode.Never
    }

    val collectsEmail = formArgs.billingDetailsCollectionConfiguration.email != CollectionMode.Never

    return BankFormFieldsState(
        showNameField = if (instantDebits) {
            collectsName
        } else {
            collectsName || !hasDefaultName
        },
        showEmailField = collectsEmail || !hasDefaultEmail,
        showPhoneField = formArgs.billingDetailsCollectionConfiguration.phone == CollectionMode.Always,
        showAddressFields = formArgs.billingDetailsCollectionConfiguration.address == AddressCollectionMode.Full,
    )
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
