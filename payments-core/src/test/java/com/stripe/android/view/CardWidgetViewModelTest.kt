package com.stripe.android.view

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.utils.FakeCardElementConfigRepository
import com.stripe.android.utils.FeatureFlags
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

private val paymentConfig = PaymentConfiguration(
    publishableKey = "pk_test_123",
    stripeAccountId = null,
)

class CardWidgetViewModelTest {

    @get:Rule
    val featureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.cardBrandChoice,
        isEnabled = false,
    )

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `Emits correct CBC eligibility when the feature is enabled and merchant is eligible`() = runTest(testDispatcher) {
        featureFlagTestRule.setEnabled(true)

        val stripeRepository = FakeCardElementConfigRepository()

        val viewModel = CardWidgetViewModel(
            paymentConfigProvider = { paymentConfig },
            stripeRepository = stripeRepository,
            dispatcher = testDispatcher,
        )

        viewModel.isCbcEligible.test {
            assertThat(awaitItem()).isFalse()
            stripeRepository.enqueueEligible()
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `Emits correct CBC eligibility when the feature is disabled even if merchant is eligible`() = runTest(testDispatcher) {
        val stripeRepository = FakeCardElementConfigRepository()

        val viewModel = CardWidgetViewModel(
            paymentConfigProvider = { paymentConfig },
            stripeRepository = stripeRepository,
            dispatcher = testDispatcher,
        )

        viewModel.isCbcEligible.test {
            assertThat(awaitItem()).isFalse()
            stripeRepository.enqueueEligible()
            expectNoEvents()
        }
    }

    @Test
    fun `Emits correct CBC eligibility when the feature is enabled and merchant is not eligible`() = runTest(testDispatcher) {
        featureFlagTestRule.setEnabled(true)

        val stripeRepository = FakeCardElementConfigRepository()

        val viewModel = CardWidgetViewModel(
            paymentConfigProvider = { paymentConfig },
            stripeRepository = stripeRepository,
            dispatcher = testDispatcher,
        )

        viewModel.isCbcEligible.test {
            assertThat(awaitItem()).isFalse()
            stripeRepository.enqueueNotEligible()
            expectNoEvents()
        }
    }

    @Test
    fun `Emits correct CBC eligibility when query fails`() = runTest(testDispatcher) {
        featureFlagTestRule.setEnabled(true)

        val stripeRepository = FakeCardElementConfigRepository()

        val viewModel = CardWidgetViewModel(
            paymentConfigProvider = { paymentConfig },
            stripeRepository = stripeRepository,
            dispatcher = testDispatcher,
        )

        viewModel.isCbcEligible.test {
            assertThat(awaitItem()).isFalse()
            stripeRepository.enqueueFailure()
            expectNoEvents()
        }
    }
}
