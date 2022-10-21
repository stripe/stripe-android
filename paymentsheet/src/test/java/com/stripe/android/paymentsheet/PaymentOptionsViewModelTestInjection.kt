package com.stripe.android.paymentsheet

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.injection.PaymentOptionsViewModelSubcomponent
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.address.AddressRepository
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.StaticAddressResourceRepository
import com.stripe.android.ui.core.forms.resources.StaticLpmResourceRepository
import com.stripe.android.ui.core.injection.NonFallbackInjector
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import javax.inject.Provider

@ExperimentalCoroutinesApi
internal open class PaymentOptionsViewModelTestInjection {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val addressResourceRepository = StaticAddressResourceRepository(
        AddressRepository(ApplicationProvider.getApplicationContext<Context>().resources)
    )

    val eventReporter = mock<EventReporter>()

    private lateinit var injector: Injector

    @After
    open fun after() {
        WeakMapInjectorRegistry.clear()
    }

    @ExperimentalCoroutinesApi
    fun createViewModel(
        paymentMethods: List<PaymentMethod> = emptyList(),
        @InjectorKey injectorKey: String,
        args: PaymentOptionContract.Args = PaymentSheetFixtures.PAYMENT_OPTIONS_CONTRACT_ARGS
    ): PaymentOptionsViewModel = runBlocking {
        val lpmRepository =
            LpmRepository(LpmRepository.LpmRepositoryArguments(ApplicationProvider.getApplicationContext<Application>().resources))
        lpmRepository.forceUpdate(
            listOf(
                PaymentMethod.Type.Card.code,
                PaymentMethod.Type.USBankAccount.code
            ),
            null
        )
        PaymentOptionsViewModel(
            args,
            prefsRepositoryFactory = {
                FakePrefsRepository()
            },
            eventReporter = eventReporter,
            customerRepository = FakeCustomerRepository(paymentMethods),
            workContext = testDispatcher,
            application = ApplicationProvider.getApplicationContext(),
            logger = Logger.noop(),
            injectorKey = injectorKey,
            lpmResourceRepository = StaticLpmResourceRepository(lpmRepository),
            addressResourceRepository = addressResourceRepository,
            savedStateHandle = SavedStateHandle().apply {
                set(BaseSheetViewModel.SAVE_RESOURCE_REPOSITORY_READY, true)
            },
            linkLauncher = mock<LinkPaymentLauncher>().apply {
                whenever(getAccountStatusFlow(any())).thenReturn(flowOf(AccountStatus.Verified))
            }
        )
    }

    @FlowPreview
    fun registerViewModel(
        @InjectorKey injectorKey: String,
        viewModel: PaymentOptionsViewModel,
        lpmRepository: LpmRepository = mock(),
        formViewModel: FormViewModel = FormViewModel(
            context = context,
            formFragmentArguments = FormFragmentArguments(
                PaymentMethod.Type.Card.code,
                showCheckbox = true,
                showCheckboxControlledFields = true,
                merchantName = "Merchant, Inc.",
                amount = Amount(50, "USD"),
                initialPaymentMethodCreateParams = null
            ),
            lpmResourceRepository = StaticLpmResourceRepository(lpmRepository),
            addressResourceRepository = addressResourceRepository
        )
    ) {
        val mockBuilder = mock<PaymentOptionsViewModelSubcomponent.Builder>()
        val mockSubcomponent = mock<PaymentOptionsViewModelSubcomponent>()
        val mockSubComponentBuilderProvider =
            mock<Provider<PaymentOptionsViewModelSubcomponent.Builder>>()
        whenever(mockBuilder.build()).thenReturn(mockSubcomponent)
        whenever(mockBuilder.savedStateHandle(any())).thenReturn(mockBuilder)
        whenever(mockBuilder.application(any())).thenReturn(mockBuilder)
        whenever(mockBuilder.args(any())).thenReturn(mockBuilder)
        whenever(mockSubcomponent.viewModel).thenReturn(viewModel)
        whenever(mockSubComponentBuilderProvider.get()).thenReturn(mockBuilder)

        val mockFormBuilder = mock<FormViewModelSubcomponent.Builder>()
        val mockFormSubcomponent = mock<FormViewModelSubcomponent>()
        val mockFormSubComponentBuilderProvider =
            mock<Provider<FormViewModelSubcomponent.Builder>>()
        whenever(mockFormBuilder.build()).thenReturn(mockFormSubcomponent)
        whenever(mockFormBuilder.formFragmentArguments(any())).thenReturn(mockFormBuilder)
        whenever(mockFormSubcomponent.viewModel).thenReturn(formViewModel)
        whenever(mockFormSubComponentBuilderProvider.get()).thenReturn(mockFormBuilder)

        injector = object : NonFallbackInjector {
            override fun inject(injectable: Injectable<*>) {
                (injectable as? PaymentOptionsViewModel.Factory)?.let {
                    injectable.subComponentBuilderProvider = mockSubComponentBuilderProvider
                }
                (injectable as? FormViewModel.Factory)?.let {
                    injectable.subComponentBuilderProvider = mockFormSubComponentBuilderProvider
                }
            }
        }
        WeakMapInjectorRegistry.register(injector, injectorKey)
    }
}
