package com.stripe.android.checkout.ece

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExpressCheckoutElementContentTest {
    private val composeRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.emptyRuleChain()
        .around(createComposeCleanupRule())
        .around(composeRule)
        .around(CoroutineTestRule())

    @Test
    fun `renders wallet button text`() {
        val interactor = FakeExpressCheckoutElementInteractor(
            state = stateFlowOf(
                ExpressCheckoutElementInteractorStateFactory.create(
                    expressButtons = listOf(
                        ExpressButton.GooglePay,
                        ExpressButton.Link,
                    ),
                )
            )
        )

        composeRule.setContent {
            ExpressCheckoutElementContent(interactor = interactor)
        }

        composeRule.onNodeWithText("Google Pay Button").assertExists()
        composeRule.onNodeWithText("Link Button").assertExists()
    }

    @Test
    fun `does not render wallet button text when state has no buttons`() {
        val interactor = FakeExpressCheckoutElementInteractor(
            state = stateFlowOf(
                ExpressCheckoutElementInteractorStateFactory.create(expressButtons = emptyList())
            )
        )

        composeRule.setContent {
            ExpressCheckoutElementContent(interactor = interactor)
        }

        composeRule.onNodeWithText("Google Pay Button").assertDoesNotExist()
        composeRule.onNodeWithText("Link Button").assertDoesNotExist()
    }

    private class FakeExpressCheckoutElementInteractor(
        override val state: StateFlow<ExpressCheckoutElementInteractor.State>,
    ) : ExpressCheckoutElementInteractor
}
