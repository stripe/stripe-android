package com.stripe.android.view

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.utils.FeatureFlags
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class CardWidgetViewModelTest {

    @get:Rule
    val featureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.cardBrandChoice,
        isEnabled = false,
    )

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `Emits correct CBC eligibility when the feature is enabled`() = runTest(testDispatcher) {
        featureFlagTestRule.setEnabled(true)

        val viewModel = CardWidgetViewModel(
            cbcEligible = { true },
            dispatcher = testDispatcher,
        )

        viewModel.isCbcEligible.test {
            assertThat(awaitItem()).isFalse()
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `Emits correct CBC eligibility when the feature is disabled`() = runTest(testDispatcher) {
        val viewModel = CardWidgetViewModel(
            cbcEligible = { true },
            dispatcher = testDispatcher,
        )

        viewModel.isCbcEligible.test {
            assertThat(awaitItem()).isFalse()
        }
    }
}
