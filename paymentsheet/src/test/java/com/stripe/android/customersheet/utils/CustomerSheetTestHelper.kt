package com.stripe.android.customersheet.utils

import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.CardBrandFilter
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.CustomerPermissions
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetIntegration
import com.stripe.android.customersheet.CustomerSheetLoader
import com.stripe.android.customersheet.CustomerSheetViewModel
import com.stripe.android.customersheet.FakeStripeRepository
import com.stripe.android.customersheet.analytics.CustomerSheetEventReporter
import com.stripe.android.customersheet.data.CustomerSheetDataResult
import com.stripe.android.customersheet.data.CustomerSheetIntentDataSource
import com.stripe.android.customersheet.data.CustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.CustomerSheetSavedSelectionDataSource
import com.stripe.android.customersheet.data.FakeCustomerSheetIntentDataSource
import com.stripe.android.customersheet.data.FakeCustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.FakeCustomerSheetSavedSelectionDataSource
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.DefaultConfirmationHandler
import com.stripe.android.paymentsheet.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.FakeBacsMandateConfirmationLauncher
import com.stripe.android.paymentsheet.ui.DefaultEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.PaymentMethodRemoveOperation
import com.stripe.android.paymentsheet.ui.PaymentMethodUpdateOperation
import com.stripe.android.paymentsheet.utils.FakeUserFacingLogger
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.utils.CompletableSingle
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import org.mockito.kotlin.mock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal object CustomerSheetTestHelper {
    internal val application = ApplicationProvider.getApplicationContext<Application>()

    internal fun createViewModel(
        isLiveMode: Boolean = false,
        workContext: CoroutineContext = EmptyCoroutineContext,
        integrationType: CustomerSheetIntegration.Type = CustomerSheetIntegration.Type.CustomerAdapter,
        isGooglePayAvailable: Boolean = true,
        customerPaymentMethods: List<PaymentMethod> = listOf(CARD_PAYMENT_METHOD),
        customerPermissions: CustomerPermissions = CustomerPermissions(
            canRemovePaymentMethods = true,
        ),
        cbcEligibility: CardBrandChoiceEligibility = CardBrandChoiceEligibility.Ineligible,
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
        paymentMethodDataSource: CustomerSheetPaymentMethodDataSource = FakeCustomerSheetPaymentMethodDataSource(
            paymentMethods = CustomerSheetDataResult.success(customerPaymentMethods)
        ),
        intentDataSource: CustomerSheetIntentDataSource = FakeCustomerSheetIntentDataSource(),
        savedSelectionDataSource: CustomerSheetSavedSelectionDataSource = FakeCustomerSheetSavedSelectionDataSource(),
        customerSheetLoader: CustomerSheetLoader = FakeCustomerSheetLoader(
            customerPaymentMethods = customerPaymentMethods,
            supportedPaymentMethods = supportedPaymentMethods,
            paymentSelection = savedPaymentSelection,
            isGooglePayAvailable = isGooglePayAvailable,
            cbcEligibility = cbcEligibility,
            permissions = customerPermissions,
        ),
        editInteractorFactory: ModifiableEditPaymentMethodViewInteractor.Factory =
            createModifiableEditPaymentMethodViewInteractorFactory(),
        errorReporter: ErrorReporter = FakeErrorReporter(),
    ): CustomerSheetViewModel {
        return CustomerSheetViewModel(
            application = application,
            workContext = workContext,
            originalPaymentSelection = savedPaymentSelection,
            paymentConfigurationProvider = { paymentConfiguration },
            paymentMethodDataSourceProvider = CompletableSingle(paymentMethodDataSource),
            intentDataSourceProvider = CompletableSingle(intentDataSource),
            savedSelectionDataSourceProvider = CompletableSingle(savedSelectionDataSource),
            stripeRepository = stripeRepository,
            configuration = configuration,
            integrationType = integrationType,
            isLiveModeProvider = { isLiveMode },
            logger = Logger.noop(),
            confirmationHandlerFactory = DefaultConfirmationHandler.Factory(
                intentConfirmationInterceptor = intentConfirmationInterceptor,
                paymentConfigurationProvider = { paymentConfiguration },
                bacsMandateConfirmationLauncherFactory = {
                    FakeBacsMandateConfirmationLauncher()
                },
                stripePaymentLauncherAssistedFactory = object : StripePaymentLauncherAssistedFactory {
                    override fun create(
                        publishableKey: () -> String,
                        stripeAccountId: () -> String?,
                        statusBarColor: Int?,
                        includePaymentSheetNextHandlers: Boolean,
                        hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>
                    ): StripePaymentLauncher {
                        return mock()
                    }
                },
                googlePayPaymentMethodLauncherFactory = object : GooglePayPaymentMethodLauncherFactory {
                    override fun create(
                        lifecycleScope: CoroutineScope,
                        config: GooglePayPaymentMethodLauncher.Config,
                        readyCallback: GooglePayPaymentMethodLauncher.ReadyCallback,
                        activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
                        skipReadyCheck: Boolean,
                        cardBrandFilter: CardBrandFilter
                    ): GooglePayPaymentMethodLauncher = mock()
                },
                statusBarColor = { null },
                savedStateHandle = SavedStateHandle(),
                errorReporter = FakeErrorReporter(),
                logger = FakeUserFacingLogger(),
            ),
            eventReporter = eventReporter,
            customerSheetLoader = customerSheetLoader,
            editInteractorFactory = editInteractorFactory,
            errorReporter = errorReporter,
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
                displayName: ResolvableString,
                canRemove: Boolean,
                isLiveMode: Boolean,
                cardBrandFilter: CardBrandFilter
            ): ModifiableEditPaymentMethodViewInteractor {
                return DefaultEditPaymentMethodViewInteractor(
                    initialPaymentMethod = initialPaymentMethod,
                    displayName = "Card".resolvableString,
                    removeExecutor = removeExecutor,
                    updateExecutor = updateExecutor,
                    eventHandler = eventHandler,
                    workContext = workContext,
                    canRemove = canRemove,
                    isLiveMode = isLiveMode,
                    cardBrandFilter = cardBrandFilter
                )
            }
        }
    }
}
