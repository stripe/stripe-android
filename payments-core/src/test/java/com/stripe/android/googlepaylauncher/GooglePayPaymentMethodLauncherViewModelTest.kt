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
import app.cash.turbine.test
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentsClient
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.GooglePayConfig
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.PaymentConfiguration
import com.stripe.android.challenge.PassiveChallengeActivityResult
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.isInstanceOf
import com.stripe.android.model.GooglePayFixtures
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.fakeCreationExtras
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TestRule
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

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val rule: TestRule = CoroutineTestRule(testDispatcher)

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

    private fun createViewModel(
        args: GooglePayPaymentMethodLauncherContractV2.Args = ARGS,
        stripeRepository: FakeStripeRepository = this.stripeRepository,
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ): GooglePayPaymentMethodLauncherViewModel {
        return GooglePayPaymentMethodLauncherViewModel(
            paymentsClient,
            REQUEST_OPTIONS,
            args,
            stripeRepository,
            googlePayJsonFactory,
            googlePayRepository,
            savedStateHandle
        )
    }

    @Test
    fun `createPaymentMethod() should return expected result without captcha token`() = runTest {
        val viewModel = createViewModel()
        val result = viewModel.createPaymentMethod(
            PaymentData.fromJson(
                GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS.toString()
            ),
            passiveCaptchaToken = null
        )
        assertThat(result)
            .isEqualTo(
                GooglePayPaymentMethodLauncher.Result.Completed(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                )
            )
    }

    @Test
    fun `createPaymentMethod() should return expected result with captcha token`() = runTest {
        val testRepository = FakeStripeRepository()
        val viewModel = createViewModel(stripeRepository = testRepository)
        val captchaToken = "test_captcha_token"
        val result = viewModel.createPaymentMethod(
            PaymentData.fromJson(
                GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS.toString()
            ),
            passiveCaptchaToken = captchaToken
        )
        assertThat(result)
            .isEqualTo(
                GooglePayPaymentMethodLauncher.Result.Completed(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                )
            )
        // Verify that the repository was called with radar options containing the captcha token
        assertThat(testRepository.lastCreatePaymentMethodParams?.radarOptions?.hCaptchaToken)
            .isEqualTo(captchaToken)
    }

    @Test
    fun `createTransactionInfo() with amount should create expected TransactionInfo`() {
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()
        for (countryCode in listOf("us", "ca")) {
            val transactionInfo = viewModel.createTransactionInfo(
                GooglePayPaymentMethodLauncherContractV2.Args(
                    GooglePayPaymentMethodLauncher.Config(
                        GooglePayEnvironment.Test,
                        merchantCountryCode = countryCode,
                        merchantName = "Widget, Inc."
                    ),
                    currencyCode = "usd",
                    amount = 0,
                    passiveCaptchaParams = null
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
        val viewModel = createViewModel()
        for (countryCode in listOf("de", "fr", "gb", "jp", "mx")) {
            val transactionInfo = viewModel.createTransactionInfo(
                GooglePayPaymentMethodLauncherContractV2.Args(
                    GooglePayPaymentMethodLauncher.Config(
                        GooglePayEnvironment.Test,
                        merchantCountryCode = countryCode,
                        merchantName = "Widget, Inc."
                    ),
                    currencyCode = "usd",
                    amount = 0,
                    passiveCaptchaParams = null
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
        val viewModel = createViewModel()
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
    fun `handlePaymentData() without passiveCaptchaParams should update result directly`() = runTest {
        val viewModel = createViewModel()
        val paymentData = PaymentData.fromJson(
            GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS.toString()
        )

        viewModel.handlePaymentData(paymentData)

        val result = viewModel.googlePayResult.first { it != null }
        assertThat(result)
            .isEqualTo(
                GooglePayPaymentMethodLauncher.Result.Completed(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
                )
            )
    }

    @Test
    fun `handlePaymentData() with passiveCaptchaParams should emit RunPassiveChallenge effect`() = runTest {
        val paymentData = PaymentData.fromJson(
            GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS.toString()
        )
        val passiveCaptchaParams = PassiveCaptchaParams(
            siteKey = "test_site_key",
            rqData = "test"
        )
        val argsWithCaptcha = ARGS.copy(passiveCaptchaParams = passiveCaptchaParams)
        val viewModelWithCaptcha = createViewModel(args = argsWithCaptcha)

        viewModelWithCaptcha.handlePaymentData(paymentData)

        advanceUntilIdle()

        viewModelWithCaptcha.effects.test {
            val effect = awaitItem()

            assertThat(effect).isInstanceOf<GooglePayPaymentMethodLauncherViewModel.Effect.RunPassiveChallenge>()
            val runPassiveChallengeEffect = effect as GooglePayPaymentMethodLauncherViewModel.Effect.RunPassiveChallenge
            assertThat(runPassiveChallengeEffect.passiveCaptchaParams).isEqualTo(passiveCaptchaParams)
        }
    }

    @Test
    fun `handlePassiveChallengeResult() with Success should create payment method with token`() = runTest {
        val paymentData = PaymentData.fromJson(
            GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS.toString()
        )
        val captchaToken = "success_token"
        val passiveCaptchaParams = PassiveCaptchaParams(
            siteKey = "test_site_key",
            rqData = "test"
        )
        val argsWithCaptcha = ARGS.copy(passiveCaptchaParams = passiveCaptchaParams)
        val viewModelWithCaptcha = createViewModel(args = argsWithCaptcha)

        // First set payment data
        viewModelWithCaptcha.handlePaymentData(paymentData)

        viewModelWithCaptcha.effects.test {
            awaitItem()

            // Then handle success result
            val successResult = PassiveChallengeActivityResult.Success(captchaToken)
            viewModelWithCaptcha.handlePassiveChallengeResult(successResult)

            val result = viewModelWithCaptcha.googlePayResult.first { it != null }
            assertThat(result)
                .isEqualTo(
                    GooglePayPaymentMethodLauncher.Result.Completed(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                )
            assertThat(stripeRepository.lastCreatePaymentMethodParams?.radarOptions?.hCaptchaToken)
                .isEqualTo(captchaToken)
        }
    }

    @Test
    fun `handlePassiveChallengeResult() with Failed should create payment method without token`() = runTest {
        val paymentData = PaymentData.fromJson(
            GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS.toString()
        )

        val passiveCaptchaParams = PassiveCaptchaParams(
            siteKey = "test_site_key",
            rqData = "test"
        )
        val argsWithCaptcha = ARGS.copy(passiveCaptchaParams = passiveCaptchaParams)

        val viewModelWithCaptcha = createViewModel(args = argsWithCaptcha)

        // First set payment data
        viewModelWithCaptcha.handlePaymentData(paymentData)

        viewModelWithCaptcha.effects.test {
            awaitItem()

            // Then handle failed result
            val failedResult = PassiveChallengeActivityResult.Failed(RuntimeException("Captcha failed"))
            viewModelWithCaptcha.handlePassiveChallengeResult(failedResult)

            val result = viewModelWithCaptcha.googlePayResult.first { it != null }
            assertThat(result)
                .isEqualTo(
                    GooglePayPaymentMethodLauncher.Result.Completed(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                )
            assertThat(stripeRepository.lastCreatePaymentMethodParams?.radarOptions?.hCaptchaToken)
                .isNull()
        }
    }

    @Test
    fun `handlePassiveChallengeResult() without paymentData should return error`() = runTest {
        val viewModel = createViewModel()
        val successResult = PassiveChallengeActivityResult.Success("token")
        viewModel.handlePassiveChallengeResult(successResult)

        val result = viewModel.googlePayResult.first { it != null }
        assertThat(result).isInstanceOf<GooglePayPaymentMethodLauncher.Result.Failed>()
        val failedResult = result as GooglePayPaymentMethodLauncher.Result.Failed
        assertThat(failedResult.error).isInstanceOf<IllegalStateException>()
        assertThat(failedResult.error.message).contains("PaymentData is null")
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
                    passiveCaptchaParams = null
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
        var lastCreatePaymentMethodParams: PaymentMethodCreateParams? = null
            private set

        override suspend fun createPaymentMethod(
            paymentMethodCreateParams: PaymentMethodCreateParams,
            options: ApiRequest.Options,
        ): Result<PaymentMethod> {
            lastCreatePaymentMethodParams = paymentMethodCreateParams
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
            amount = 1000,
            passiveCaptchaParams = null
        )
        val REQUEST_OPTIONS = ApiRequest.Options(
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "account"
        )
    }
}
