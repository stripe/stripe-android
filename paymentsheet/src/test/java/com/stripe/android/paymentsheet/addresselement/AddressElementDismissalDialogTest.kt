package com.stripe.android.paymentsheet.addresselement

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import app.cash.turbine.Turbine
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.ui.core.elements.TEST_TAG_DIALOG_CONFIRM_BUTTON
import com.stripe.android.ui.core.elements.TEST_TAG_DIALOG_DISMISS_BUTTON
import com.stripe.android.ui.core.elements.TEST_TAG_SIMPLE_DIALOG
import com.stripe.android.uicore.DefaultStripeTheme
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class AddressElementDismissalDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `dialog displays copy and discard invokes callback`() = runScenario {
        composeRule.onNodeWithTag(TEST_TAG_SIMPLE_DIALOG).onChildren().assertAny(
            hasText("Discard changes?")
        )
        composeRule.onNodeWithTag(TEST_TAG_SIMPLE_DIALOG).onChildren().assertAny(
            hasText("Your address changes won't be saved.")
        )
        composeRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).assert(hasText("Discard"))
        composeRule.onNodeWithTag(TEST_TAG_DIALOG_DISMISS_BUTTON).assert(hasText("Cancel"))

        composeRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).performClick()

        discardChanges.takeItem()
        keepEditing.expectNoEvents()
    }

    @Test
    fun `cancel dismisses dialog and keeps editing`() = runScenario {
        composeRule.onNodeWithTag(TEST_TAG_DIALOG_DISMISS_BUTTON).performClick()
        composeRule.waitForIdle()

        keepEditing.takeItem()
        discardChanges.expectNoEvents()
        composeRule.onNodeWithTag(TEST_TAG_SIMPLE_DIALOG).assertDoesNotExist()
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        var dialogVisible by mutableStateOf(true)
        val discardChanges = Turbine<Unit>()
        val keepEditing = Turbine<Unit>()

        composeRule.setContent {
            DefaultStripeTheme {
                if (dialogVisible) {
                    AddressElementDismissalDialog(
                        onDiscardChanges = {
                            discardChanges.add(Unit)
                            dialogVisible = false
                        },
                        onKeepEditing = {
                            keepEditing.add(Unit)
                            dialogVisible = false
                        },
                    )
                }
            }
        }

        Scenario(discardChanges, keepEditing).block()
        discardChanges.ensureAllEventsConsumed()
        keepEditing.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val discardChanges: Turbine<Unit>,
        val keepEditing: Turbine<Unit>,
    )
}
