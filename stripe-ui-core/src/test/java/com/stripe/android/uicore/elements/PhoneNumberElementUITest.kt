package com.stripe.android.uicore.elements

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.uicore.analytics.LocalUiEventReporter
import com.stripe.android.uicore.analytics.UiEventReporter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class PhoneNumberElementUITest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `on click, should report interaction`() {
        val eventReporter: UiEventReporter = mock()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                PhoneNumberElementUI(
                    controller = PhoneNumberController.createPhoneNumberController(),
                    enabled = true,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.testTag(TEST_TAG)
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
                PhoneNumberElementUI(
                    controller = PhoneNumberController.createPhoneNumberController(),
                    enabled = false,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG).performClick()

        verifyNoInteractions(eventReporter)
    }

    @Test
    fun `on text input, should report interaction`() {
        val eventReporter: UiEventReporter = mock()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                PhoneNumberElementUI(
                    controller = PhoneNumberController.createPhoneNumberController(),
                    enabled = true,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG).performTextInput("2267007611")

        verify(eventReporter).onFieldInteracted()
    }

    private companion object {
        const val TEST_TAG = "ReportablePhoneNumberElement"
    }
}
