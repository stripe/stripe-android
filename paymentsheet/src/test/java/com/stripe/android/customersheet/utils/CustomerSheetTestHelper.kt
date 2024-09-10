package com.stripe.android.customersheet.utils

import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetLoader
import com.stripe.android.customersheet.CustomerSheetViewModel
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.FakeCustomerAdapter
import com.stripe.android.customersheet.FakeStripeRepository
import com.stripe.android.customersheet.analytics.CustomerSheetEventReporter
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContractV2
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.lpmfoundations.luxe.LpmRepositoryTestHelpers
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.IntentConfirmationHandler
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
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import org.mockito.kotlin.mock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalCustomerSheetApi::class)
internal object CustomerSheetTestHelper {
    internal val application = ApplicationProvider.getApplicationContext<Application>()

    internal fun createViewModel(
        isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable = IsFinancialConnectionsAvailable { true },
        isLiveMode: Boolean = false,
        workContext: CoroutineContext = EmptyCoroutineContext,
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
            workContext = workContext,
            originalPaymentSelection = savedPaymentSelection,
            paymentConfigurationProvider = { paymentConfiguration },
            customerAdapterProvider = CompletableDeferred(customerAdapter),
            resources = application.resources,
            stripeRepository = stripeRepository,
            configuration = configuration,
            isLiveModeProvider = { isLiveMode },
            logger = Logger.noop(),
            intentConfirmationHandlerFactory = IntentConfirmationHandler.Factory(
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
                        skipReadyCheck: Boolean
                    ): GooglePayPaymentMethodLauncher = mock()
                },
                statusBarColor = { null },
                savedStateHandle = SavedStateHandle(),
                errorReporter = FakeErrorReporter(),
                logger = FakeUserFacingLogger(),
            ),
            eventReporter = eventReporter,
            customerSheetLoader = customerSheetLoader,
            isFinancialConnectionsAvailable = isFinancialConnectionsAvailable,
            editInteractorFactory = editInteractorFactory,
            errorReporter = FakeErrorReporter(),
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
                )
            }
        }
    }
}
