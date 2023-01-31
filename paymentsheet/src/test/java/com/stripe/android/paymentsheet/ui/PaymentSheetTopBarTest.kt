package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.R
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class PaymentSheetTopBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun `Handles navigation icon press correctly`() {
        val mockKeyboardController = mock<SoftwareKeyboardController>()
        var didCallOnNavigationIconPressed = false

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalSoftwareKeyboardController provides mockKeyboardController,
            ) {
                PaymentSheetTopBar(
                    state = mockState(isEnabled = true),
                    elevation = 0.dp,
                    onNavigationIconPressed = { didCallOnNavigationIconPressed = true },
                    onEditIconPressed = { throw AssertionError("Not expected") },
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        verify(mockKeyboardController).hide()
        assertThat(didCallOnNavigationIconPressed).isTrue()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun `Ignores navigation icon press if not enabled`() {
        val mockKeyboardController = mock<SoftwareKeyboardController>()
        var didCallOnNavigationIconPressed = false

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalSoftwareKeyboardController provides mockKeyboardController,
            ) {
                PaymentSheetTopBar(
                    state = mockState(isEnabled = false),
                    elevation = 0.dp,
                    onNavigationIconPressed = { didCallOnNavigationIconPressed = true },
                    onEditIconPressed = { throw AssertionError("Not expected") },
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        verify(mockKeyboardController, never()).hide()
        assertThat(didCallOnNavigationIconPressed).isFalse()
    }

    @Ignore("Figure out why this times out when run with other tests")
    @Test
    fun `Handles edit icon press correctly`() {
        var didCallOnEditIconPressed = false

        composeTestRule.setContent {
            PaymentSheetTopBar(
                state = mockState(showEditMenu = true),
                elevation = 0.dp,
                onNavigationIconPressed = { throw AssertionError("Not expected") },
                onEditIconPressed = { didCallOnEditIconPressed = true },
            )
        }

        composeTestRule
            .onNodeWithText("EDIT")
            .performClick()

        assertThat(didCallOnEditIconPressed).isTrue()
    }

    private fun mockState(
        isEnabled: Boolean = true,
        showEditMenu: Boolean = false,
    ): PaymentSheetTopBarState {
        return PaymentSheetTopBarState(
            icon = R.drawable.stripe_ic_paymentsheet_back,
            contentDescription = R.string.back,
            showTestModeLabel = false,
            showEditMenu = showEditMenu,
            editMenuLabel = R.string.edit,
            isEnabled = isEnabled,
        )
    }
}
