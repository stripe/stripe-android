package com.stripe.android.uicore.elements

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.platform.LocalAutofillTree
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
class TextFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `on click, should report interaction`() {
        val eventReporter: UiEventReporter = mock()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                TextField(
                    textFieldController = NameConfig.createController("John"),
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
                TextField(
                    textFieldController = NameConfig.createController("John"),
                    enabled = false,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG).performClick()

        verifyNoInteractions(eventReporter)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun `on autofill event, should report autofill event`() {
        val eventReporter: UiEventReporter = mock()
        val autofillTree = AutofillTree()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter,
                LocalAutofillTree provides autofillTree,
            ) {
                TextField(
                    textFieldController = EmailConfig.createController("email@ema"),
                    enabled = true,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        autofillTree.children[autofillTree.children.toList().first().first]?.onFill?.invoke("email@email.com")

        verify(eventReporter).onAutofillEvent(AutofillType.EmailAddress.name)
    }

    @Test
    fun `on value change, should report interaction`() {
        val eventReporter: UiEventReporter = mock()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                TextField(
                    textFieldController = NameConfig.createController("John"),
                    enabled = true,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG).performTextInput("John Doe")

        verify(eventReporter).onFieldInteracted()
    }

    private companion object {
        const val TEST_TAG = "ReportableTextField"
    }
}
