package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.createComposeCleanupRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class PaymentSheetTopBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun `Handles navigation icon press correctly`() {
        val mockKeyboardController = mock<SoftwareKeyboardController>()
        var didCallOnNavigationIconPressed = false

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalSoftwareKeyboardController provides mockKeyboardController,
            ) {
                PaymentSheetTopBar(
                    state = mockState(),
                    title = null,
                    canNavigateBack = true,
                    isEnabled = true,
                        onNavigationIconPressed = { didCallOnNavigationIconPressed = true },
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        verify(mockKeyboardController).hide()
        assertThat(didCallOnNavigationIconPressed).isTrue()
    }

    @Test
    fun `Ignores navigation icon press if not enabled`() {
        val mockKeyboardController = mock<SoftwareKeyboardController>()
        var didCallOnNavigationIconPressed = false

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalSoftwareKeyboardController provides mockKeyboardController,
            ) {
                PaymentSheetTopBar(
                    state = mockState(),
                    title = null,
                    canNavigateBack = true,
                    isEnabled = false,
                        onNavigationIconPressed = { didCallOnNavigationIconPressed = true },
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()
        verify(mockKeyboardController, never()).hide()
        assertThat(didCallOnNavigationIconPressed).isFalse()
    }

    @Test
    fun `Handles edit icon press correctly`() {
        var didCallOnEditIconPressed = false

        composeTestRule.setContent {
            PaymentSheetTopBar(
                state = mockState(showEditMenu = true, onEditIconPressed = { didCallOnEditIconPressed = true }),
                title = null,
                canNavigateBack = true,
                isEnabled = true,
                onNavigationIconPressed = { throw AssertionError("Not expected") },
            )
        }

        composeTestRule
            .onNodeWithText("EDIT")
            .performClick()

        assertThat(didCallOnEditIconPressed).isTrue()
    }

    @Test
    fun `Displays title when provided`() {
        composeTestRule.setContent {
            PaymentSheetTopBar(
                state = mockState(),
                title = "Select your payment method",
                canNavigateBack = false,
                isEnabled = true,
                onNavigationIconPressed = { throw AssertionError("Not expected") },
            )
        }

        composeTestRule
            .onNodeWithTag(SHEET_TITLE_TEST_TAG)
            .assertTextEquals("Select your payment method")
    }

    @Test
    fun `Does not display title when null`() {
        composeTestRule.setContent {
            PaymentSheetTopBar(
                state = mockState(),
                title = null,
                canNavigateBack = false,
                isEnabled = true,
                onNavigationIconPressed = { throw AssertionError("Not expected") },
            )
        }

        composeTestRule
            .onNodeWithTag(SHEET_TITLE_TEST_TAG)
            .assertDoesNotExist()
    }

    private fun mockState(
        showEditMenu: Boolean = false,
        onEditIconPressed: () -> Unit = { throw AssertionError("Not expected") }
    ): PaymentSheetTopBarState {
        return PaymentSheetTopBarState(
            showEditMenu = showEditMenu,
            isEditing = false,
            onEditIconPressed = onEditIconPressed,
        )
    }
}
