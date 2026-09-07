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
import com.google.common.truth.Truth.assertThat
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
    fun `dialog displays copy and discard invokes callback`() {
        var discardChangesCount = 0
        var keepEditingCount = 0

        composeRule.setContent {
            DefaultStripeTheme {
                AddressElementDismissalDialog(
                    onDiscardChanges = { discardChangesCount++ },
                    onKeepEditing = { keepEditingCount++ },
                )
            }
        }

        composeRule.onNodeWithTag(TEST_TAG_SIMPLE_DIALOG).onChildren().assertAny(
            hasText("Discard changes?")
        )
        composeRule.onNodeWithTag(TEST_TAG_SIMPLE_DIALOG).onChildren().assertAny(
            hasText("Your address changes have not been saved.")
        )
        composeRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).assert(hasText("Discard changes"))
        composeRule.onNodeWithTag(TEST_TAG_DIALOG_DISMISS_BUTTON).assert(hasText("Keep editing"))

        composeRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).performClick()

        assertThat(discardChangesCount).isEqualTo(1)
        assertThat(keepEditingCount).isEqualTo(0)
    }

    @Test
    fun `keep editing callback dismisses dialog`() {
        var dialogVisible by mutableStateOf(true)
        var keepEditingCount = 0

        composeRule.setContent {
            DefaultStripeTheme {
                if (dialogVisible) {
                    AddressElementDismissalDialog(
                        onDiscardChanges = {},
                        onKeepEditing = {
                            keepEditingCount++
                            dialogVisible = false
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithTag(TEST_TAG_DIALOG_DISMISS_BUTTON).performClick()
        composeRule.waitForIdle()

        assertThat(keepEditingCount).isEqualTo(1)
        composeRule.onNodeWithTag(TEST_TAG_SIMPLE_DIALOG).assertDoesNotExist()
    }
}
