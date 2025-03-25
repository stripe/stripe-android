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
import com.stripe.android.financialconnections.ElementsSessionContext
import com.stripe.android.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.financialconnections.model.BankAccount
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.model.Address
import com.stripe.android.model.IncentiveEligibilitySession
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
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
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.BankFormScreenState.ResultIdentifier
import com.stripe.android.paymentsheet.paymentdatacollection.ach.di.DaggerUSBankAccountFormComponent
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SetAsDefaultPaymentMethodElement
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal class USBankAccountFormViewModel @Inject internal constructor(
    private val args: Args,
    private val application: Application,
    private val lazyPaymentConfig: Provider<PaymentConfiguration>,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val defaultBillingDetails = args.formArgs.billingDetails
    private val collectionConfiguration = args.formArgs.billingDetailsCollectionConfiguration

    private val collectingAddress =
        args.formArgs.billingDetailsCollectionConfiguration.address == AddressCollectionMode.Full

    private val collectingPhone =
        args.formArgs.billingDetailsCollectionConfiguration.phone == CollectionMode.Always

    private val collectingName = if (args.instantDebits) {
        args.formArgs.billingDetailsCollectionConfiguration.name == CollectionMode.Always
    } else {
        args.formArgs.billingDetailsCollectionConfiguration.name != CollectionMode.Never
    }

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

    val name: StateFlow<String> = nameController.formFieldValue.mapAsStateFlow { formFieldEntry ->
        formFieldEntry.takeIf { it.isComplete }?.value ?: ""
    }

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

    val email: StateFlow<String?> = emailController.formFieldValue.mapAsStateFlow { formFieldEntry ->
        formFieldEntry.takeIf { it.isComplete }?.value
    }

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

    val phoneController = PhoneNumberController.createPhoneNumberController(
        initiallySelectedCountryCode = defaultPhoneCountry,
        initialValue = defaultPhone ?: "",
    )

    val phone: StateFlow<String?> = phoneController.formFieldValue.mapAsStateFlow { formFieldEntry ->
        formFieldEntry.takeIf { it.isComplete }?.value
    }

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

    val lastTextFieldIdentifier: StateFlow<IdentifierSpec?> = if (collectingAddress) {
        addressElement.getTextFieldIdentifiers().mapAsStateFlow { it.last() }
    } else if (collectingPhone) {
        stateFlowOf(IdentifierSpec.Phone)
    } else if (collectingEmail) {
        stateFlowOf(IdentifierSpec.Email)
    } else if (collectingName) {
        stateFlowOf(IdentifierSpec.Name)
    } else {
        stateFlowOf(null)
    }

    private val defaultSaveForFutureUse: Boolean =
        args.savedPaymentMethod?.input?.saveForFutureUse ?: false

    val saveForFutureUseElement: SaveForFutureUseElement = SaveForFutureUseElement(
        initialValue = defaultSaveForFutureUse,
        merchantName = args.formArgs.merchantName
    )

    val saveForFutureUseCheckedFlow: StateFlow<Boolean> = saveForFutureUseElement.controller.saveForFutureUse

    val setAsDefaultPaymentMethodElement: SetAsDefaultPaymentMethodElement? =
        if (args.setAsDefaultPaymentMethodEnabled) {
            SetAsDefaultPaymentMethodElement(
                initialValue = false,
                saveForFutureUseCheckedFlow = saveForFutureUseCheckedFlow,
                setAsDefaultMatchesSaveForFutureUse = args.setAsDefaultMatchesSaveForFutureUse,
            )
        } else {
            null
        }

    private val screenStateWithoutSaveForFutureUse = MutableStateFlow(value = determineInitialState())

    private val billingDetails = combineAsStateFlow(name, email, phone, address) { name, email, phone, address ->
        PaymentMethod.BillingDetails(address, email, name, phone)
    }

    val currentScreenState: StateFlow<BankFormScreenState> = combineAsStateFlow(
        screenStateWithoutSaveForFutureUse,
        saveForFutureUseCheckedFlow,
    ) { state, saveForFutureUse ->
        val mandateText = state.linkedBankAccount?.let {
            buildMandateText(
                isVerifyWithMicrodeposits = it.isVerifyingWithMicrodeposits,
                isSaveForFutureUseSelected = saveForFutureUse,
            )
        }

        state.updateWithMandate(mandateText)
    }

    val linkedAccount: StateFlow<PaymentSelection.New.USBankAccount?> = combineAsStateFlow(
        currentScreenState,
        billingDetails,
        setAsDefaultPaymentMethodElement?.controller?.setAsDefaultPaymentMethodChecked ?: stateFlowOf(false),
    ) { state, billingDetails, _ ->
        state.toPaymentSelection(billingDetails)
    }

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

        val hasDefaultName = args.formArgs.billingDetails?.name != null &&
            args.formArgs.billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod
        val hasDefaultEmail = args.formArgs.billingDetails?.email != null &&
            args.formArgs.billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod

        if (!args.instantDebits) {
            assert((hasDefaultName || collectingName) && (hasDefaultEmail || collectingEmail)) {
                "If name or email are not collected, they must be provided through defaults"
            }
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
                financialConnectionsAvailability = args.financialConnectionsAvailability,
                callback = ::handleInstantDebitsResult,
            )
        } else {
            CollectBankAccountLauncher.createForPaymentSheet(
                hostedSurface = args.hostedSurface,
                activityResultRegistryOwner = activityResultRegistryOwner,
                financialConnectionsAvailability = args.financialConnectionsAvailability,
                callback = ::handleCollectBankAccountResult,
            )
        }
    }

    @VisibleForTesting
    fun handleCollectBankAccountResult(result: CollectBankAccountResultInternal) {
        hasLaunched = false

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
        screenStateWithoutSaveForFutureUse.update {
            it.updateWithLinkedBankAccount(
                account = BankFormScreenState.LinkedBankAccount(
                    resultIdentifier = ResultIdentifier.PaymentMethod(result.paymentMethod),
                    bankName = result.bankName,
                    last4 = result.last4,
                    intentId = result.intent?.id,
                    financialConnectionsSessionId = null,
                    mandateText = buildMandateText(isVerifyWithMicrodeposits = false),
                    isVerifyingWithMicrodeposits = false,
                    eligibleForIncentive = result.eligibleForIncentive,
                )
            )
        }
    }

    private fun handleResultForACH(
        usBankAccountData: CollectBankAccountResponseInternal.USBankAccountData,
        intentId: String?,
    ) {
        when (val paymentAccount = usBankAccountData.financialConnectionsSession.paymentAccount) {
            is BankAccount -> {
                screenStateWithoutSaveForFutureUse.update {
                    it.updateWithLinkedBankAccount(
                        account = BankFormScreenState.LinkedBankAccount(
                            resultIdentifier = ResultIdentifier.Session(
                                id = usBankAccountData.financialConnectionsSession.id,
                            ),
                            bankName = paymentAccount.bankName,
                            last4 = paymentAccount.last4,
                            intentId = intentId,
                            financialConnectionsSessionId = usBankAccountData.financialConnectionsSession.id,
                            mandateText = buildMandateText(isVerifyWithMicrodeposits = true),
                            isVerifyingWithMicrodeposits = paymentAccount.usesMicrodeposits,
                        )
                    )
                }
            }

            is FinancialConnectionsAccount -> {
                screenStateWithoutSaveForFutureUse.update {
                    it.updateWithLinkedBankAccount(
                        account = BankFormScreenState.LinkedBankAccount(
                            resultIdentifier = ResultIdentifier.Session(
                                id = usBankAccountData.financialConnectionsSession.id,
                            ),
                            bankName = paymentAccount.institutionName,
                            last4 = paymentAccount.last4,
                            intentId = intentId,
                            financialConnectionsSessionId = usBankAccountData.financialConnectionsSession.id,
                            mandateText = buildMandateText(isVerifyWithMicrodeposits = false),
                            isVerifyingWithMicrodeposits = false,
                        )
                    )
                }
            }

            null -> {
                reset(R.string.stripe_paymentsheet_ach_something_went_wrong.resolvableString)
            }
        }
    }

    fun handlePrimaryButtonClick() {
        val screenState = currentScreenState.value
        if (screenState.linkedBankAccount == null) {
            screenStateWithoutSaveForFutureUse.update {
                it.processing()
            }

            collectBankAccount(args.clientSecret)
        }
    }

    fun reset(error: ResolvableString? = null) {
        hasLaunched = false
        shouldReset = false
        screenStateWithoutSaveForFutureUse.value = args.toInitialState(error = error)
        saveForFutureUseElement.controller.onValueChange(true)
    }

    fun onDestroy() {
        if (shouldReset) {
            reset()
        }
        collectBankAccountLauncher?.unregister()
        collectBankAccountLauncher = null
    }

    fun formattedMerchantName(): String {
        return args.formArgs.merchantName.trimEnd { it == '.' }
    }

    private fun determineInitialState(): BankFormScreenState {
        return if (args.savedPaymentMethod != null) {
            args.savedPaymentMethod.screenState
        } else {
            args.toInitialState()
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
        return CollectBankAccountConfiguration.InstantDebits(
            email = email.value,
            elementsSessionContext = makeElementsSessionContext(),
        )
    }

    private fun createUSBankAccountConfiguration(): CollectBankAccountConfiguration.USBankAccountInternal {
        return CollectBankAccountConfiguration.USBankAccountInternal(
            name = name.value,
            email = email.value,
            elementsSessionContext = makeElementsSessionContext(),
        )
    }

    private fun makeElementsSessionContext(): ElementsSessionContext {
        val intentId = args.stripeIntentId!!
        val eligibleForIncentive = args.incentive != null

        val incentiveEligibilitySession = if (eligibleForIncentive) {
            if (args.clientSecret == null) {
                IncentiveEligibilitySession.DeferredIntent(intentId)
            } else if (args.isPaymentFlow) {
                IncentiveEligibilitySession.PaymentIntent(intentId)
            } else {
                IncentiveEligibilitySession.SetupIntent(intentId)
            }
        } else {
            null
        }

        return ElementsSessionContext(
            amount = args.formArgs.amount?.value,
            currency = args.formArgs.amount?.currencyCode,
            linkMode = args.linkMode,
            billingDetails = makeElementsSessionContextBillingDetails(),
            prefillDetails = makePrefillDetails(),
            incentiveEligibilitySession = incentiveEligibilitySession,
        )
    }

    private fun makeElementsSessionContextBillingDetails(): ElementsSessionContext.BillingDetails {
        val attachDefaultsToPaymentMethod = collectionConfiguration.attachDefaultsToPaymentMethod
        val name = name.value.takeIf { collectingName || attachDefaultsToPaymentMethod }
        val email = email.value.takeIf { collectingEmail || attachDefaultsToPaymentMethod }
        val phone = phone.value.takeIf { collectingPhone || attachDefaultsToPaymentMethod }
        val address = address.value.takeIf { collectingAddress || attachDefaultsToPaymentMethod }

        return ElementsSessionContext.BillingDetails(
            name = name,
            // The createPaymentDetails endpoint does not accept uppercase characters.
            email = email?.lowercase(),
            phone = phone,
            address = address?.let {
                ElementsSessionContext.BillingDetails.Address(
                    line1 = it.line1,
                    line2 = it.line2,
                    postalCode = it.postalCode,
                    city = it.city,
                    state = it.state,
                    country = it.country,
                )
            },
        )
    }

    private fun makePrefillDetails(): ElementsSessionContext.PrefillDetails {
        return ElementsSessionContext.PrefillDetails(
            email = email.value ?: defaultBillingDetails?.email,
            phone = phone.value ?: defaultBillingDetails?.phone,
            phoneCountryCode = phoneController.getCountryCode(),
        )
    }

    private fun collectBankAccountForDeferredIntent() {
        val elementsSessionId = args.stripeIntentId ?: return

        val configuration = if (args.instantDebits) {
            createInstantDebitsConfiguration()
        } else {
            createUSBankAccountConfiguration()
        }

        if (args.isPaymentFlow) {
            collectBankAccountLauncher?.presentWithDeferredPayment(
                publishableKey = lazyPaymentConfig.get().publishableKey,
                stripeAccountId = lazyPaymentConfig.get().stripeAccountId,
                configuration = configuration,
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
                configuration = configuration,
                elementsSessionId = elementsSessionId,
                customerId = null,
                onBehalfOf = args.onBehalfOf,
            )
        }
    }

    private fun createNewPaymentSelection(
        resultIdentifier: ResultIdentifier,
        last4: String?,
        bankName: String?,
        billingDetails: PaymentMethod.BillingDetails,
    ): PaymentSelection.New.USBankAccount {
        val customerRequestedSave = customerRequestedSave(
            showCheckbox = args.showCheckbox,
            saveForFutureUse = saveForFutureUseCheckedFlow.value
        )

        val paymentMethodCreateParams = when (resultIdentifier) {
            is ResultIdentifier.PaymentMethod -> {
                PaymentMethodCreateParams.createInstantDebits(
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
                    billingDetails = billingDetails,
                    allowRedisplay = args.formArgs.paymentMethodSaveConsentBehavior.allowRedisplay(
                        isSetupIntent = args.formArgs.hasIntentToSetup,
                        customerRequestedSave = customerRequestedSave,
                    ),
                )
            }
        }

        val instantDebitsInfo = (resultIdentifier as? ResultIdentifier.PaymentMethod)?.let {
            PaymentSelection.New.USBankAccount.InstantDebitsInfo(
                paymentMethod = it.paymentMethod.copy(
                    billingDetails = billingDetails,
                ),
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

        val labelResource = last4?.let {
            application.getString(
                R.string.stripe_paymentsheet_payment_method_item_card_number,
                it,
            )
        }

        return PaymentSelection.New.USBankAccount(
            label = labelResource ?: "••••",
            iconResource = TransformToBankIcon(bankName),
            paymentMethodCreateParams = paymentMethodCreateParams,
            paymentMethodOptionsParams = paymentMethodOptionsParams,
            customerRequestedSave = customerRequestedSave,
            screenState = currentScreenState.value,
            instantDebits = instantDebitsInfo,
            input = PaymentSelection.New.USBankAccount.Input(
                name = billingDetails.name.orEmpty(),
                email = billingDetails.email,
                phone = billingDetails.phone,
                address = billingDetails.address,
                saveForFutureUse = saveForFutureUseCheckedFlow.value,
            ),
            paymentMethodExtraParams = if (setAsDefaultPaymentMethodElement != null) {
                PaymentMethodExtraParams.USBankAccount(
                    setAsDefault = setAsDefaultPaymentMethodElement.controller.shouldPaymentMethodBeSetAsDefault.value
                )
            } else {
                null
            }
        )
    }

    private fun buildMandateText(
        isVerifyWithMicrodeposits: Boolean,
        isSaveForFutureUseSelected: Boolean = saveForFutureUseCheckedFlow.value,
    ): ResolvableString {
        return USBankAccountTextBuilder.buildMandateAndMicrodepositsText(
            merchantName = formattedMerchantName(),
            isVerifyingMicrodeposits = isVerifyWithMicrodeposits,
            isSaveForFutureUseSelected = isSaveForFutureUseSelected,
            isInstantDebits = args.instantDebits,
            isSetupFlow = !args.isPaymentFlow,
        )
    }

    private fun BankFormScreenState.toPaymentSelection(
        billingDetails: PaymentMethod.BillingDetails,
    ): PaymentSelection.New.USBankAccount? {
        val linkedAccount = linkedBankAccount ?: return null

        return createNewPaymentSelection(
            resultIdentifier = linkedAccount.resultIdentifier,
            bankName = linkedBankAccount.bankName,
            last4 = linkedAccount.last4,
            billingDetails = billingDetails,
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
        val incentive: PaymentMethodIncentive?,
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
        val financialConnectionsAvailability: FinancialConnectionsAvailability?,
        val setAsDefaultPaymentMethodEnabled: Boolean,
        val setAsDefaultMatchesSaveForFutureUse: Boolean,
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

private fun USBankAccountFormViewModel.Args.toInitialState(
    error: ResolvableString? = null,
): BankFormScreenState {
    return BankFormScreenState(
        isPaymentFlow = isPaymentFlow,
        promoText = incentive?.displayText,
        error = error,
    )
}
