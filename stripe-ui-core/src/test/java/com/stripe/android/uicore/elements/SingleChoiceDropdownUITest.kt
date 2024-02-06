package com.stripe.android.uicore.elements

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.analytics.LocalUiEventReporter
import com.stripe.android.uicore.analytics.UiEventReporter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class SingleChoiceDropdownUITest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `on item click, should report interaction`() {
        val eventReporter: UiEventReporter = mock()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                SingleChoiceDropdown(
                    expanded = true,
                    title = resolvableString("Select a choice"),
                    currentChoice = TestChoice("Apple"),
                    choices = listOf(
                        TestChoice("Apple"),
                        TestChoice("Orange"),
                        TestChoice("Banana"),
                    ),
                    onChoiceSelected = {},
                    headerTextColor = Color.Blue,
                    optionTextColor = Color.Blue,
                    onDismiss = {},
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG).onChildAt(1).performClick()

        verify(eventReporter).onFieldInteracted()
    }

    @Test
    fun `on title click, should not report interaction`() {
        val eventReporter: UiEventReporter = mock()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalUiEventReporter provides eventReporter
            ) {
                SingleChoiceDropdown(
                    expanded = true,
                    title = resolvableString("Select a choice"),
                    currentChoice = TestChoice("Apple"),
                    choices = listOf(
                        TestChoice("Apple"),
                        TestChoice("Orange"),
                        TestChoice("Banana"),
                    ),
                    onChoiceSelected = {},
                    headerTextColor = Color.Blue,
                    optionTextColor = Color.Blue,
                    onDismiss = {},
                    modifier = Modifier.testTag(TEST_TAG),
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG).onChildAt(0).performClick()

        verifyNoInteractions(eventReporter)
    }

    private companion object {
        const val TEST_TAG = "ReportableDropdown"

        data class TestChoice(
            val name: String,
        ) : SingleChoiceDropdownItem {
            override val label: ResolvableString = resolvableString(name)
            override val icon: Int? = null
        }
    }
}
