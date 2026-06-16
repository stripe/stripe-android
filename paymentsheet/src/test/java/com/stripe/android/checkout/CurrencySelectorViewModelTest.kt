package com.stripe.android.checkout

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeAnalyticsRequestExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
internal class CurrencySelectorViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(dispatcher)

    @Test
    fun `init fires currency selector init analytics event`() = runScenario {
        val requests = fakeAnalyticsRequestExecutor.getExecutedRequests()
        assertThat(requests).hasSize(1)
        assertThat(requests.first().params["event"])
            .isEqualTo("elements.adaptive_pricing.currency_selector_init")
    }

    @Test
    fun `init only fires analytics event once across config changes`() = runScenario(
        savedStateHandle = SavedStateHandle(mapOf("currency_selector_initialized" to true)),
    ) {
        val requests = fakeAnalyticsRequestExecutor.getExecutedRequests()
        assertThat(requests).isEmpty()
    }

    @Test
    fun `onCurrencySelected calls updateCurrency with correct code`() = runScenario {
        updateCurrencyResult.add(Result.success(Unit))

        viewModel.onCurrencySelected("eur")

        assertThat(updateCurrencyCalls.awaitItem()).isEqualTo("eur")
    }

    @Test
    fun `onCurrencySelected sets error message on failure`() = runScenario {
        updateCurrencyResult.add(Result.failure(RuntimeException("Something went wrong")))
        viewModel.errorMessage.test {
            assertThat(awaitItem()).isNull()

            viewModel.onCurrencySelected("eur")
            assertThat(updateCurrencyCalls.awaitItem()).isEqualTo("eur")

            assertThat(awaitItem()).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        }
    }

    @Test
    fun `errorMessage is cleared when checkout session changes`() = runScenario {
        updateCurrencyResult.add(Result.failure(RuntimeException("fail")))

        viewModel.onCurrencySelected("eur")
        assertThat(updateCurrencyCalls.awaitItem()).isEqualTo("eur")

        viewModel.errorMessage.test {
            assertThat(awaitItem()).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)

            checkoutSessionFlow.value = InternalState(
                key = "CheckoutConfigurationMergerTest",
                configuration = Checkout.Configuration().build(),
                CheckoutSessionResponseFactory.create(currency = "eur"),
                flagImages = null,
            ).asCheckoutSession()

            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `onCurrencySelected does not set error on success`() = runScenario {
        updateCurrencyResult.add(Result.success(Unit))

        viewModel.onCurrencySelected("gbp")
        assertThat(updateCurrencyCalls.awaitItem()).isEqualTo("gbp")

        assertThat(viewModel.errorMessage.value).isNull()
    }

    @Test
    fun `multiple currency selections track all calls`() = runScenario {
        updateCurrencyResult.add(Result.success(Unit))
        viewModel.onCurrencySelected("eur")
        assertThat(updateCurrencyCalls.awaitItem()).isEqualTo("eur")

        updateCurrencyResult.add(Result.success(Unit))
        viewModel.onCurrencySelected("gbp")
        assertThat(updateCurrencyCalls.awaitItem()).isEqualTo("gbp")

        updateCurrencyResult.add(Result.success(Unit))
        viewModel.onCurrencySelected("jpy")
        assertThat(updateCurrencyCalls.awaitItem()).isEqualTo("jpy")
    }

    private fun runScenario(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        block: suspend Scenario.() -> Unit,
    ) = runTest(dispatcher) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val fakeAnalyticsRequestExecutor = FakeAnalyticsRequestExecutor()
        val checkoutSessionFlow = MutableStateFlow(
            InternalState(
                key = "CheckoutConfigurationMergerTest",
                configuration = Checkout.Configuration().build(),
                CheckoutSessionResponseFactory.create(),
                flagImages = null,
            ).asCheckoutSession()
        )
        val updateCurrencyCalls = Turbine<String>()
        val updateCurrencyResult = Turbine<Result<Unit>>()

        val viewModel = CurrencySelectorViewModel(
            checkoutSession = checkoutSessionFlow,
            updateCurrency = { code ->
                updateCurrencyCalls.add(code)
                updateCurrencyResult.awaitItem()
            },
            analyticsRequestExecutor = fakeAnalyticsRequestExecutor,
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = application,
                publishableKey = "pk_test_123",
            ),
            savedStateHandle = savedStateHandle,
        )

        Scenario(
            fakeAnalyticsRequestExecutor = fakeAnalyticsRequestExecutor,
            checkoutSessionFlow = checkoutSessionFlow,
            updateCurrencyCalls = updateCurrencyCalls,
            updateCurrencyResult = updateCurrencyResult,
            viewModel = viewModel,
        ).block()

        updateCurrencyCalls.ensureAllEventsConsumed()
        updateCurrencyResult.ensureAllEventsConsumed()
    }

    private class Scenario(
        val fakeAnalyticsRequestExecutor: FakeAnalyticsRequestExecutor,
        val checkoutSessionFlow: MutableStateFlow<CheckoutSession>,
        val updateCurrencyCalls: Turbine<String>,
        val updateCurrencyResult: Turbine<Result<Unit>>,
        val viewModel: CurrencySelectorViewModel,
    )
}
