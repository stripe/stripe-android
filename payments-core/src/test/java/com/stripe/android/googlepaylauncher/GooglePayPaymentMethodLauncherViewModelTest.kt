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
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.GooglePayFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.fakeCreationExtras
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class GooglePayPaymentMethodLauncherViewModelTest {

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

    @Test
    fun `createPaymentMethod() should return expected result`() = runTest {
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
    fun `createTransactionInfo() with 0 amount in US and CA should expect TotalPriceStatus NOT_CURRENTLY_KNOWN`() {
        for (countryCode in listOf("us", "ca")) {
            val transactionInfo = viewModel.createTransactionInfo(
                GooglePayPaymentMethodLauncherContractV2.Args(
                    GooglePayPaymentMethodLauncher.Config(
                        GooglePayEnvironment.Test,
                        merchantCountryCode = countryCode,
                        merchantName = "Widget, Inc."
                    ),
                    currencyCode = "usd",
                    amount = 0
                )
            )
            assertThat(transactionInfo)
                .isEqualTo(
                    GooglePayJsonFactory.TransactionInfo(
                        currencyCode = "usd",
                        totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.NotCurrentlyKnown,
                        countryCode = countryCode,
                        transactionId = null,
                        totalPrice = null,
                        checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default
                    )
                )
        }
    }

    @Test
    fun `createTransactionInfo() with 0 amount outside US and CA should honor the price`() {
        for (countryCode in listOf("de", "fr", "gb", "jp", "mx")) {
            val transactionInfo = viewModel.createTransactionInfo(
                GooglePayPaymentMethodLauncherContractV2.Args(
                    GooglePayPaymentMethodLauncher.Config(
                        GooglePayEnvironment.Test,
                        merchantCountryCode = countryCode,
                        merchantName = "Widget, Inc."
                    ),
                    currencyCode = "usd",
                    amount = 0
                )
            )
            assertThat(transactionInfo)
                .isEqualTo(
                    GooglePayJsonFactory.TransactionInfo(
                        currencyCode = "usd",
                        totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
                        countryCode = countryCode,
                        transactionId = null,
                        totalPrice = 0,
                        checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default
                    )
                )
        }
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
    fun `Factory gets initialized with fallback when no Injector is available`() {
        scenario.onFragment { fragment ->
            val application = ApplicationProvider.getApplicationContext<Application>()
            val publishableKey = "publishable_key"
            PaymentConfiguration.init(application, publishableKey)

            val factory = GooglePayPaymentMethodLauncherViewModel.Factory(
                GooglePayPaymentMethodLauncherContractV2.Args(
                    config = GooglePayPaymentMethodLauncher.Config(
                        GooglePayEnvironment.Test,
                        "US",
                        "merchant"
                    ),
                    currencyCode = "usd",
                    amount = 1099,
                    label = null,
                    transactionId = null,
                )
            )

            val factorySpy = spy(factory)

            assertNotNull(
                factorySpy.create(
                    modelClass = GooglePayPaymentMethodLauncherViewModel::class.java,
                    extras = fragment.fakeCreationExtras(),
                )
            )
        }
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        override suspend fun createPaymentMethod(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            options: ApiRequest.Options,
        ): Result<PaymentMethod> {
            return Result.success(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
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
        val ARGS = GooglePayPaymentMethodLauncherContractV2.Args(
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
