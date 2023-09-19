package com.stripe.android.view

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CardWidgetViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `Emits correct CBC eligibility when the feature is enabled`() = runTest(testDispatcher) {
        val viewModel = CardWidgetViewModel(
            cbcEnabled = { true },
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
            cbcEnabled = { false },
            dispatcher = testDispatcher,
        )

        viewModel.isCbcEligible.test {
            assertThat(awaitItem()).isFalse()
        }
    }
}
