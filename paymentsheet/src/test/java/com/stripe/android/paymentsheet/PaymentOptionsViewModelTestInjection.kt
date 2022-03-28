package com.stripe.android.paymentsheet

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.injection.PaymentOptionsViewModelSubcomponent
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val testDispatcher = UnconfinedTestDispatcher()

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
            resourceRepository = mock(),
            savedStateHandle = SavedStateHandle().apply {
                set(BaseSheetViewModel.SAVE_RESOURCE_REPOSITORY_READY, true)
            }
        )
    }

    @ExperimentalCoroutinesApi
    fun createFormViewModel(): FormViewModel = runBlocking {
        FormViewModel(
            layout = mock(),
            config = mock(),
            resourceRepository = mock(),
            transformSpecToElement = mock()
        )
    }

    fun registerViewModel(
        @InjectorKey injectorKey: String,
        viewModel: PaymentOptionsViewModel,
        formViewModel: FormViewModel
    ) {
        val mockBuilder = mock<PaymentOptionsViewModelSubcomponent.Builder>()
        val mockSubcomponent = mock<PaymentOptionsViewModelSubcomponent>()
        val mockSubComponentBuilderProvider = mock<Provider<PaymentOptionsViewModelSubcomponent.Builder>>()
        whenever(mockBuilder.build()).thenReturn(mockSubcomponent)
        whenever(mockBuilder.savedStateHandle(any())).thenReturn(mockBuilder)
        whenever(mockBuilder.application(any())).thenReturn(mockBuilder)
        whenever(mockBuilder.args(any())).thenReturn(mockBuilder)
        whenever(mockSubcomponent.viewModel).thenReturn(viewModel)
        whenever(mockSubComponentBuilderProvider.get()).thenReturn(mockBuilder)

        val mockFormBuilder = mock<FormViewModelSubcomponent.Builder>()
        val mockFormSubcomponent = mock<FormViewModelSubcomponent>()
        val mockFormSubComponentBuilderProvider = mock<Provider<FormViewModelSubcomponent.Builder>>()
        whenever(mockFormBuilder.build()).thenReturn(mockFormSubcomponent)
        whenever(mockFormBuilder.formFragmentArguments(any())).thenReturn(mockFormBuilder)
        whenever(mockFormBuilder.layout(any())).thenReturn(mockFormBuilder)
        whenever(mockFormSubcomponent.viewModel).thenReturn(formViewModel)
        whenever(mockFormSubComponentBuilderProvider.get()).thenReturn(mockFormBuilder)

        injector = object : Injector {
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
