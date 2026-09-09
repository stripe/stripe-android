@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CheckoutPlaygroundScenariosUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `group rows navigate through nested catalog`() = runScenario {
        scenario("lpms").assertIsDisplayed().performClick()

        scenario("north_america").assertIsDisplayed().performClick()

        scenario("us_common").assertIsDisplayed()
    }

    @Test
    fun `leaf selection returns selected preset`() = runScenario {
        scenario("tax").performClick()
        scenario("no_tax").performClick()

        assertThat(selected?.key).isEqualTo("no_tax")
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        var currentGroup by mutableStateOf(CheckoutPlaygroundScenarios.root)
        var selected: CheckoutPlaygroundScenario.Leaf? = null
        composeRule.setContent {
            CheckoutPlaygroundScenariosUi(
                group = currentGroup,
                onOpenGroup = { currentGroup = it },
                onSelect = { selected = it },
            )
        }
        block(
            Scenario(
                scenario = { key ->
                    val node = currentGroup.children.single { it.key == key }
                    composeRule.onNodeWithTag(checkoutScenarioTestTag(node))
                },
                selectedLeaf = { selected },
            )
        )
    }

    private data class Scenario(
        val scenario: (String) -> androidx.compose.ui.test.SemanticsNodeInteraction,
        private val selectedLeaf: () -> CheckoutPlaygroundScenario.Leaf?,
    ) {
        val selected: CheckoutPlaygroundScenario.Leaf?
            get() = selectedLeaf()
    }
}
