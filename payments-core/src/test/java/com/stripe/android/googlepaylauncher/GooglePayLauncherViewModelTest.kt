package com.stripe.android.googlepaylauncher

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.GooglePayConfig
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.PaymentIntentResult
import com.stripe.android.SetupIntentResult
import com.stripe.android.StripePaymentController
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
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class GooglePayLauncherViewModelTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val paymentController = FakePaymentController()
    private val stripeRepository = FakeStripeRepository()
    private val googlePayJsonFactory = GooglePayJsonFactory(
        googlePayConfig = GooglePayConfig(
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "account"
        )
    )

    private val viewModel = GooglePayLauncherViewModel(
        REQUEST_OPTIONS,
        ARGS,
        stripeRepository,
        paymentController,
        googlePayJsonFactory
    )

    @BeforeTest
    fun setup() {
    }

    @AfterTest
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `createTransactionInfo() with PaymentIntent should return expected TransactionInfo`() {
        val transactionInfo = viewModel.createTransactionInfo(
            PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
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
    fun `createTransactionInfo() with SetupIntent should throw expected exception`() {
        val error = assertFailsWith<IllegalStateException> {
            viewModel.createTransactionInfo(
                SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD
            )
        }
        assertThat(error.message)
            .isEqualTo("SetupIntents are not currently supported in GooglePayLauncher.")
    }

    @Test
    fun `getStripeIntent() with PaymentIntent client secret should return a PaymentIntent`() =
        testDispatcher.runBlockingTest {
            assertThat(
                viewModel.getStripeIntent(
                    "pi_1F7J1aCRMbs6FrXfaJcvbxF6_secret_mIuDLsSfoo1m6s"
                )
            ).isEqualTo(
                PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
            )
        }

    @Test
    fun `getStripeIntent() with SetupIntent client secret should return a SetupIntent`() =
        testDispatcher.runBlockingTest {
            assertThat(
                viewModel.getStripeIntent(
                    "seti_1GSmaFCRMbs6FrXfmjThcHan_secret_H0oC2iSB4FtW4d"
                )
            ).isEqualTo(
                SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD
            )
        }

    @Test
    fun `getStripeIntent() with invalid client secret should throw exception`() =
        testDispatcher.runBlockingTest {
            val error = assertFailsWith<IllegalStateException> {
                viewModel.getStripeIntent(
                    "invalid!"
                )
            }
            assertThat(error.message)
                .isEqualTo("Invalid client secret.")
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
            val paymentFlowResult = PaymentFlowResult.Unvalidated.fromIntent(data)
            return PaymentIntentResult(
                PaymentIntentFixtures.PI_SUCCEEDED,
                outcomeFromFlow = paymentFlowResult.flowOutcome
            )
        }

        override suspend fun getSetupIntentResult(data: Intent): SetupIntentResult {
            val paymentFlowResult = PaymentFlowResult.Unvalidated.fromIntent(data)
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
        val ARGS = GooglePayLauncherContract.Args(
            "pi_123_secret_456",
            GooglePayLauncher.Config(
                GooglePayEnvironment.Test,
                merchantCountryCode = "us",
                merchantName = "Widget, Inc."
            )
        )
        val REQUEST_OPTIONS = ApiRequest.Options(
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "account"
        )
    }
}
