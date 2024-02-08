package com.stripe.android.ui.core.elements

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.uicore.analytics.LocalUiEventReporter
import com.stripe.android.uicore.analytics.UiEventReporter
import com.stripe.android.utils.createPaymentsUiCoreComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class ScanCardButtonUITest {
    @get:Rule
    val composeTestRule = createPaymentsUiCoreComposeRule()

    @Test
    fun `on click, should report interaction`() {
        val eventReporter: UiEventReporter = mock()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                ScanCardButtonUI(
                    enabled = true,
                    modifier = Modifier.testTag(TEST_TAG),
                    onResult = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG).performClick()

        verify(eventReporter).onFieldInteracted()
    }

    @Test
    fun `on click and disabled, should not report interaction`() {
        val eventReporter: UiEventReporter = mock()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                ScanCardButtonUI(
                    enabled = false,
                    modifier = Modifier.testTag(TEST_TAG),
                    onResult = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG).performClick()

        verifyNoInteractions(eventReporter)
    }

    private companion object {
        const val TEST_TAG = "ReportableButton"
    }
}
