package com.stripe.android.paymentsheet.verticalmode

import android.os.Build
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.definitions.AffirmDefinition
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.FakeStripeImageLoader
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class NewPaymentMethodRowButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `initially selected row stays collapsed until clicked`() = runScenario {
        promotion().assertIsNotDisplayed()
        calls.expectNoEvents()

        row().performClick()

        assertClickCalls()
        promotion().assertIsDisplayed()
    }

    @Test
    fun `disabled click does not evaluate expansion or select row`() = runScenario(isEnabled = false) {
        row().assertIsNotEnabled().performClick()

        calls.expectNoEvents()
        promotion().assertIsNotDisplayed()
    }

    @Test
    fun `rebuilding the same row preserves expansion without evaluating callback`() = runScenario {
        row().performClick()
        assertClickCalls()
        promotion().assertIsDisplayed()

        composeRule.runOnIdle {
            expansionResult.value = false
            paymentMethod.value = paymentMethod.value.copy(displayName = "Updated title".resolvableString)
        }

        composeRule.onNodeWithText("Updated title").assertIsDisplayed()
        promotion().assertIsDisplayed()
        calls.expectNoEvents()

        row().performClick()

        assertClickCalls()
        promotion().assertIsNotDisplayed()
    }

    @Test
    fun `unselected row hides expansion and retains it when selected again`() = runScenario {
        row().performClick()
        assertClickCalls()
        promotion().assertIsDisplayed()

        composeRule.runOnIdle { isSelected.value = false }
        promotion().assertIsNotDisplayed()
        composeRule.runOnIdle { isSelected.value = true }
        promotion().assertIsDisplayed()
        calls.expectNoEvents()
    }

    @Test
    fun `changing synthetic code resets expansion without evaluating callback`() = runScenario {
        row().performClick()
        assertClickCalls()
        promotion().assertIsDisplayed()

        composeRule.runOnIdle {
            paymentMethod.value = paymentMethod.value.copy(syntheticCode = "another_affirm")
        }

        promotion().assertIsNotDisplayed()
        calls.expectNoEvents()
    }

    @Test
    fun `successful expansion falls back to subtitle when promotion is unavailable`() = runScenario(
        promotion = null,
    ) {
        subtitle().assertIsNotDisplayed()

        row().performClick()

        assertClickCalls()
        subtitle().assertIsDisplayed()
    }

    @Test
    fun `false expansion decision keeps promotion and subtitle collapsed`() = runScenario(
        expansionResult = false,
    ) {
        row().performClick()

        assertClickCalls()
        promotion().assertIsNotDisplayed()
        subtitle().assertIsNotDisplayed()
    }

    @Test
    fun `false expansion decision keeps fallback subtitle collapsed`() = runScenario(
        expansionResult = false,
        promotion = null,
    ) {
        row().performClick()

        assertClickCalls()
        subtitle().assertIsNotDisplayed()
    }

    private fun runScenario(
        isEnabled: Boolean = true,
        expansionResult: Boolean = true,
        promotion: PaymentMethodMessagePromotion? = FakePaymentMethodMessagePromotionsHelper.affirmPromotion,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val calls = Turbine<String>()
        val expansionResultState = mutableStateOf(expansionResult)
        val isEnabledState = mutableStateOf(isEnabled)
        val isSelectedState = mutableStateOf(true)
        val metadata = PaymentMethodMetadataFactory.create()
        val paymentMethod = mutableStateOf(
            AffirmDefinition.uiDefinitionFactory(metadata).createSupportedPaymentMethod(metadata)
                .asDisplayablePaymentMethod(
                    customerSavedPaymentMethods = emptyList(),
                    incentive = null,
                    promotionProvider = { promotion },
                    shouldExpandOnClick = {
                        calls.add("expansion")
                        expansionResultState.value
                    },
                    onClick = { calls.add("selection") },
                )
        )
        val imageLoader = FakeStripeImageLoader()
        composeRule.setContent {
            NewPaymentMethodRowButton(
                isEnabled = isEnabledState.value,
                isSelected = isSelectedState.value,
                displayablePaymentMethod = paymentMethod.value,
                imageLoader = imageLoader,
            )
        }
        Scenario(paymentMethod, isSelectedState, expansionResultState, calls).block()
        calls.ensureAllEventsConsumed()
        imageLoader.ensureAllEventsConsumed()
    }

    private fun row() = composeRule.onNodeWithTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_affirm")

    private fun promotion() = composeRule.onNodeWithText("This is a message", substring = true)

    private fun subtitle() = composeRule.onNodeWithText("Pay over time with Affirm")

    private data class Scenario(
        val paymentMethod: MutableState<DisplayablePaymentMethod>,
        val isSelected: MutableState<Boolean>,
        val expansionResult: MutableState<Boolean>,
        val calls: Turbine<String>,
    ) {
        suspend fun assertClickCalls() {
            assertThat(calls.awaitItem()).isEqualTo("expansion")
            assertThat(calls.awaitItem()).isEqualTo("selection")
            calls.expectNoEvents()
        }
    }
}
