package com.stripe.android.paymentsheet

import android.app.Application
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContract
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.injection.PaymentSheetViewModelSubcomponent
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.uicore.address.AddressRepository
import com.stripe.android.utils.FakeCustomerRepository
import com.stripe.android.utils.FakeIntentConfirmationInterceptor
import com.stripe.android.utils.FakePaymentSheetLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import javax.inject.Provider

internal open class BasePaymentSheetViewModelInjectionTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val eventReporter = mock<EventReporter>()
    private val googlePayPaymentMethodLauncherFactory =
        createGooglePayPaymentMethodLauncherFactory()
    private val stripePaymentLauncherAssistedFactory = mock<StripePaymentLauncherAssistedFactory>()
    private val fakeIntentConfirmationInterceptor = FakeIntentConfirmationInterceptor()

    private lateinit var injector: NonFallbackInjector

    @After
    open fun after() {
        WeakMapInjectorRegistry.clear()
    }

    private fun createGooglePayPaymentMethodLauncherFactory() =
        object : GooglePayPaymentMethodLauncherFactory {
            override fun create(
                lifecycleScope: CoroutineScope,
                config: GooglePayPaymentMethodLauncher.Config,
                readyCallback: GooglePayPaymentMethodLauncher.ReadyCallback,
                activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContract.Args>,
                skipReadyCheck: Boolean
            ): GooglePayPaymentMethodLauncher {
                val googlePayPaymentMethodLauncher = mock<GooglePayPaymentMethodLauncher>()
                readyCallback.onReady(true)
                return googlePayPaymentMethodLauncher
            }
        }

    fun createViewModel(
        stripeIntent: StripeIntent,
        customerRepositoryPMs: List<PaymentMethod> = emptyList(),
        args: PaymentSheetContractV2.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
    ): PaymentSheetViewModel = runBlocking {
        TestViewModelFactory.create { linkHandler, linkInteractor, savedStateHandle ->
            PaymentSheetViewModel(
                ApplicationProvider.getApplicationContext(),
                args,
                eventReporter,
                { PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY) },
                StripeIntentValidator(),
                FakePaymentSheetLoader(
                    stripeIntent = stripeIntent,
                    customerPaymentMethods = customerRepositoryPMs,
                ),
                FakeCustomerRepository(customerRepositoryPMs),
                FakePrefsRepository(),
                lpmRepository = LpmRepository(
                    LpmRepository.LpmRepositoryArguments(
                        ApplicationProvider.getApplicationContext<Application>().resources
                    )
                ).apply {
                    this.update(
                        PaymentIntentFactory.create(
                            paymentMethodTypes = listOf(
                                PaymentMethod.Type.Card.code,
                                PaymentMethod.Type.USBankAccount.code
                            )
                        ),
                        null
                    )
                },
                stripePaymentLauncherAssistedFactory,
                googlePayPaymentMethodLauncherFactory,
                Logger.noop(),
                testDispatcher,
                savedStateHandle = savedStateHandle,
                linkHandler = linkHandler,
                linkConfigurationCoordinator = linkInteractor,
                intentConfirmationInterceptor = fakeIntentConfirmationInterceptor,
            )
        }
    }

    fun registerViewModel(
        @InjectorKey injectorKey: String,
        viewModel: PaymentSheetViewModel,
        lpmRepository: LpmRepository,
        formViewModel: FormViewModel = FormViewModel(
            context = context,
            formArguments = FormArguments(
                PaymentMethod.Type.Card.code,
                showCheckbox = true,
                showCheckboxControlledFields = true,
                merchantName = "Merchant, Inc.",
                amount = Amount(50, "USD"),
                initialPaymentMethodCreateParams = null
            ),
            lpmRepository = lpmRepository,
            addressRepository = createAddressRepository(),
            showCheckboxFlow = mock()
        )
    ) {
        injector = object : NonFallbackInjector {
            override fun inject(injectable: Injectable<*>) {
                (injectable as? PaymentSheetViewModel.Factory)?.let {
                    val mockBuilder = mock<PaymentSheetViewModelSubcomponent.Builder>()
                    val mockSubcomponent = mock<PaymentSheetViewModelSubcomponent>()
                    val mockSubComponentBuilderProvider =
                        mock<Provider<PaymentSheetViewModelSubcomponent.Builder>>()

                    whenever(mockBuilder.build()).thenReturn(mockSubcomponent)
                    whenever(mockBuilder.savedStateHandle(any())).thenReturn(mockBuilder)
                    whenever(mockBuilder.paymentSheetViewModelModule(any())).thenReturn(mockBuilder)
                    whenever(mockSubcomponent.viewModel).thenReturn(viewModel)
                    whenever(mockSubComponentBuilderProvider.get()).thenReturn(mockBuilder)
                    injectable.subComponentBuilderProvider = mockSubComponentBuilderProvider
                }
                (injectable as? FormViewModel.Factory)?.let {
                    val mockBuilder = mock<FormViewModelSubcomponent.Builder>()
                    val mockSubcomponent = mock<FormViewModelSubcomponent>()
                    val mockSubComponentBuilderProvider =
                        mock<Provider<FormViewModelSubcomponent.Builder>>()

                    whenever(mockBuilder.formArguments(any())).thenReturn(mockBuilder)
                    whenever(mockBuilder.showCheckboxFlow(any())).thenReturn(mockBuilder)
                    whenever(mockBuilder.build()).thenReturn(mockSubcomponent)
                    whenever(mockBuilder.formArguments(any())).thenReturn(mockBuilder)
                    whenever(mockSubcomponent.viewModel).thenReturn(formViewModel)
                    whenever(mockSubComponentBuilderProvider.get()).thenReturn(mockBuilder)
                    injectable.subComponentBuilderProvider = mockSubComponentBuilderProvider
                }
            }
        }
        viewModel.injector = injector
        WeakMapInjectorRegistry.register(injector, injectorKey)
    }
}

private fun createAddressRepository(): AddressRepository {
    return AddressRepository(
        resources = ApplicationProvider.getApplicationContext<Application>().resources,
        workContext = Dispatchers.Unconfined,
    )
}
