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
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
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
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.ui.DefaultEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.PaymentMethodRemoveOperation
import com.stripe.android.paymentsheet.ui.PaymentMethodUpdateOperation
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import org.mockito.kotlin.mock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalCustomerSheetApi::class)
internal object CustomerSheetTestHelper {
    internal val application = ApplicationProvider.getApplicationContext<Application>()

    internal val usBankAccountFormArguments = USBankAccountFormArguments(
        showCheckbox = false,
        instantDebits = false,
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
        allowsRemovalOfLastSavedPaymentMethod = true,
    )

    internal val addPaymentMethodViewState = CustomerSheetViewState.AddPaymentMethod(
        paymentMethodCode = PaymentMethod.Type.Card.code,
        formFieldValues = FormFieldValues(
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse,
        ),
        formElements = emptyList(),
        formArguments = FormArguments(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            merchantName = ""
        ),
        usBankAccountFormArguments = usBankAccountFormArguments,
        supportedPaymentMethods = listOf(
            LpmRepositoryTestHelpers.card,
            LpmRepositoryTestHelpers.usBankAccount,
        ),
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

    internal fun createViewModel(
        isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable = IsFinancialConnectionsAvailable { true },
        isLiveMode: Boolean = false,
        workContext: CoroutineContext = EmptyCoroutineContext,
        initialBackStack: List<CustomerSheetViewState> = listOf(
            CustomerSheetViewState.Loading(
                isLiveMode
            )
        ),
        isGooglePayAvailable: Boolean = true,
        customerPaymentMethods: List<PaymentMethod> = listOf(CARD_PAYMENT_METHOD),
        supportedPaymentMethods: List<SupportedPaymentMethod> = listOf(
            LpmRepositoryTestHelpers.card,
            LpmRepositoryTestHelpers.usBankAccount,
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
            customerAdapterProvider = CompletableDeferred(customerAdapter),
            resources = application.resources,
            stripeRepository = stripeRepository,
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
            statusBarColor = null,
            eventReporter = eventReporter,
            customerSheetLoader = customerSheetLoader,
            isFinancialConnectionsAvailable = isFinancialConnectionsAvailable,
            editInteractorFactory = editInteractorFactory,
        ).apply {
            registerFromActivity(DummyActivityResultCaller(), TestLifecycleOwner())

            paymentMethodMetadata = PaymentMethodMetadataFactory.create()
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
                displayName: String,
                canRemove: Boolean,
            ): ModifiableEditPaymentMethodViewInteractor {
                return DefaultEditPaymentMethodViewInteractor(
                    initialPaymentMethod = initialPaymentMethod,
                    displayName = "Card",
                    removeExecutor = removeExecutor,
                    updateExecutor = updateExecutor,
                    eventHandler = eventHandler,
                    workContext = workContext,
                    canRemove = canRemove,
                )
            }
        }
    }
}
