package com.stripe.android.googlepaylauncher

import android.content.Intent
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentsClient
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.GooglePayConfig
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.PaymentController
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.StripePaymentController
import com.stripe.android.exception.StripeException
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.authentication.AbsPaymentController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.lang.Exception
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class GooglePayLauncherViewModelTest {
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

    private val viewModel = createViewModel()

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `isReadyToPay() should return expected value`() = testDispatcher.runBlockingTest {
        assertThat(viewModel.isReadyToPay())
            .isTrue()
    }

    @Test
    fun `createLoadPaymentDataTask() should throw expected exception when Google Pay is not available`() =
        testDispatcher.runBlockingTest {
            googlePayRepository.value = false

            val error = assertFailsWith<IllegalStateException> {
                viewModel.createLoadPaymentDataTask()
            }
            assertThat(error.message)
                .isEqualTo("Google Pay is unavailable.")
        }

    @Test
    fun `createLoadPaymentDataTask() should return task when Google Pay is available`() =
        testDispatcher.runBlockingTest {
            assertThat(viewModel.createLoadPaymentDataTask())
                .isNotNull()
        }

    @Test
    fun `createTransactionInfo() with PaymentIntent should return expected TransactionInfo`() {
        val transactionInfo = viewModel.createTransactionInfo(
            PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.currency.orEmpty()
        )
        assertThat(transactionInfo)
            .isEqualTo(
                GooglePayJsonFactory.TransactionInfo(
                    currencyCode = "usd",
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                    countryCode = "us",
                    transactionId = "pi_1F7J1aCRMbs6FrXfaJcvbxF6",
                    totalPrice = 1099,
                    totalPriceLabel = null,
                    checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.CompleteImmediatePurchase
                )
            )
    }

    @Test
    fun `createTransactionInfo() with SetupIntent should return expected TransactionInfo`() {
        val transactionInfo = viewModel.createTransactionInfo(
            SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            "usd"
        )
        assertThat(transactionInfo)
            .isEqualTo(
                GooglePayJsonFactory.TransactionInfo(
                    currencyCode = "usd",
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
                    countryCode = "us",
                    transactionId = "seti_1GSmaFCRMbs",
                    totalPrice = 0,
                    totalPriceLabel = null,
                    checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default
                )
            )
    }

    @Test
    fun `getResultFromConfirmation() using PaymentIntent should return expected result`() =
        testDispatcher.runBlockingTest {
            val result = viewModel.getResultFromConfirmation(
                StripePaymentController.PAYMENT_REQUEST_CODE,
                Intent()
                    .putExtras(
                        PaymentFlowResult.Unvalidated(
                            clientSecret = "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s"
                        ).toBundle()
                    )
            )
            assertThat(result)
                .isEqualTo(GooglePayLauncher.Result.Completed)
        }

    @Test
    fun `getResultFromConfirmation() using SetupIntent should return expected result`() =
        testDispatcher.runBlockingTest {
            val result = viewModel.getResultFromConfirmation(
                StripePaymentController.SETUP_REQUEST_CODE,
                Intent()
                    .putExtras(
                        PaymentFlowResult.Unvalidated(
                            clientSecret = "seti_1GSmaFCRMbs6FrXfmjThcHan_secret_H0oC2iSB4FtW4d"
                        ).toBundle()
                    )
            )
            assertThat(result)
                .isEqualTo(GooglePayLauncher.Result.Completed)
        }

    @Test
    fun `getResultFromConfirmation() with failed confirmation should return expected result`() =
        testDispatcher.runBlockingTest {
            val exception = StripeException.create(Exception("Failure"))
            val result = viewModel.getResultFromConfirmation(
                StripePaymentController.PAYMENT_REQUEST_CODE,
                Intent()
                    .putExtras(
                        PaymentFlowResult.Unvalidated(
                            clientSecret = "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s",
                            exception = exception
                        ).toBundle()
                    )
            )
            assertThat(result)
                .isEqualTo(GooglePayLauncher.Result.Failed(exception))
        }

    @Test
    fun `confirmStripeIntent() using PaymentIntent should confirm Payment Intent`() =
        testDispatcher.runBlockingTest {
            val mockPaymentController: PaymentController = mock()
            createViewModel(paymentController = mockPaymentController)
                .confirmStripeIntent(mock(), mock())

            val argumentCaptor: KArgumentCaptor<ConfirmStripeIntentParams> = argumentCaptor()
            verify(mockPaymentController)
                .startConfirmAndAuth(any(), argumentCaptor.capture(), any())
            assertThat(argumentCaptor.firstValue)
                .isInstanceOf(ConfirmPaymentIntentParams::class.java)
        }

    @Test
    fun `confirmStripeIntent() using SetupIntent should confirm Setup Intent`() =
        testDispatcher.runBlockingTest {
            val mockPaymentController: PaymentController = mock()
            createViewModel(
                args = GooglePayLauncherContract.SetupIntentArgs(
                    "pi_123_secret_456",
                    GOOGLE_PAY_CONFIG,
                    "USD"
                ),
                paymentController = mockPaymentController
            ).confirmStripeIntent(mock(), mock())

            val argumentCaptor: KArgumentCaptor<ConfirmStripeIntentParams> = argumentCaptor()
            verify(mockPaymentController)
                .startConfirmAndAuth(any(), argumentCaptor.capture(), any())
            assertThat(argumentCaptor.firstValue)
                .isInstanceOf(ConfirmSetupIntentParams::class.java)
        }

    private fun createViewModel(
        args: GooglePayLauncherContract.Args = ARGS,
        paymentController: PaymentController = FakePaymentController()
    ) = GooglePayLauncherViewModel(
        paymentsClient,
        REQUEST_OPTIONS,
        args,
        stripeRepository,
        paymentController,
        googlePayJsonFactory,
        googlePayRepository
    )

    private class FakePaymentController : AbsPaymentController() {
        override fun shouldHandlePaymentResult(
            requestCode: Int,
            data: Intent?
        ): Boolean {
            return requestCode == StripePaymentController.PAYMENT_REQUEST_CODE
        }

        override fun shouldHandleSetupResult(
            requestCode: Int,
            data: Intent?
        ): Boolean {
            return requestCode == StripePaymentController.SETUP_REQUEST_CODE
        }

        override suspend fun getPaymentIntentResult(
            data: Intent
        ): PaymentIntentResult {
            val paymentFlowResult = PaymentFlowResult.Unvalidated.fromIntent(data).validate()
            return PaymentIntentResult(
                PaymentIntentFixtures.PI_SUCCEEDED,
                outcomeFromFlow = paymentFlowResult.flowOutcome
            )
        }

        override suspend fun getSetupIntentResult(data: Intent): SetupIntentResult {
            val paymentFlowResult = PaymentFlowResult.Unvalidated.fromIntent(data).validate()
            return SetupIntentResult(
                SetupIntentFixtures.SI_SUCCEEDED,
                outcomeFromFlow = paymentFlowResult.flowOutcome
            )
        }
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        override suspend fun retrievePaymentIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ): PaymentIntent {
            return PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        }

        override suspend fun retrieveSetupIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ): SetupIntent {
            return SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD
        }
    }

    private companion object {
        val GOOGLE_PAY_CONFIG = GooglePayLauncher.Config(
            GooglePayEnvironment.Test,
            merchantCountryCode = "us",
            merchantName = "Widget, Inc."
        )
        val ARGS = GooglePayLauncherContract.PaymentIntentArgs(
            "pi_123_secret_456",
            GOOGLE_PAY_CONFIG
        )
        val REQUEST_OPTIONS = ApiRequest.Options(
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "account"
        )
    }
}
