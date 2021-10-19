package com.stripe.android.googlepaylauncher

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentsClient
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.GooglePayConfig
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.PaymentConfiguration
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherViewModelSubcomponent
import com.stripe.android.model.GooglePayFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.payments.core.injection.Injectable
import com.stripe.android.payments.core.injection.Injector
import com.stripe.android.payments.core.injection.WeakMapInjectorRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class GooglePayPaymentMethodLauncherViewModelTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val stripeRepository = FakeStripeRepository()
    private val googlePayJsonFactory = GooglePayJsonFactory(
        googlePayConfig = GooglePayConfig(
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "account"
        )
    )

    private val googlePayRepository = FakeGooglePayRepository(true)

    private val task = mock<Task<PaymentData>>()
    private val paymentsClient = mock<PaymentsClient>().also {
        whenever(it.loadPaymentData(any()))
            .thenReturn(task)
    }

    private val scenario = launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
        TestFragment()
    }

    private val viewModel = GooglePayPaymentMethodLauncherViewModel(
        paymentsClient,
        REQUEST_OPTIONS,
        ARGS,
        stripeRepository,
        googlePayJsonFactory,
        googlePayRepository,
        SavedStateHandle()
    )

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `createPaymentMethod() should return expected result`() = testDispatcher.runBlockingTest {
        val result = viewModel.createPaymentMethod(
            PaymentData.fromJson(
                GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS.toString()
            )
        )
        assertThat(result)
            .isEqualTo(
                GooglePayPaymentMethodLauncher.Result.Completed(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                )
            )
    }

    @Test
    fun `createTransactionInfo() with amount should create expected TransactionInfo`() {
        val transactionInfo = viewModel.createTransactionInfo(ARGS)
        assertThat(transactionInfo)
            .isEqualTo(
                GooglePayJsonFactory.TransactionInfo(
                    currencyCode = "usd",
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
                    countryCode = "us",
                    transactionId = null,
                    totalPrice = 1000,
                    checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default
                )
            )
    }

    @Test
    fun `createTransactionInfo() with 0 amount should create expected TransactionInfo`() {
        val transactionInfo = viewModel.createTransactionInfo(ARGS.copy(amount = 0))
        assertThat(transactionInfo)
            .isEqualTo(
                GooglePayJsonFactory.TransactionInfo(
                    currencyCode = "usd",
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
                    countryCode = "us",
                    transactionId = null,
                    totalPrice = 0,
                    checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default
                )
            )
    }

    @Test
    fun `createTransactionInfo() with transactionId should create expected TransactionInfo`() {
        val transactionId = "test_id"
        val transactionInfo =
            viewModel.createTransactionInfo(ARGS.copy(transactionId = transactionId))
        assertThat(transactionInfo)
            .isEqualTo(
                GooglePayJsonFactory.TransactionInfo(
                    currencyCode = "usd",
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
                    countryCode = "us",
                    transactionId = transactionId,
                    totalPrice = 1000,
                    checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default
                )
            )
    }

    @Test
    fun `Factory gets initialized by Injector when Injector is available`() {
        scenario.onFragment { fragment ->
            val mockBuilder = mock<GooglePayPaymentMethodLauncherViewModelSubcomponent.Builder>()
            val mockSubcomponent = mock<GooglePayPaymentMethodLauncherViewModelSubcomponent>()

            whenever(mockBuilder.build()).thenReturn(mockSubcomponent)
            whenever(mockBuilder.savedStateHandle(any())).thenReturn(mockBuilder)
            whenever(mockBuilder.args(any())).thenReturn(mockBuilder)
            whenever(mockSubcomponent.viewModel).thenReturn(viewModel)

            val injector = object : Injector {
                override fun inject(injectable: Injectable<*>) {
                    val factory = injectable as GooglePayPaymentMethodLauncherViewModel.Factory
                    factory.subComponentBuilder = mockBuilder
                }
            }
            val injectorKey = "testInjectorKey"
            WeakMapInjectorRegistry.register(injector, injectorKey)
            val factory = GooglePayPaymentMethodLauncherViewModel.Factory(
                ApplicationProvider.getApplicationContext(),
                GooglePayPaymentMethodLauncherContract.Args(
                    mock(),
                    "usd",
                    1099,
                    null,
                    GooglePayPaymentMethodLauncherContract.Args.InjectionParams(
                        injectorKey,
                        emptySet(),
                        false,
                        "key",
                        null
                    )
                ),
                fragment
            )
            val factorySpy = spy(factory)
            val createdViewModel =
                factorySpy.create(GooglePayPaymentMethodLauncherViewModel::class.java)
            verify(factorySpy, times(0)).fallbackInitialize(any())
            assertThat(createdViewModel).isEqualTo(viewModel)

            WeakMapInjectorRegistry.staticCacheMap.clear()
        }
    }

    @Test
    fun `Factory gets initialized with fallback when no Injector is available`() {
        scenario.onFragment { fragment ->
            val context = ApplicationProvider.getApplicationContext<Application>()
            val productUsage = setOf("TestProductUsage")
            val publishableKey = "publishable_key"
            PaymentConfiguration.init(context, publishableKey)

            val factory = GooglePayPaymentMethodLauncherViewModel.Factory(
                context,
                GooglePayPaymentMethodLauncherContract.Args(
                    GooglePayPaymentMethodLauncher.Config(
                        GooglePayEnvironment.Test,
                        "US",
                        "merchant"
                    ),
                    "usd",
                    1099,
                    null,
                    GooglePayPaymentMethodLauncherContract.Args.InjectionParams(
                        DUMMY_INJECTOR_KEY,
                        productUsage,
                        false,
                        publishableKey,
                        null
                    )
                ),
                fragment
            )
            val factorySpy = spy(factory)
            assertNotNull(factorySpy.create(GooglePayPaymentMethodLauncherViewModel::class.java))
            verify(factorySpy).fallbackInitialize(
                argWhere {
                    it.application == context &&
                        it.productUsage == productUsage &&
                        it.publishableKey == publishableKey
                }
            )
        }
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        override suspend fun createPaymentMethod(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            options: ApiRequest.Options
        ): PaymentMethod {
            return PaymentMethodFixtures.CARD_PAYMENT_METHOD
        }
    }

    internal class TestFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View = FrameLayout(inflater.context)
    }

    private companion object {
        val ARGS = GooglePayPaymentMethodLauncherContract.Args(
            GooglePayPaymentMethodLauncher.Config(
                GooglePayEnvironment.Test,
                merchantCountryCode = "us",
                merchantName = "Widget, Inc."
            ),
            currencyCode = "usd",
            amount = 1000
        )
        val REQUEST_OPTIONS = ApiRequest.Options(
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "account"
        )
    }
}
