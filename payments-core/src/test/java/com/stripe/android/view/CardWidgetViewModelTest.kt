package com.stripe.android.view

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.utils.FakeCardElementConfigRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.internal.wait
import org.junit.Test

private val paymentConfig = PaymentConfiguration(
    publishableKey = "pk_test_123",
    stripeAccountId = null,
)

class CardWidgetViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `Emits correct CBC eligibility when the feature is enabled and merchant is eligible`() =
        runTest(testDispatcher) {
            val stripeRepository = FakeCardElementConfigRepository()

            val viewModel = CardWidgetViewModel(
                paymentConfigProvider = { paymentConfig },
                stripeRepository = stripeRepository,
                dispatcher = testDispatcher,
                handle = SavedStateHandle()
            )

            viewModel.isCbcEligible.test {
                assertThat(awaitItem()).isFalse()
                stripeRepository.enqueueEligible()
                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `Emits correct CBC eligibility when the feature is enabled and merchant is not eligible`() =
        runTest(testDispatcher) {
            val stripeRepository = FakeCardElementConfigRepository()

            val viewModel = CardWidgetViewModel(
                paymentConfigProvider = { paymentConfig },
                stripeRepository = stripeRepository,
                dispatcher = testDispatcher,
                handle = SavedStateHandle()
            )

            viewModel.isCbcEligible.test {
                assertThat(awaitItem()).isFalse()
                stripeRepository.enqueueNotEligible()
                expectNoEvents()
            }
        }

    @Test
    fun `Emits correct CBC eligibility when query fails`() = runTest(testDispatcher) {
        val stripeRepository = FakeCardElementConfigRepository()

        val viewModel = CardWidgetViewModel(
            paymentConfigProvider = { paymentConfig },
            stripeRepository = stripeRepository,
            dispatcher = testDispatcher,
            handle = SavedStateHandle()
        )

        viewModel.isCbcEligible.test {
            assertThat(awaitItem()).isFalse()
            stripeRepository.enqueueFailure()
            expectNoEvents()
        }
    }

    @Test
    fun `Saves OBO to savedStateHandle`() = runTest(testDispatcher) {
        val stripeRepository = FakeCardElementConfigRepository()
        val handle = SavedStateHandle()

        val viewModel = CardWidgetViewModel(
            paymentConfigProvider = { paymentConfig },
            stripeRepository = stripeRepository,
            dispatcher = testDispatcher,
            handle = handle
        )

        viewModel.onBehalfOf = "test"
        val obo: String? = handle["on_behalf_of"]
        assertThat(obo).isEqualTo("test")
    }

    @Test
    fun `Setting valid OBO re-fetches correct eligibility`() = runTest(testDispatcher) {
        val stripeRepository = FakeCardElementConfigRepository()

        val viewModel = CardWidgetViewModel(
            paymentConfigProvider = { paymentConfig },
            stripeRepository = stripeRepository,
            dispatcher = testDispatcher,
            handle = SavedStateHandle()
        )

        viewModel.isCbcEligible.test {
            assertThat(awaitItem()).isFalse()
            stripeRepository.enqueueEligible()
            viewModel.onBehalfOf = "valid_obo"
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `Setting invalid OBO re-fetches correct eligibility`() = runTest(testDispatcher) {
        val stripeRepository = FakeCardElementConfigRepository()

        val viewModel = CardWidgetViewModel(
            paymentConfigProvider = { paymentConfig },
            stripeRepository = stripeRepository,
            dispatcher = testDispatcher,
            handle = SavedStateHandle()
        )

        stripeRepository.enqueueEligible()

        viewModel.isCbcEligible.test {
            viewModel.onBehalfOf = "valid_obo"
            assertThat(awaitItem()).isTrue()
            stripeRepository.enqueueNotEligible()
            viewModel.onBehalfOf = "invalid_obo"
            assertThat(awaitItem()).isFalse()
        }
    }
}
