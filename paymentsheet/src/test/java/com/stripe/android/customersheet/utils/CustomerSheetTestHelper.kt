package com.stripe.android.customersheet.utils

import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetLoader
import com.stripe.android.customersheet.CustomerSheetViewModel
import com.stripe.android.customersheet.CustomerSheetViewState
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.FakeCustomerAdapter
import com.stripe.android.customersheet.FakeStripeRepository
import com.stripe.android.customersheet.analytics.CustomerSheetEventReporter
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.ui.DefaultEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.PaymentMethodRemoveOperation
import com.stripe.android.paymentsheet.ui.PaymentMethodUpdateOperation
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalCustomerSheetApi::class)
internal object CustomerSheetTestHelper {
    internal val application = ApplicationProvider.getApplicationContext<Application>()

    internal val usBankAccountFormArguments = USBankAccountFormArguments(
        onBehalfOf = null,
        isCompleteFlow = false,
        isPaymentFlow = false,
        stripeIntentId = null,
        clientSecret = null,
        shippingDetails = null,
        draftPaymentSelection = null,
        onMandateTextChanged = { _, _ -> },
        onCollectBankAccountResult = { },
        onConfirmUSBankAccount = { },
        onUpdatePrimaryButtonState = { },
        onUpdatePrimaryButtonUIState = { },
        onError = { },
    )

    internal val selectPaymentMethodViewState = CustomerSheetViewState.SelectPaymentMethod(
        title = null,
        savedPaymentMethods = listOf(CARD_PAYMENT_METHOD),
        paymentSelection = null,
        isLiveMode = false,
        isProcessing = false,
        isEditing = false,
        isGooglePayEnabled = false,
        primaryButtonVisible = false,
        primaryButtonLabel = null,
        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
    )

    internal val addPaymentMethodViewState = CustomerSheetViewState.AddPaymentMethod(
        paymentMethodCode = PaymentMethod.Type.Card.code,
        formViewData = FormViewModel.ViewData(
            completeFormValues = FormFieldValues(
                showsMandate = false,
                userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse,
            ),
        ),
        formArguments = FormArguments(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            showCheckbox = false,
            showCheckboxControlledFields = false,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            merchantName = ""
        ),
        usBankAccountFormArguments = usBankAccountFormArguments,
        supportedPaymentMethods = listOf(
            LpmRepository.HardcodedCard,
            LpmRepository.hardCodedUsBankAccount,
        ),
        selectedPaymentMethod = LpmRepository.HardcodedCard,
        enabled = true,
        isLiveMode = false,
        isProcessing = false,
        errorMessage = null,
        isFirstPaymentMethod = false,
        primaryButtonLabel = resolvableString(R.string.stripe_paymentsheet_save),
        primaryButtonEnabled = false,
        customPrimaryButtonUiState = null,
        bankAccountResult = null,
        draftPaymentSelection = null,
        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
    )

    internal fun mockedFormViewModel(
        configuration: CustomerSheet.Configuration,
        lpmRepository: LpmRepository,
    ): Provider<FormViewModelSubcomponent.Builder> {
        val formViewModel = FormViewModel(
            context = application,
            formArguments = FormArguments(
                PaymentMethod.Type.Card.code,
                showCheckbox = false,
                showCheckboxControlledFields = false,
                initialPaymentMethodCreateParams = null,
                merchantName = configuration.merchantDisplayName,
                billingDetails = configuration.defaultBillingDetails,
                billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration,
                cbcEligibility = CardBrandChoiceEligibility.Ineligible
            ),
            lpmRepository = lpmRepository,
            addressRepository = AddressRepository(
                resources = ApplicationProvider.getApplicationContext<Application>().resources,
                workContext = Dispatchers.Unconfined,
            ),
            showCheckboxFlow = mock()
        )
        val mockFormBuilder = mock<FormViewModelSubcomponent.Builder>()
        val mockFormSubcomponent = mock<FormViewModelSubcomponent>()
        val mockFormSubComponentBuilderProvider =
            mock<Provider<FormViewModelSubcomponent.Builder>>()
        whenever(mockFormBuilder.build()).thenReturn(mockFormSubcomponent)
        whenever(mockFormBuilder.formArguments(any())).thenReturn(mockFormBuilder)
        whenever(mockFormBuilder.showCheckboxFlow(any())).thenReturn(mockFormBuilder)
        whenever(mockFormSubcomponent.viewModel).thenReturn(formViewModel)
        whenever(mockFormSubComponentBuilderProvider.get()).thenReturn(mockFormBuilder)

        return mockFormSubComponentBuilderProvider
    }

