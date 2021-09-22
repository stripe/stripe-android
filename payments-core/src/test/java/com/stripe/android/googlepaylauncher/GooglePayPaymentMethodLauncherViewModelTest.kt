package com.stripe.android.googlepaylauncher

import androidx.lifecycle.SavedStateHandle
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentsClient
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.GooglePayConfig
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.model.GooglePayFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.ApiRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test

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

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        override suspend fun createPaymentMethod(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            options: ApiRequest.Options
        ): PaymentMethod {
            return PaymentMethodFixtures.CARD_PAYMENT_METHOD
        }
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
