package com.stripe.android.link.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.theme.DefaultLinkTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinkLogoutSheetTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun on_logout_button_click_callback_is_called() {
        val clickRecorder = MockClickRecorder()
        setContent(clickRecorder)

        composeTestRule.onNodeWithText("Log out of Link").performClick()

        assertThat(clickRecorder).isEqualTo(MockClickRecorder(logoutClicked = true))
    }

    @Test
    fun on_cancel_button_click_callback_is_called() {
        val clickRecorder = MockClickRecorder()
        setContent(clickRecorder)

        composeTestRule.onNodeWithText("Cancel").performClick()

        assertThat(clickRecorder).isEqualTo(MockClickRecorder(cancelClicked = true))
    }

    private fun setContent(
        clickRecorder: MockClickRecorder
    ) = composeTestRule.setContent {
        DefaultLinkTheme {
            LinkLogoutSheet(
                onLogoutClick = clickRecorder::onLogoutClicked,
                onCancelClick = clickRecorder::onCancelClick
            )
        }
    }

    private data class MockClickRecorder(
        var logoutClicked: Boolean = false,
        var cancelClicked: Boolean = false
    ) {
        fun onLogoutClicked() { logoutClicked = true }
        fun onCancelClick() { cancelClicked = true }
    }
}
