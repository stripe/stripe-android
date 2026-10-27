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
    fun `selected collapsed row stays collapsed until state changes`() = runScenario {
        promotion().assertIsNotDisplayed()

        row().performClick()
        assertThat(calls.awaitItem()).isEqualTo("selection")
        promotion().assertIsNotDisplayed()

        composeRule.runOnIdle { paymentMethod.value = paymentMethod.value.copy(isExpanded = true) }
        promotion().assertIsDisplayed()
    }

    @Test
    fun `disabled click does not select row`() = runScenario(isEnabled = false) {
        row().assertIsNotEnabled().performClick()

        calls.expectNoEvents()
        promotion().assertIsNotDisplayed()
    }

    @Test
    fun `expanded row stays expanded after leaving composition and returning`() = runScenario {
        composeRule.runOnIdle { paymentMethod.value = paymentMethod.value.copy(isExpanded = true) }
        promotion().assertIsDisplayed()

        composeRule.runOnIdle { isVisible.value = false }
        row().assertDoesNotExist()
        composeRule.runOnIdle { isVisible.value = true }

        promotion().assertIsDisplayed()
        calls.expectNoEvents()
    }

    @Test
    fun `unselected row hides expansion and retains it when selected again`() = runScenario(isExpanded = true) {
        promotion().assertIsDisplayed()
        composeRule.runOnIdle { isSelected.value = false }
        promotion().assertIsNotDisplayed()
        composeRule.runOnIdle { isSelected.value = true }
        promotion().assertIsDisplayed()
        calls.expectNoEvents()
    }

    @Test
    fun `expanded row falls back to subtitle when promotion is unavailable`() = runScenario(
        isExpanded = true,
        promotion = null,
    ) {
        subtitle().assertIsDisplayed()
    }

    @Test
    fun `collapsed row hides fallback subtitle`() = runScenario(promotion = null) {
        subtitle().assertIsNotDisplayed()
    }

    private fun runScenario(
        isEnabled: Boolean = true,
        isExpanded: Boolean = false,
        promotion: PaymentMethodMessagePromotion? = FakePaymentMethodMessagePromotionsHelper.affirmPromotion,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val calls = Turbine<String>()
        val isVisible = mutableStateOf(true)
        val isSelectedState = mutableStateOf(true)
        val metadata = PaymentMethodMetadataFactory.create()
        val paymentMethod = mutableStateOf(
            AffirmDefinition.uiDefinitionFactory(metadata).createSupportedPaymentMethod(metadata)
                .asDisplayablePaymentMethod(
                    customerSavedPaymentMethods = emptyList(),
                    incentive = null,
                    promotionProvider = { promotion },
                    isExpanded = isExpanded,
                    onClick = { calls.add("selection") },
                )
        )
        val imageLoader = FakeStripeImageLoader()
        composeRule.setContent {
            if (isVisible.value) {
                NewPaymentMethodRowButton(
                    isEnabled = isEnabled,
                    isSelected = isSelectedState.value,
                    displayablePaymentMethod = paymentMethod.value,
                    imageLoader = imageLoader,
                )
            }
        }
        Scenario(paymentMethod, isSelectedState, isVisible, calls).block()
        calls.ensureAllEventsConsumed()
        imageLoader.ensureAllEventsConsumed()
    }

    private fun row() = composeRule.onNodeWithTag("${TEST_TAG_NEW_PAYMENT_METHOD_ROW_BUTTON}_affirm")

    private fun promotion() = composeRule.onNodeWithText("This is a message", substring = true)

    private fun subtitle() = composeRule.onNodeWithText("Pay over time with Affirm")

    private data class Scenario(
        val paymentMethod: MutableState<DisplayablePaymentMethod>,
        val isSelected: MutableState<Boolean>,
        val isVisible: MutableState<Boolean>,
        val calls: Turbine<String>,
    )
}
