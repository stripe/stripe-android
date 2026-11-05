package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.google.testing.junit.testparameterinjector.TestParameter
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
internal class PrimaryButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `idle button displays the requested lock icon`() = runScenario(
        processingState = PrimaryButtonProcessingState.Idle(null),
    ) {
        composeRule.onNodeWithTag(PRIMARY_BUTTON_LOCK_ICON_TEST_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `idle button displays the requested secure icon`(@TestParameter enabled: Boolean) = runScenario(
        processingState = PrimaryButtonProcessingState.Idle(null),
        icon = PrimaryButtonIcon.Secure,
        enabled = enabled,
    ) {
        composeRule.onNodeWithTag(PRIMARY_BUTTON_SECURE_ICON_TEST_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(PRIMARY_BUTTON_LOCK_ICON_TEST_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `processing button ignores the requested secure icon`() = assertSecureIconIgnored(
        processingState = PrimaryButtonProcessingState.Processing,
    )

    @Test
    fun `completed button ignores the requested secure icon`() = assertSecureIconIgnored(
        processingState = PrimaryButtonProcessingState.Completed,
    )

    private fun assertSecureIconIgnored(processingState: PrimaryButtonProcessingState) = runScenario(
        processingState = processingState,
        icon = PrimaryButtonIcon.Secure,
    ) {
        composeRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PRIMARY_BUTTON_SECURE_ICON_TEST_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithTag(PRIMARY_BUTTON_LOCK_ICON_TEST_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `processing button ignores the requested lock icon`() = assertLockIconIgnored(
        processingState = PrimaryButtonProcessingState.Processing,
    )

    @Test
    fun `completed button ignores the requested lock icon`() = assertLockIconIgnored(
        processingState = PrimaryButtonProcessingState.Completed,
    )

    private fun assertLockIconIgnored(processingState: PrimaryButtonProcessingState) = runScenario(processingState) {
        composeRule.onNodeWithTag(PRIMARY_BUTTON_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PRIMARY_BUTTON_LOCK_ICON_TEST_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    private fun runScenario(
        processingState: PrimaryButtonProcessingState,
        icon: PrimaryButtonIcon = PrimaryButtonIcon.Lock,
        enabled: Boolean = true,
        block: () -> Unit,
    ) {
        composeRule.setContent {
            // Render the final state without waiting for the completion animation.
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PrimaryButton(
                    label = "Pay",
                    icon = icon,
                    enabled = enabled,
                    processingState = processingState,
                ) {}
            }
        }
        block()
    }
}
