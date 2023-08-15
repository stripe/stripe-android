package com.stripe.android.paymentsheet.ui

import android.os.Build
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import com.stripe.android.R as StripeR
import com.stripe.android.ui.core.R as StripeUiCoreR

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
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
            contentDescription = StripeUiCoreR.string.stripe_back,
            showTestModeLabel = false,
            showEditMenu = showEditMenu,
            editMenuLabel = StripeR.string.stripe_edit,
            isEnabled = isEnabled,
        )
    }
}
