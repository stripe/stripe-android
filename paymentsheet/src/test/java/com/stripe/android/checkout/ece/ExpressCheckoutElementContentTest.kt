package com.stripe.android.checkout.ece

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.link.ui.LinkButtonTestTag
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.paymentsheet.ui.GOOGLE_PAY_BUTTON_TEST_TAG
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
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

    @Before
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext<Context>(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        )
    }

    @Test
    fun `renders wallet buttons`() {
        val viewActionRecorder = ViewActionRecorder<ExpressCheckoutElementInteractor.ViewAction>()
        val interactor = FakeExpressCheckoutElementInteractor(
            state = stateFlowOf(
                ExpressCheckoutElementInteractorStateFactory.create()
            ),
            viewActionRecorder = viewActionRecorder,
        )

        composeRule.setContent {
            ExpressCheckoutElementContent(interactor = interactor)
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG).assertExists()
        composeRule.onNodeWithTag(LinkButtonTestTag).assertExists()
        assertThat(viewActionRecorder.viewActions)
            .containsExactly(ExpressCheckoutElementInteractor.ViewAction.OnDisplayed)
    }

    @Test
    fun `does not render wallet buttons when state has no buttons`() {
        val interactor = FakeExpressCheckoutElementInteractor(
            state = stateFlowOf(
                ExpressCheckoutElementInteractorStateFactory.create(expressButtons = emptyList())
            ),
        )

        composeRule.setContent {
            ExpressCheckoutElementContent(interactor = interactor)
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(LinkButtonTestTag).assertDoesNotExist()
    }

    @Test
    fun `reports wallet tapped when Google Pay button is pressed`() {
        val state = ExpressCheckoutElementInteractorStateFactory.create()
        val viewActionRecorder = ViewActionRecorder<ExpressCheckoutElementInteractor.ViewAction>()
        val interactor = FakeExpressCheckoutElementInteractor(
            state = stateFlowOf(state),
            viewActionRecorder = viewActionRecorder,
        )

        composeRule.setContent {
            ExpressCheckoutElementContent(interactor = interactor)
        }

        composeRule.waitForIdle()
        viewActionRecorder.consume(ExpressCheckoutElementInteractor.ViewAction.OnDisplayed)

        composeRule.onNodeWithTag(GOOGLE_PAY_BUTTON_TEST_TAG).performClick()

        composeRule.runOnIdle {
            viewActionRecorder.consume(
                ExpressCheckoutElementInteractor.ViewAction.OnWalletTapped(
                    expressButton = state.expressButtons.filterIsInstance<ExpressButton.GooglePay>().single(),
                )
            )
            assertThat(viewActionRecorder.viewActions).isEmpty()
        }
    }

    @Test
    fun `reports wallet tapped when Link button is pressed`() {
        val state = ExpressCheckoutElementInteractorStateFactory.create()
        val viewActionRecorder = ViewActionRecorder<ExpressCheckoutElementInteractor.ViewAction>()
        val interactor = FakeExpressCheckoutElementInteractor(
            state = stateFlowOf(state),
            viewActionRecorder = viewActionRecorder,
        )

        composeRule.setContent {
            ExpressCheckoutElementContent(interactor = interactor)
        }

        composeRule.waitForIdle()
        viewActionRecorder.consume(ExpressCheckoutElementInteractor.ViewAction.OnDisplayed)

        composeRule.onNodeWithTag(LinkButtonTestTag).performClick()

        composeRule.runOnIdle {
            viewActionRecorder.consume(
                ExpressCheckoutElementInteractor.ViewAction.OnWalletTapped(
                    expressButton = state.expressButtons.filterIsInstance<ExpressButton.Link>().single(),
                )
            )
            assertThat(viewActionRecorder.viewActions).isEmpty()
        }
    }

    private class FakeExpressCheckoutElementInteractor(
        override val state: StateFlow<ExpressCheckoutElementInteractor.State>,
        private val viewActionRecorder: ViewActionRecorder<ExpressCheckoutElementInteractor.ViewAction> =
            ViewActionRecorder(),
    ) : ExpressCheckoutElementInteractor {
        override fun handleViewAction(viewAction: ExpressCheckoutElementInteractor.ViewAction) {
            viewActionRecorder.record(viewAction)
        }
    }
}
