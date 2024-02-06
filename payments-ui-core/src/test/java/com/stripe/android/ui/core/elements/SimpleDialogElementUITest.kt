package com.stripe.android.ui.core.elements

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.uicore.analytics.LocalUiEventReporter
import com.stripe.android.uicore.analytics.UiEventReporter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SimpleDialogElementUITest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `on confirm click, should should report interaction`() {
        val eventReporter: UiEventReporter = mock()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                SimpleDialogElementUI(
                    titleText = "Should remove?",
                    confirmText = "Yes",
                    dismissText = "No",
                    messageText = "Should remove this item?",
                    onConfirmListener = {},
                    onDismissListener = {},
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }
        }

        composeTestRule
            .onNodeWithTag(TEST_TAG)
            .onChildren()
            .filterToOne(hasText("Yes"))
            .performClick()

        verify(eventReporter).onFieldInteracted()
    }

    @Test
    fun `on dismiss click, should should report interaction`() {
        val eventReporter: UiEventReporter = mock()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                SimpleDialogElementUI(
                    titleText = "Should remove?",
                    confirmText = "Yes",
                    dismissText = "No",
                    messageText = "Should remove this item?",
                    onConfirmListener = {},
                    onDismissListener = {},
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }
        }

        composeTestRule
            .onNodeWithTag(TEST_TAG)
            .onChildren()
            .filterToOne(hasText("No"))
            .performClick()

        verify(eventReporter).onFieldInteracted()
    }

    private companion object {
        const val TEST_TAG = "ReportableDialog"
    }
}