    internal fun createViewModel(
        isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable = IsFinancialConnectionsAvailable { true },
        lpmRepository: LpmRepository = LpmRepository(
            LpmRepository.LpmRepositoryArguments(
                resources = application.resources,
                isFinancialConnectionsAvailable = isFinancialConnectionsAvailable,
            )
        ).apply {
            update(
                PaymentIntentFactory.create(
                    paymentMethodTypes = PaymentMethod.Type.values().map { it.code },
                ),
                null
            )
        },
        isLiveMode: Boolean = false,
        workContext: CoroutineContext = EmptyCoroutineContext,
        initialBackStack: List<CustomerSheetViewState> = listOf(
            CustomerSheetViewState.Loading(
                isLiveMode
            )
        ),
        isGooglePayAvailable: Boolean = true,
        customerPaymentMethods: List<PaymentMethod> = listOf(CARD_PAYMENT_METHOD),
        supportedPaymentMethods: List<LpmRepository.SupportedPaymentMethod> = listOf(
            LpmRepository.HardcodedCard,
            LpmRepository.hardCodedUsBankAccount,
        ),
        savedPaymentSelection: PaymentSelection? = null,
        stripeRepository: StripeRepository = FakeStripeRepository(),
        paymentConfiguration: PaymentConfiguration = PaymentConfiguration(
            publishableKey = "pk_test_123",
            stripeAccountId = null,
        ),
        configuration: CustomerSheet.Configuration = CustomerSheet.Configuration(
            merchantDisplayName = "Example",
            googlePayEnabled = isGooglePayAvailable
        ),
        formViewModelSubcomponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder> =
            mockedFormViewModel(configuration, lpmRepository),
        eventReporter: CustomerSheetEventReporter = mock(),
        intentConfirmationInterceptor: IntentConfirmationInterceptor = FakeIntentConfirmationInterceptor().apply {
            enqueueCompleteStep(true)
        },
        customerAdapter: CustomerAdapter = FakeCustomerAdapter(
            paymentMethods = CustomerAdapter.Result.success(customerPaymentMethods)
        ),
        customerSheetLoader: CustomerSheetLoader = FakeCustomerSheetLoader(
            customerPaymentMethods = customerPaymentMethods,
            supportedPaymentMethods = supportedPaymentMethods,
            paymentSelection = savedPaymentSelection,
            isGooglePayAvailable = isGooglePayAvailable,
        ),
        editInteractorFactory: ModifiableEditPaymentMethodViewInteractor.Factory =
            createModifiableEditPaymentMethodViewInteractorFactory(),
    ): CustomerSheetViewModel {
        return CustomerSheetViewModel(
            application = application,
            initialBackStack = initialBackStack,
            workContext = workContext,
            originalPaymentSelection = savedPaymentSelection,
            paymentConfigurationProvider = { paymentConfiguration },
            formViewModelSubcomponentBuilderProvider = formViewModelSubcomponentBuilderProvider,
            resources = application.resources,
            stripeRepository = stripeRepository,
            customerAdapter = customerAdapter,
            lpmRepository = lpmRepository,
            configuration = configuration,
            isLiveModeProvider = { isLiveMode },
            logger = Logger.noop(),
            intentConfirmationInterceptor = intentConfirmationInterceptor,
            paymentLauncherFactory = object : StripePaymentLauncherAssistedFactory {
                override fun create(
                    publishableKey: () -> String,
                    stripeAccountId: () -> String?,
                    statusBarColor: Int?,
                    includePaymentSheetAuthenticators: Boolean,
                    hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>
                ): StripePaymentLauncher {
                    return mock()
                }
            },
            statusBarColor = { null },
            eventReporter = eventReporter,
            customerSheetLoader = customerSheetLoader,
            isFinancialConnectionsAvailable = isFinancialConnectionsAvailable,
            editInteractorFactory = editInteractorFactory,
        ).apply {
            registerFromActivity(DummyActivityResultCaller(), TestLifecycleOwner())
        }
    }

    internal fun createModifiableEditPaymentMethodViewInteractorFactory(
        workContext: CoroutineContext = StandardTestDispatcher(),
    ): ModifiableEditPaymentMethodViewInteractor.Factory {
        return object : ModifiableEditPaymentMethodViewInteractor.Factory {
            override fun create(
                initialPaymentMethod: PaymentMethod,
                eventHandler: (EditPaymentMethodViewInteractor.Event) -> Unit,
                removeExecutor: PaymentMethodRemoveOperation,
                updateExecutor: PaymentMethodUpdateOperation,
                displayName: String
            ): ModifiableEditPaymentMethodViewInteractor {
                return DefaultEditPaymentMethodViewInteractor(
                    initialPaymentMethod = initialPaymentMethod,
                    displayName = "Card",
                    removeExecutor = removeExecutor,
                    updateExecutor = updateExecutor,
                    eventHandler = eventHandler,
                    workContext = workContext
                )
            }
        }
    }
}
