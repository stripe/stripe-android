package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
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

    @Test
    fun `Handles navigation icon press correctly`() {
        val mockTextInputService = mock<TextInputService>()
        var didCallOnNavigationIconPressed = false

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalTextInputService provides mockTextInputService,
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

        @Suppress("DEPRECATION")
        verify(mockTextInputService).hideSoftwareKeyboard()
        assertThat(didCallOnNavigationIconPressed).isTrue()
    }

    @Test
    fun `Ignores navigation icon press if not enabled`() {
        val mockTextInputService = mock<TextInputService>()
        var didCallOnNavigationIconPressed = false

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalTextInputService provides mockTextInputService,
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
        @Suppress("DEPRECATION")
        verify(mockTextInputService, never()).hideSoftwareKeyboard()
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
