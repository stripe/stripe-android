package com.stripe.android.uicore.elements

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RowElementUITest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun `initial field measurement does not recompose row`() = runScenario {
        assertInitialVisibilityChecks()
    }

    @Test
    fun `field height changes do not recompose row`() = runScenario {
        assertInitialVisibilityChecks()

        composeRule.runOnIdle { fieldHeight.value = 80.dp }
        composeRule.waitForIdle()

        hiddenIdentifiers.containsCalls.expectNoEvents()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val scenario = Scenario()
        val controller = RowController(
            listOf("City", "State").map { label ->
                SimpleTextElement(
                    identifier = FormFieldId.Generic(label),
                    controller = SimpleTextFieldController(SimpleTextFieldConfig(label.resolvableString)),
                )
            }
        )
        composeRule.setContent {
            RowElementUI(
                enabled = true,
                controller = controller,
                hiddenIdentifiers = scenario.hiddenIdentifiers,
                lastTextFieldIdentifier = null,
                modifier = Modifier.layout { measurable, constraints ->
                    val height = scenario.fieldHeight.value.roundToPx()
                    val placeable = measurable.measure(
                        constraints.copy(minHeight = height, maxHeight = height)
                    )
                    layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
                },
            )
        }
        composeRule.waitForIdle()
        scenario.block()
        scenario.hiddenIdentifiers.containsCalls.ensureAllEventsConsumed()
    }

    private class Scenario {
        val fieldHeight = mutableStateOf(60.dp)
        val hiddenIdentifiers = FakeHiddenIdentifiers()

        suspend fun assertInitialVisibilityChecks() {
            // The row filters its fields, then each field checks its own visibility.
            // Measuring a field should not recompose the row and repeat those checks.
            listOf("City", "State", "City", "State").forEach { label ->
                assertThat(hiddenIdentifiers.containsCalls.awaitItem()).isEqualTo(FormFieldId.Generic(label))
            }
        }
    }

    private class FakeHiddenIdentifiers : Set<FormFieldId> by emptySet() {
        val containsCalls = Turbine<FormFieldId>()

        override fun contains(element: FormFieldId): Boolean {
            containsCalls.add(element)
            return false
        }
    }
}
