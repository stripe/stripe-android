package com.stripe.android.uicore.elements

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.uicore.analytics.LocalUiEventReporter
import com.stripe.android.uicore.analytics.UiEventReporter
import com.stripe.android.uicore.analytics.rememberInteractionReporter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.atMostOnce
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class UiEventReporterComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `on interaction source interaction received, should report event`() {
        val eventReporter: UiEventReporter = mock()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                val reportingElements = rememberInteractionReporter()

                Button(
                    modifier = Modifier.testTag(TEST_TAG),
                    interactionSource = reportingElements.interactionSource,
                    onClick = {},
                ) {
                    Text(text = "Click")
                }
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG).performClick()

        verify(eventReporter).onFieldInteracted()
    }

    @Test
    fun `on manual interaction received, should report event`() {
        val eventReporter: UiEventReporter = mock()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                val reportingElements = rememberInteractionReporter()

                Button(
                    modifier = Modifier.testTag(TEST_TAG),
                    onClick = {
                        reportingElements.reportInteractionManually()
                    },
                ) {
                    Text(text = "Click")
                }
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG).performClick()

        verify(eventReporter).onFieldInteracted()
    }

    @Test
    fun `on multiple interactions received, should report event only once`() {
        val eventReporter: UiEventReporter = mock()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                val reportingElements = rememberInteractionReporter()

                Button(
                    modifier = Modifier.testTag(TEST_TAG),
                    interactionSource = reportingElements.interactionSource,
                    onClick = {},
                ) {
                    Text(text = "Click")
                }
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG).performClick()

        verify(eventReporter, atMostOnce()).onFieldInteracted()
    }

    @Test
    fun `on multiple interactions with config changes, should report event only once`() {
        val eventReporter: UiEventReporter = mock()

        val restorationTester = StateRestorationTester(composeTestRule)

        restorationTester.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                val reportingElements = rememberInteractionReporter()

                Button(
                    modifier = Modifier.testTag(TEST_TAG),
                    interactionSource = reportingElements.interactionSource,
                    onClick = {},
                ) {
                    Text(text = "Click")
                }
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG).performClick()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithTag(TEST_TAG).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG).performClick()

        verify(eventReporter, atMostOnce()).onFieldInteracted()
    }

    private companion object {
        const val TEST_TAG = "Button"
    }
}
