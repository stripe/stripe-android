package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultLauncher
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContract
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.PaymentSheetViewModelSubcomponent
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.repositories.StripeIntentRepository
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.After
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import javax.inject.Provider

internal open class PaymentSheetViewModelTestInjection {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()

    val eventReporter = mock<EventReporter>()
    private val googlePayPaymentMethodLauncherFactory =
        createGooglePayPaymentMethodLauncherFactory()
    private val stripePaymentLauncherAssistedFactory =
        mock<StripePaymentLauncherAssistedFactory>()

    @After
    open fun after() {
        println("Clear weakMapInjectorRegistry")
        WeakMapInjectorRegistry.staticCacheMap.clear()
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

    @ExperimentalCoroutinesApi
    fun createViewModel(
        stripeIntent: StripeIntent,
        paymentMethods: List<PaymentMethod> = emptyList(),
        @InjectorKey injectorKey: String,
        args: PaymentSheetContract.Args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY
    ): PaymentSheetViewModel = runBlocking {
        PaymentSheetViewModel(
            ApplicationProvider.getApplicationContext(),
            args,
            eventReporter,
            { PaymentConfiguration(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY) },
            StripeIntentRepository.Static(stripeIntent),
            StripeIntentValidator(),
            FakeCustomerRepository(paymentMethods),
            FakePrefsRepository(),
            resourceRepository = mock(),
            stripePaymentLauncherAssistedFactory,
            googlePayPaymentMethodLauncherFactory,
            Logger.noop(),
            testDispatcher,
            injectorKey,
            savedStateHandle = SavedStateHandle().apply {
                set(BaseSheetViewModel.SAVE_RESOURCE_REPOSITORY_READY, true)
            }
        )
    }

    fun registerViewModel(
        viewModel: PaymentSheetViewModel,
        @InjectorKey injectorKey: String
    ) {
        val mockBuilder = mock<PaymentSheetViewModelSubcomponent.Builder>()
        val mockSubcomponent = mock<PaymentSheetViewModelSubcomponent>()
        val mockSubComponentBuilderProvider = mock<Provider<PaymentSheetViewModelSubcomponent.Builder>>()

        whenever(mockBuilder.build()).thenReturn(mockSubcomponent)
        whenever(mockBuilder.savedStateHandle(any())).thenReturn(mockBuilder)
        whenever(mockBuilder.paymentSheetViewModelModule(any())).thenReturn(mockBuilder)
        whenever(mockSubcomponent.viewModel).thenReturn(viewModel)
        whenever(mockSubComponentBuilderProvider.get()).thenReturn(mockBuilder)

        val injector = object : Injector {
            override fun inject(injectable: Injectable<*>) {
                val factory = injectable as PaymentSheetViewModel.Factory
                factory.subComponentBuilderProvider = mockSubComponentBuilderProvider
            }
        }
        WeakMapInjectorRegistry.register(injector, injectorKey)
    }

}
