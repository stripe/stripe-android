package com.stripe.android.googlepaylauncher

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
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
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.googlepaylauncher.GooglePayLauncherViewModel.Companion.HAS_LAUNCHED_KEY
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.AbsPaymentController
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class GooglePayLauncherViewModelTest {
    private val savedStateHandle = SavedStateHandle()
    private val googlePayJsonFactory = GooglePayJsonFactory(
        googlePayConfig = GooglePayConfig(
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "account"
        )
    )

    private val googlePayRepository = FakeGooglePayRepository(true)
    private val testDispatcher = UnconfinedTestDispatcher()

    private val task = mock<Task<PaymentData>>().also {
        whenever(it.isComplete).thenReturn(true)
    }
    private val paymentsClient = mock<PaymentsClient>().also {
        whenever(it.loadPaymentData(any()))
            .thenReturn(task)
    }

    @Test
    fun `isReadyToPay() should return expected value`() = runTest {
        assertThat(createViewModel().isReadyToPay())
            .isTrue()
    }

    @Test
    fun `createLoadPaymentDataTask() should throw expected exception when Google Pay is not available`() =
        runTest {
            googlePayRepository.value = false
            createViewModel().googlePayResult.test {
                val failed = awaitItem() as GooglePayLauncher.Result.Failed
                val error = failed.error
                assertThat(error).isInstanceOf(IllegalStateException::class.java)
                assertThat(error.message).isEqualTo("Google Pay is unavailable.")
            }
        }

    @Test
    fun `googlePayLaunchTask should return task when Google Pay is available`() = runTest {
        createViewModel().googlePayLaunchTask.test {
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `createTransactionInfo() with PaymentIntent should return expected TransactionInfo`() {
        val transactionInfo = createViewModel().createTransactionInfo(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            currencyCode = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.currency.orEmpty(),
        )
        assertThat(transactionInfo)
            .isEqualTo(
                GooglePayJsonFactory.TransactionInfo(
                    currencyCode = "usd",
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Final,
                    countryCode = "us",
                    transactionId = "pi_1F7J1aCRMbs6FrXfaJcvbxF6",
                    totalPrice = 1099L,
                    totalPriceLabel = null,
                    checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.CompleteImmediatePurchase
                )
            )
    }

    @Test
    fun `createTransactionInfo() with SetupIntent should return expected TransactionInfo`() {
        val transactionInfo = createViewModel().createTransactionInfo(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            currencyCode = "usd",
        )
        assertThat(transactionInfo)
            .isEqualTo(
                GooglePayJsonFactory.TransactionInfo(
                    currencyCode = "usd",
                    totalPriceStatus = GooglePayJsonFactory.TransactionInfo.TotalPriceStatus.Estimated,
                    countryCode = "us",
                    transactionId = "seti_1GSmaFCRMbs",
                    totalPrice = 0L,
                    totalPriceLabel = null,
                    checkoutOption = GooglePayJsonFactory.TransactionInfo.CheckoutOption.Default
                )
            )
    }

    @Test
    fun `hasLaunched is stored in savedStateHandle`() = runTest {
        val viewModel = createViewModel()

        viewModel.googlePayLaunchTask.test {
            assertThat(awaitItem()).isNotNull()
            assertThat(savedStateHandle.get<Boolean>(HAS_LAUNCHED_KEY)).isNull()
            viewModel.markTaskAsLaunched()
            assertThat(awaitItem()).isNull()
            assertThat(savedStateHandle.get<Boolean>(HAS_LAUNCHED_KEY)).isTrue()
        }
    }

    @Test
    fun `hasLaunched=true prevents initial loading of googlePayLaunchTask`() = runTest {
        savedStateHandle[HAS_LAUNCHED_KEY] = true
        val viewModel = createViewModel()
        viewModel.googlePayLaunchTask.test {
            expectNoEvents() // We already launched, so we don't expect an emission here.
        }
    }

    @Test
    fun `getResultFromConfirmation() using PaymentIntent should return expected result`() =
        runTest {
            val result = createViewModel().getResultFromConfirmation(
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
        runTest {
            val result = createViewModel().getResultFromConfirmation(
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
        runTest {
            val exception = StripeException.create(Exception("Failure"))
            val result = createViewModel().getResultFromConfirmation(
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
        runTest {
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
        runTest {
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
        paymentController: PaymentController = FakePaymentController(),
        stripeRepository: StripeRepository = FakeStripeRepository(),
    ) = GooglePayLauncherViewModel(
        paymentsClient = paymentsClient,
        requestOptions = REQUEST_OPTIONS,
        args = args,
        stripeRepository = stripeRepository,
        paymentController = paymentController,
        googlePayJsonFactory = googlePayJsonFactory,
        googlePayRepository = googlePayRepository,
        savedStateHandle = savedStateHandle,
        errorReporter = FakeErrorReporter(),
        workContext = testDispatcher,
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
        ): Result<PaymentIntentResult> {
            return runCatching {
                val paymentFlowResult = PaymentFlowResult.Unvalidated.fromIntent(data).validate()
                PaymentIntentResult(
                    intent = PaymentIntentFixtures.PI_SUCCEEDED,
                    outcomeFromFlow = paymentFlowResult.flowOutcome
                )
            }
        }

        override suspend fun getSetupIntentResult(data: Intent): Result<SetupIntentResult> {
            return runCatching {
                val paymentFlowResult = PaymentFlowResult.Unvalidated.fromIntent(data).validate()
                SetupIntentResult(
                    intent = SetupIntentFixtures.SI_SUCCEEDED,
                    outcomeFromFlow = paymentFlowResult.flowOutcome
                )
            }
        }
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {

        override suspend fun retrievePaymentIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ): Result<PaymentIntent> {
            return Result.success(PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD)
        }

        override suspend fun retrieveSetupIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ): Result<SetupIntent> {
            return Result.success(SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD)
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
