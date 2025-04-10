package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
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
                    canNavigateBack = true,
                    isEnabled = true,
                    elevation = 0.dp,
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
                    canNavigateBack = true,
                    isEnabled = false,
                    elevation = 0.dp,
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
                canNavigateBack = true,
                isEnabled = true,
                elevation = 0.dp,
                onNavigationIconPressed = { throw AssertionError("Not expected") },
            )
        }

        composeTestRule
            .onNodeWithText("EDIT")
            .performClick()

        assertThat(didCallOnEditIconPressed).isTrue()
    }

    private fun mockState(
        showEditMenu: Boolean = false,
        onEditIconPressed: () -> Unit = { throw AssertionError("Not expected") }
    ): PaymentSheetTopBarState {
        return PaymentSheetTopBarState(
            showTestModeLabel = false,
            showEditMenu = showEditMenu,
            isEditing = false,
            onEditIconPressed = onEditIconPressed,
        )
    }
}
