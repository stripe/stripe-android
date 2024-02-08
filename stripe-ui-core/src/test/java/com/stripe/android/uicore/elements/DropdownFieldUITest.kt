package com.stripe.android.uicore.elements

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.uicore.analytics.LocalUiEventReporter
import com.stripe.android.uicore.analytics.UiEventReporter
import com.stripe.android.uicore.utils.createUiCoreComposeTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class DropdownFieldUITest {
    @get:Rule
    val composeTestRule = createUiCoreComposeTestRule()

    @Test
    fun `on click, should report interaction`() {
        val eventReporter: UiEventReporter = mock()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                DropDown(
                    modifier = Modifier.testTag(TEST_TAG),
                    controller = DropdownFieldController(
                        config = CountryConfig(),
                        initialValue = "US"
                    ),
                    enabled = true
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG).performClick()

        verify(eventReporter).onFieldInteracted()
    }

    @Test
    fun `on click and disable, should report not interaction`() {
        val eventReporter: UiEventReporter = mock()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                DropDown(
                    modifier = Modifier.testTag(TEST_TAG),
                    controller = DropdownFieldController(
                        config = CountryConfig(),
                        initialValue = "US"
                    ),
                    enabled = false
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG).performClick()

        verifyNoInteractions(eventReporter)
    }

    private companion object {
        const val TEST_TAG = "ReportableDropdown"
    }
}
