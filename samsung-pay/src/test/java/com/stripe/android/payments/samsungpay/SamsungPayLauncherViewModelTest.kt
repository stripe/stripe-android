package com.stripe.android.payments.samsungpay

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SamsungPayLauncherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeTokenExchangeHandler = FakeTokenExchangeHandler()
    private val application: Application = RuntimeEnvironment.getApplication()

    private val config = SamsungPayLauncher.Config(
        environment = SamsungPayEnvironment.Test,
        productId = "test_product_id",
        merchantName = "Test Store",
    )

    private val fakeStripeIntentRepository = object : StripeIntentRepository {
        var paymentIntentToReturn: PaymentIntent = mock<PaymentIntent>().also {
            whenever(it.amount).thenReturn(1000L)
            whenever(it.currency).thenReturn("usd")
        }
        var confirmResult: PaymentIntent = mock()
        var confirmSetupResult: SetupIntent = mock()

        override suspend fun retrievePaymentIntent(clientSecret: String): PaymentIntent {
            return paymentIntentToReturn
        }

        override suspend fun confirmPaymentIntent(params: ConfirmPaymentIntentParams): PaymentIntent {
            return confirmResult
        }

        override suspend fun confirmSetupIntent(params: ConfirmSetupIntentParams): SetupIntent {
            return confirmSetupResult
        }
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        tokenExchangeHandler: TokenExchangeHandler = fakeTokenExchangeHandler,
    ): SamsungPayLauncherViewModel {
        return SamsungPayLauncherViewModel(
            savedStateHandle = savedStateHandle,
            stripeIntentRepository = fakeStripeIntentRepository,
            config = config,
            tokenExchangeHandler = tokenExchangeHandler,
            credentialParser = SamsungPayCredentialParser,
            context = application,
            workContext = testDispatcher,
        )
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial result is null`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        assertThat(viewModel.result.value).isNull()
    }

    @Test
    fun `hasLaunched prevents duplicate launches`() = runTest(testDispatcher) {
        val savedState = SavedStateHandle(
            mapOf(SamsungPayLauncherViewModel.HAS_LAUNCHED_KEY to true)
        )

        val viewModel = createViewModel(savedStateHandle = savedState)

        val args = SamsungPayLauncherContract.Args.PaymentIntentArgs(
            clientSecret = "pi_123_secret_456",
            config = config,
        )

        // startPayment should be a no-op since hasLaunched is already true
        viewModel.startPayment(args)
        advanceUntilIdle()

        // Result stays null since nothing happened
        assertThat(viewModel.result.value).isNull()
    }

    @Test
    fun `FakeTokenExchangeHandler tracks calls`() = runTest {
        val handler = FakeTokenExchangeHandler()
        handler.tokenToReturn = "tok_test_abc"

        val request = SamsungPayTokenRequest(
            rawCredential = "{}",
            cryptogram = "abc",
            cryptogramType = "S",
            version = "100",
            cardBrand = "VI",
            last4Dpan = "1234",
            last4Fpan = "5678",
            currencyType = "USD",
        )

        val result = handler.exchangeForToken(request)

        assertThat(result).isEqualTo("tok_test_abc")
        assertThat(handler.callCount).isEqualTo(1)
        assertThat(handler.lastRequest).isEqualTo(request)
    }

    @Test
    fun `FakeTokenExchangeHandler throws when configured`() = runTest {
        val handler = FakeTokenExchangeHandler()
        handler.errorToThrow = RuntimeException("Server error")

        val request = SamsungPayTokenRequest(
            rawCredential = "{}",
            cryptogram = "abc",
            cryptogramType = "S",
            version = "100",
            cardBrand = "VI",
            last4Dpan = "1234",
            last4Fpan = "5678",
            currencyType = "USD",
        )

        try {
            handler.exchangeForToken(request)
            assertThat(false).isTrue() // Should not reach here
        } catch (e: RuntimeException) {
            assertThat(e.message).isEqualTo("Server error")
        }
    }
}
