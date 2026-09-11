package com.stripe.android.paymentsheet.addresselement

import androidx.compose.material.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.StripeTheme
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class InputAddressScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `processing shows loading and blocks save and close actions`() {
        val actions = ViewActionRecorder<ViewAction>()
        composeRule.setContent {
            StripeTheme {
                InputAddressScreen(
                    primaryButtonEnabled = false,
                    primaryButtonLoading = true,
                    primaryButtonText = "Save",
                    title = "Shipping address",
                    onPrimaryButtonClick = { actions.record(ViewAction.Save) },
                    onDisabledButtonClick = { actions.record(ViewAction.DisabledSave) },
                    closeButtonEnabled = false,
                    onCloseClick = { actions.record(ViewAction.Close) },
                    topContent = {},
                    formContent = { Text("Address form") },
                    bottomContent = {},
                )
            }
        }

        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertExists()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithContentDescription("Close").assertIsNotEnabled().performClick()

        assertThat(actions.viewActions).isEmpty()
    }

    private enum class ViewAction {
        Save,
        DisabledSave,
        Close,
    }
}
