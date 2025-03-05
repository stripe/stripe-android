package com.stripe.android.view

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.utils.FakeCardElementConfigRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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
                dispatcher = testDispatcher
            )

            viewModel.isCbcEligible.test {
                expectNoEvents()
                stripeRepository.enqueueEligible()
                expectNoEvents()
                viewModel.updateCardNumber("")
                expectNoEvents()
                viewModel.updateCardNumber("1")
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
                dispatcher = testDispatcher
            )

            viewModel.isCbcEligible.test {
                expectNoEvents()
                stripeRepository.enqueueNotEligible()
                viewModel.updateCardNumber("1")
                assertThat(awaitItem()).isFalse()
            }
        }

    @Test
    fun `Emits correct CBC eligibility when query fails`() = runTest(testDispatcher) {
        val stripeRepository = FakeCardElementConfigRepository()

        val viewModel = CardWidgetViewModel(
            paymentConfigProvider = { paymentConfig },
            stripeRepository = stripeRepository,
            dispatcher = testDispatcher
        )

        viewModel.isCbcEligible.test {
            expectNoEvents()
            stripeRepository.enqueueFailure()
            viewModel.updateCardNumber("1")
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `Setting valid OBO re-fetches correct eligibility`() = runTest(testDispatcher) {
        val stripeRepository = FakeCardElementConfigRepository()

        val viewModel = CardWidgetViewModel(
            paymentConfigProvider = { paymentConfig },
            stripeRepository = stripeRepository,
            dispatcher = testDispatcher
        )

        viewModel.updateCardNumber("1")

        viewModel.isCbcEligible.test {
            stripeRepository.enqueueEligible()
            assertThat(awaitItem()).isTrue()
            viewModel.setOnBehalfOf("valid_obo")
            assertThat(awaitItem()).isTrue()
            viewModel.setOnBehalfOf("valid_obo_2")
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `Setting invalid OBO re-fetches correct eligibility`() = runTest(testDispatcher) {
        val stripeRepository = FakeCardElementConfigRepository()

        val viewModel = CardWidgetViewModel(
            paymentConfigProvider = { paymentConfig },
            stripeRepository = stripeRepository,
            dispatcher = testDispatcher
        )

        stripeRepository.enqueueEligible()

        viewModel.isCbcEligible.test {
            viewModel.updateCardNumber("1")
            assertThat(awaitItem()).isTrue()
            viewModel.setOnBehalfOf("valid_obo")
            assertThat(awaitItem()).isTrue()
            stripeRepository.enqueueNotEligible()
            viewModel.setOnBehalfOf("invalid_obo")
            assertThat(awaitItem()).isFalse()
        }
    }
}
