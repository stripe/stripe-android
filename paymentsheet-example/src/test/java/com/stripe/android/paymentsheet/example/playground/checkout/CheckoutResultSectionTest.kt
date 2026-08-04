package com.stripe.android.paymentsheet.example.playground.checkout

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.example.playground.PlaygroundTheme
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
internal class CheckoutResultSectionTest {
    private val composeRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.emptyRuleChain()
        .around(createComposeCleanupRule())
        .around(composeRule)
        .around(CoroutineTestRule())

    @Test
    fun `completed result is displayed`() = runScenario(
        result = CheckoutControllerExampleViewModel.ConfirmationResult.Completed,
    ) {
        composeRule.onNodeWithText("Payment completed").assertExists()
        composeRule.onNodeWithText("New payment").assertExists()
    }

    @Test
    fun `failed result and message are displayed`() = runScenario(
        result = CheckoutControllerExampleViewModel.ConfirmationResult.Failed("Payment was declined"),
    ) {
        composeRule.onNodeWithText("Payment failed").assertExists()
        composeRule.onNodeWithText("Payment was declined").assertExists()
        composeRule.onNodeWithText("New payment").assertExists()
    }

    @Test
    fun `new payment invokes callback`() {
        var newPaymentRequested = false

        runScenario(
            result = CheckoutControllerExampleViewModel.ConfirmationResult.Completed,
            onNewPayment = { newPaymentRequested = true },
        ) {
            composeRule.onNodeWithText("New payment").performClick()

            composeRule.runOnIdle {
                assertThat(newPaymentRequested).isTrue()
            }
        }
    }

    private fun runScenario(
        result: CheckoutControllerExampleViewModel.ConfirmationResult,
        onNewPayment: () -> Unit = {},
        block: () -> Unit,
    ) {
        composeRule.setContent {
            PlaygroundTheme(
                content = {},
                bottomBarContent = {
                    CheckoutResultSection(
                        result = result,
                        onNewPayment = onNewPayment,
                    )
                },
            )
        }
        composeRule.waitForIdle()
        block()
    }
}
